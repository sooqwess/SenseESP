package net.espmod.render;

import net.espmod.config.EspConfig;
import net.espmod.config.HudCorner;
import net.espmod.util.ColorUtil;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;

public final class EspHudRenderer {
    private EspHudRenderer() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }

        EspConfig cfg = EspConfig.get();
        boolean bypassWalls = EspPolicy.canBypassWalls(client);

        if (cfg.showStatusHud) {
            renderStatusPill(context, client, cfg, bypassWalls);
        }

        if (!cfg.enabled || (!cfg.render2DOverlay && !cfg.renderNametags)) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Quaternionf invCamRot = new Quaternionf(camera.getRotation()).conjugate();
        int fovSetting = client.options.getFov().getValue();
        Matrix4f proj = client.gameRenderer.getBasicProjectionMatrix(fovSetting);
        float m11 = proj.m11();
        if (Math.abs(m11) < 1.0E-4f) {
            return;
        }

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        double halfH = sh * 0.5;
        double halfW = sw * 0.5;
        double focalLength = halfH * m11;

        float tickDelta = tickCounter.getTickDelta(false);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        TextRenderer font = client.textRenderer;

        for (Entity entity : client.world.getEntities()) {
            if (!EntityClassifier.shouldTrackEntity(entity, client.player, cfg)) {
                continue;
            }
            double distSq = client.player.squaredDistanceTo(entity);
            if (distSq > maxDistSq) {
                continue;
            }
            if (!EspPolicy.canRenderEntity(client, entity)) {
                continue;
            }

            double dist = Math.sqrt(distSq);
            float fade = ColorUtil.computeDistanceAlpha(dist, cfg.maxDistance, cfg.distanceFade);
            int rgb = EntityClassifier.getEntityColor(entity, cfg);

            double ix = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double iy = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double iz = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Box box = entity.getBoundingBox()
                .offset(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .expand(0.04);

            double minSX = Double.POSITIVE_INFINITY, minSY = Double.POSITIVE_INFINITY;
            double maxSX = Double.NEGATIVE_INFINITY, maxSY = Double.NEGATIVE_INFINITY;
            int visibleCorners = 0;

            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};

            for (double x : xs) {
                for (double y : ys) {
                    for (double z : zs) {
                        Vector3f rel = new Vector3f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z));
                        invCamRot.transform(rel);
                        if (rel.z >= -0.08f) {
                            continue;
                        }
                        double invZ = 1.0 / (-rel.z);
                        double sx = halfW + rel.x * invZ * focalLength;
                        double sy = halfH - rel.y * invZ * focalLength;
                        minSX = Math.min(minSX, sx);
                        minSY = Math.min(minSY, sy);
                        maxSX = Math.max(maxSX, sx);
                        maxSY = Math.max(maxSY, sy);
                        visibleCorners++;
                    }
                }
            }

            if (visibleCorners == 0 || maxSX < 0 || minSX > sw || maxSY < 0 || minSY > sh) {
                continue;
            }

            int x1 = Math.max(2, (int) Math.floor(minSX));
            int y1 = Math.max(2, (int) Math.floor(minSY));
            int x2 = Math.min(sw - 2, (int) Math.ceil(maxSX));
            int y2 = Math.min(sh - 2, (int) Math.ceil(maxSY));
            int bw = x2 - x1;
            int bh = y2 - y1;

            if (cfg.render2DOverlay && bw >= 3 && bh >= 3 && bw < sw * 0.92 && bh < sh * 0.92) {
                int strokeColor = ColorUtil.withAlpha(rgb, 0.90f * fade);
                int shadowColor = ColorUtil.withAlpha(0x050811, 0.65f * fade);
                if (cfg.corner2DBox) {
                    drawCornerBrackets(context, x1, y1, x2, y2, strokeColor, shadowColor);
                } else {
                    drawRectOutline(context, x1 - 1, y1 - 1, bw + 2, bh + 2, shadowColor);
                    drawRectOutline(context, x1, y1, bw, bh, strokeColor);
                }

                if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f && bh >= 8) {
                    float ratio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
                    int barX = Math.max(1, x1 - 4);
                    context.fill(barX - 1, y1 - 1, barX + 2, y2 + 1, ColorUtil.withAlpha(0x060A12, 0.80f * fade));
                    int filledH = Math.max(1, Math.round(bh * ratio));
                    int hpColor = ColorUtil.withAlpha(ColorUtil.healthColor(ratio), 0.95f * fade);
                    context.fill(barX, y2 - filledH, barX + 1, y2, hpColor);
                }
            }

            if (cfg.renderNametags) {
                renderNametagCard(context, font, entity, dist, rgb, fade, (x1 + x2) / 2, Math.max(14, y1 - 4), cfg);
            }
        }
    }

    private static void drawRectOutline(DrawContext g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawCornerBrackets(DrawContext g, int x1, int y1, int x2, int y2, int color, int shadow) {
        int w = x2 - x1;
        int h = y2 - y1;
        int cx = Math.max(2, Math.min(10, w / 3));
        int cy = Math.max(2, Math.min(10, h / 3));

        g.fill(x1 - 1, y1 - 1, x1 + cx + 1, y1 + 2, shadow);
        g.fill(x1 - 1, y1 - 1, x1 + 2, y1 + cy + 1, shadow);
        g.fill(x2 - cx - 1, y1 - 1, x2 + 2, y1 + 2, shadow);
        g.fill(x2 - 1, y1 - 1, x2 + 2, y1 + cy + 1, shadow);
        g.fill(x1 - 1, y2 - 1, x1 + cx + 1, y2 + 2, shadow);
        g.fill(x1 - 1, y2 - cy - 1, x1 + 2, y2 + 2, shadow);
        g.fill(x2 - cx - 1, y2 - 1, x2 + 2, y2 + 2, shadow);
        g.fill(x2 - 1, y2 - cy - 1, x2 + 2, y2 + 2, shadow);

        g.fill(x1, y1, x1 + cx, y1 + 1, color);
        g.fill(x1, y1, x1 + 1, y1 + cy, color);
        g.fill(x2 - cx, y1, x2 + 1, y1 + 1, color);
        g.fill(x2, y1, x2 + 1, y1 + cy, color);
        g.fill(x1, y2, x1 + cx, y2 + 1, color);
        g.fill(x1, y2 - cy, x1 + 1, y2 + 1, color);
        g.fill(x2 - cx, y2, x2 + 1, y2 + 1, color);
        g.fill(x2, y2 - cy, x2 + 1, y2 + 1, color);
    }

    private static void renderNametagCard(
        DrawContext g, TextRenderer font, Entity entity, double dist,
        int rgb, float fade, int centerX, int bottomY, EspConfig cfg
    ) {
        StringBuilder sb = new StringBuilder(entity.getDisplayName().getString());
        if (entity instanceof ItemEntity itemEntity && cfg.showItemCount) {
            int count = itemEntity.getStack().getCount();
            if (count > 1) {
                sb.append(" x").append(count);
            }
        }

        String hpPart = null;
        int hpRgb = 0x4ADE80;
        if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f) {
            float hp = living.getHealth() + living.getAbsorptionAmount();
            float ratio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
            hpRgb = ColorUtil.healthColor(ratio);
            hpPart = String.format(Locale.ROOT, " %.0f♥", hp * 0.5f);
        }

        String distPart = cfg.showDistance ? String.format(Locale.ROOT, " [%.0fm]", dist) : null;

        String nameText = sb.toString();
        int nameW = font.getWidth(nameText);
        int hpW = hpPart != null ? font.getWidth(hpPart) : 0;
        int distW = distPart != null ? font.getWidth(distPart) : 0;
        int totalW = nameW + hpW + distW;

        int padX = 4;
        int cardW = totalW + padX * 2 + 2;
        int cardH = font.fontHeight + 4;
        int cardX = Math.max(2, Math.min(g.getScaledWindowWidth() - cardW - 2, centerX - cardW / 2));
        int cardY = Math.max(2, bottomY - cardH);

        int bg = ColorUtil.withAlpha(0x0B101B, 0.78f * fade);
        int border = ColorUtil.withAlpha(0x1E293B, 0.85f * fade);
        int accent = ColorUtil.withAlpha(rgb, 0.95f * fade);

        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);
        drawRectOutline(g, cardX, cardY, cardW, cardH, border);
        g.fill(cardX, cardY, cardX + 2, cardY + cardH, accent);

        int tx = cardX + padX + 2;
        int ty = cardY + 2;
        g.drawText(font, nameText, tx, ty, ColorUtil.withAlpha(0xF1F5F9, 0.96f * fade), true);
        tx += nameW;

        if (hpPart != null) {
            g.drawText(font, hpPart, tx, ty, ColorUtil.withAlpha(hpRgb, 0.96f * fade), true);
            tx += hpW;
        }
        if (distPart != null) {
            g.drawText(font, distPart, tx, ty, ColorUtil.withAlpha(0x94A3B8, 0.92f * fade), true);
        }
    }

    private static void renderStatusPill(DrawContext g, MinecraftClient client, EspConfig cfg, boolean bypassWalls) {
        TextRenderer font = client.textRenderer;
        String title = Text.translatable("hud.espmod.title").getString();
        String stateBadge = cfg.enabled
            ? Text.translatable(bypassWalls ? "hud.espmod.mode.full" : "hud.espmod.mode.los").getString()
            : "OFF";
        String countStr = cfg.enabled
            ? Text.translatable("hud.espmod.targets", Integer.toString(EspHudStats.getTotalTargets())).getString()
            : "";

        int titleW = font.getWidth(title);
        int badgeW = font.getWidth(stateBadge) + 6;
        int countW = countStr.isEmpty() ? 0 : font.getWidth(countStr) + 6;

        int pillW = 8 + titleW + 5 + badgeW + countW + 5;
        int pillH = font.fontHeight + 6;

        int margin = 6;
        int x = (cfg.statusHudCorner == HudCorner.TOP_RIGHT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT)
            ? g.getScaledWindowWidth() - pillW - margin
            : margin;
        int y = (cfg.statusHudCorner == HudCorner.BOTTOM_LEFT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT)
            ? g.getScaledWindowHeight() - pillH - margin
            : margin;

        int accentRgb = !cfg.enabled ? 0x64748B : (bypassWalls ? 0x38D6FC : 0xF5B841);

        g.fill(x, y, x + pillW, y + pillH, 0xCC0A0F1A);
        drawRectOutline(g, x, y, pillW, pillH, 0xCC1E2D42);
        g.fill(x, y, x + 2, y + pillH, 0xFF000000 | accentRgb);

        int ty = y + 3;
        int cx = x + 6;
        g.drawText(font, title, cx, ty, 0xFFE2E8F0, true);
        cx += titleW + 5;

        g.fill(cx, y + 2, cx + badgeW, y + pillH - 2, ColorUtil.withAlpha(accentRgb, 0.24f));
        drawRectOutline(g, cx, y + 2, badgeW, pillH - 4, ColorUtil.withAlpha(accentRgb, 0.65f));
        g.drawText(font, stateBadge, cx + 3, ty, 0xFF000000 | accentRgb, false);
        cx += badgeW;

        if (!countStr.isEmpty()) {
            g.drawText(font, countStr, cx + 5, ty, 0xFF94A3B8, true);
        }
    }
}
