package net.espmod.render;

import net.espmod.config.EspConfig;
import net.espmod.config.HudCorner;
import net.espmod.util.ColorUtil;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;

public final class EspHudRenderer {
    private EspHudRenderer() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.gui.hud.isHidden()) {
            return;
        }

        EspConfig cfg = EspConfig.get();
        boolean bypassWalls = EspPolicy.canBypassWalls(client);

        if (cfg.showStatusHud) {
            renderStatusPill(graphics, client, cfg, bypassWalls);
        }

        if (!cfg.enabled || (!cfg.render2DOverlay && !cfg.renderNametags)) {
            return;
        }

        Camera camera = client.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invCamRot = new Quaternionf(camera.rotation()).conjugate();
        float fovDeg = Math.max(30.0f, Math.min(150.0f, camera.getFov()));
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        double halfH = sh * 0.5;
        double halfW = sw * 0.5;
        double focalLength = halfH / Math.tan(Math.toRadians(fovDeg * 0.5));

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        Font font = client.font;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!EntityClassifier.shouldTrackEntity(entity, client.player, cfg)) {
                continue;
            }
            double distSq = client.player.distanceToSqr(entity);
            if (distSq > maxDistSq) {
                continue;
            }
            if (!EspPolicy.canRenderEntity(client, entity)) {
                continue;
            }

            double dist = Math.sqrt(distSq);
            float fade = ColorUtil.computeDistanceAlpha(dist, cfg.maxDistance, cfg.distanceFade);
            int rgb = EntityClassifier.getEntityColor(entity, cfg);

            double ix = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double iy = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double iz = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            AABB box = entity.getBoundingBox()
                .move(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .inflate(0.04);

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
                    drawCornerBrackets(graphics, x1, y1, x2, y2, strokeColor, shadowColor);
                } else {
                    graphics.outline(x1 - 1, y1 - 1, bw + 2, bh + 2, shadowColor);
                    graphics.outline(x1, y1, bw, bh, strokeColor);
                }

                if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f && bh >= 8) {
                    float ratio = Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
                    int barX = Math.max(1, x1 - 4);
                    graphics.fill(barX - 1, y1 - 1, barX + 2, y2 + 1, ColorUtil.withAlpha(0x060A12, 0.80f * fade));
                    int filledH = Math.max(1, Math.round(bh * ratio));
                    int hpColor = ColorUtil.withAlpha(ColorUtil.healthColor(ratio), 0.95f * fade);
                    graphics.fill(barX, y2 - filledH, barX + 1, y2, hpColor);
                }
            }

            if (cfg.renderNametags) {
                renderNametagCard(graphics, font, entity, dist, rgb, fade, (x1 + x2) / 2, Math.max(14, y1 - 4), cfg);
            }
        }
    }

    private static void drawCornerBrackets(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, int shadow) {
        int w = x2 - x1;
        int h = y2 - y1;
        int cx = Math.max(2, Math.min(10, w / 3));
        int cy = Math.max(2, Math.min(10, h / 3));

        // Shadow pass
        g.fill(x1 - 1, y1 - 1, x1 + cx + 1, y1 + 2, shadow);
        g.fill(x1 - 1, y1 - 1, x1 + 2, y1 + cy + 1, shadow);
        g.fill(x2 - cx - 1, y1 - 1, x2 + 2, y1 + 2, shadow);
        g.fill(x2 - 1, y1 - 1, x2 + 2, y1 + cy + 1, shadow);
        g.fill(x1 - 1, y2 - 1, x1 + cx + 1, y2 + 2, shadow);
        g.fill(x1 - 1, y2 - cy - 1, x1 + 2, y2 + 2, shadow);
        g.fill(x2 - cx - 1, y2 - 1, x2 + 2, y2 + 2, shadow);
        g.fill(x2 - 1, y2 - cy - 1, x2 + 2, y2 + 2, shadow);

        // Crisp 1px corner lines
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
        GuiGraphicsExtractor g, Font font, Entity entity, double dist,
        int rgb, float fade, int centerX, int bottomY, EspConfig cfg
    ) {
        StringBuilder sb = new StringBuilder(entity.getDisplayName().getString());
        if (entity instanceof ItemEntity itemEntity && cfg.showItemCount) {
            int count = itemEntity.getItem().getCount();
            if (count > 1) {
                sb.append(" x").append(count);
            }
        }

        String hpPart = null;
        int hpRgb = 0x4ADE80;
        if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f) {
            float hp = living.getHealth() + living.getAbsorptionAmount();
            float ratio = Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
            hpRgb = ColorUtil.healthColor(ratio);
            hpPart = String.format(Locale.ROOT, " %.0f♥", hp * 0.5f);
        }

        String distPart = cfg.showDistance ? String.format(Locale.ROOT, " [%.0fm]", dist) : null;

        String nameText = sb.toString();
        int nameW = font.width(nameText);
        int hpW = hpPart != null ? font.width(hpPart) : 0;
        int distW = distPart != null ? font.width(distPart) : 0;
        int totalW = nameW + hpW + distW;

        int padX = 4;
        int cardW = totalW + padX * 2 + 2;
        int cardH = font.lineHeight + 4;
        int cardX = Math.max(2, Math.min(g.guiWidth() - cardW - 2, centerX - cardW / 2));
        int cardY = Math.max(2, bottomY - cardH);

        int bg = ColorUtil.withAlpha(0x0B101B, 0.78f * fade);
        int border = ColorUtil.withAlpha(0x1E293B, 0.85f * fade);
        int accent = ColorUtil.withAlpha(rgb, 0.95f * fade);

        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);
        g.outline(cardX, cardY, cardW, cardH, border);
        g.fill(cardX, cardY, cardX + 2, cardY + cardH, accent);

        int tx = cardX + padX + 2;
        int ty = cardY + 2;
        g.text(font, nameText, tx, ty, ColorUtil.withAlpha(0xF1F5F9, 0.96f * fade), true);
        tx += nameW;

        if (hpPart != null) {
            g.text(font, hpPart, tx, ty, ColorUtil.withAlpha(hpRgb, 0.96f * fade), true);
            tx += hpW;
        }
        if (distPart != null) {
            g.text(font, distPart, tx, ty, ColorUtil.withAlpha(0x94A3B8, 0.92f * fade), true);
        }
    }

    private static void renderStatusPill(GuiGraphicsExtractor g, Minecraft client, EspConfig cfg, boolean bypassWalls) {
        Font font = client.font;
        String title = Component.translatable("hud.espmod.title").getString();
        String stateBadge = cfg.enabled
            ? Component.translatable(bypassWalls ? "hud.espmod.mode.full" : "hud.espmod.mode.los").getString()
            : "OFF";
        String countStr = cfg.enabled
            ? Component.translatable("hud.espmod.targets", Integer.toString(EspHudStats.getTotalTargets())).getString()
            : "";

        int titleW = font.width(title);
        int badgeW = font.width(stateBadge) + 6;
        int countW = countStr.isEmpty() ? 0 : font.width(countStr) + 6;

        int pillW = 8 + titleW + 5 + badgeW + countW + 5;
        int pillH = font.lineHeight + 6;

        int margin = 6;
        int x = (cfg.statusHudCorner == HudCorner.TOP_RIGHT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT)
            ? g.guiWidth() - pillW - margin
            : margin;
        int y = (cfg.statusHudCorner == HudCorner.BOTTOM_LEFT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT)
            ? g.guiHeight() - pillH - margin
            : margin;

        int accentRgb = !cfg.enabled ? 0x64748B : (bypassWalls ? 0x38D6FC : 0xF5B841);

        g.fill(x, y, x + pillW, y + pillH, 0xCC0A0F1A);
        g.outline(x, y, pillW, pillH, 0xCC1E2D42);
        g.fill(x, y, x + 2, y + pillH, 0xFF000000 | accentRgb);

        int ty = y + 3;
        int cx = x + 6;
        g.text(font, title, cx, ty, 0xFFE2E8F0, true);
        cx += titleW + 5;

        g.fill(cx, y + 2, cx + badgeW, y + pillH - 2, ColorUtil.withAlpha(accentRgb, 0.24f));
        g.outline(cx, y + 2, badgeW, pillH - 4, ColorUtil.withAlpha(accentRgb, 0.65f));
        g.text(font, stateBadge, cx + 3, ty, 0xFF000000 | accentRgb, false);
        cx += badgeW;

        if (!countStr.isEmpty()) {
            g.text(font, countStr, cx + 5, ty, 0xFF94A3B8, true);
        }
    }
}
