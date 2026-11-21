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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EspHudRenderer {
    private record RadarDot(double dx, double dz, int rgb, boolean locked) {}

    private EspHudRenderer() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }

        EspHudStats.beginHudFrame();
        EspConfig cfg = EspConfig.get();
        boolean bypassWalls = EspPolicy.canBypassWalls(client);

        int pillBottomY = 6;
        if (cfg.showStatusHud) {
            pillBottomY = renderStatusPill(context, client, cfg, bypassWalls);
        }

        if (!cfg.enabled) {
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

        float tickDelta = tickCounter.getTickProgress(false);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        TextRenderer font = client.textRenderer;
        Entity crosshair = client.targetedEntity;
        List<RadarDot> radarDots = cfg.showRadar ? new ArrayList<>() : null;

        boolean needScreenPass = cfg.render2DOverlay || cfg.renderNametags || cfg.showDirectionPointers || cfg.showRadar;
        if (needScreenPass) {
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
                boolean isLocked = (crosshair != null && crosshair.getId() == entity.getId());

                if (cfg.hitFlashHighlight && entity instanceof LivingEntity lv && lv.hurtTime > 0) {
                    float ht = Math.min(1.0f, lv.hurtTime / 10.0f);
                    rgb = ColorUtil.lerpRgb(rgb, 0xFF2E4C, ht * 0.85f);
                }

                double ix = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
                double iy = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
                double iz = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

                if (radarDots != null) {
                    double px = MathHelper.lerp(tickDelta, client.player.lastRenderX, client.player.getX());
                    double pz = MathHelper.lerp(tickDelta, client.player.lastRenderZ, client.player.getZ());
                    radarDots.add(new RadarDot(ix - px, iz - pz, rgb, isLocked));
                }

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

                boolean onScreen = visibleCorners > 0 && maxSX >= 0 && minSX <= sw && maxSY >= 0 && minSY <= sh;
                if (!onScreen) {
                    if (cfg.showDirectionPointers && entity instanceof LivingEntity) {
                        Vector3f centerRel = new Vector3f(
                            (float) (ix - camPos.x),
                            (float) (iy + entity.getHeight() * 0.5 - camPos.y),
                            (float) (iz - camPos.z)
                        );
                        invCamRot.transform(centerRel);
                        drawOffscreenPointer(context, sw, sh, centerRel.x, -centerRel.y, rgb, fade);
                    }
                    continue;
                }

                int x1 = Math.max(2, (int) Math.floor(minSX));
                int y1 = Math.max(2, (int) Math.floor(minSY));
                int x2 = Math.min(sw - 2, (int) Math.ceil(maxSX));
                int y2 = Math.min(sh - 2, (int) Math.ceil(maxSY));
                int bw = x2 - x1;
                int bh = y2 - y1;

                if (cfg.render2DOverlay && bw >= 3 && bh >= 3 && bw < sw * 0.92 && bh < sh * 0.92) {
                    int strokeColor = ColorUtil.withAlpha(isLocked ? ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.35f) : rgb, 0.92f * fade);
                    int shadowColor = ColorUtil.withAlpha(0x040810, 0.72f * fade);
                    if (cfg.corner2DBox) {
                        drawCornerBrackets(context, x1, y1, x2, y2, strokeColor, shadowColor, isLocked);
                    } else {
                        drawRectOutline(context, x1 - 1, y1 - 1, bw + 2, bh + 2, shadowColor);
                        drawRectOutline(context, x1, y1, bw, bh, strokeColor);
                    }

                    if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f && bh >= 8) {
                        float rawRatio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
                        float ratio = EspHudStats.smoothHealth(entity.getId(), rawRatio);
                        int barX = Math.max(1, x1 - 4);
                        context.fill(barX - 1, y1 - 1, barX + 2, y2 + 1, ColorUtil.withAlpha(0x060A12, 0.84f * fade));
                        int filledH = Math.max(1, Math.round(bh * ratio));
                        int hpColor = ColorUtil.withAlpha(ColorUtil.healthColor(ratio), 0.96f * fade);
                        context.fill(barX, y2 - filledH, barX + 1, y2, hpColor);
                        if (living.getAbsorptionAmount() > 0.0f) {
                            int absH = Math.max(1, Math.min(bh, Math.round(bh * Math.min(1.0f, living.getAbsorptionAmount() / living.getMaxHealth()))));
                            context.fill(barX + 1, y2 - absH, barX + 2, y2, ColorUtil.withAlpha(0xFACC15, 0.92f * fade));
                        }
                    }
                }

                if (cfg.renderNametags) {
                    renderNametagCard(context, font, entity, dist, rgb, fade, (x1 + x2) / 2, Math.max(14, y1 - 4), cfg);
                }
            }
        }

        if (cfg.showRadar && radarDots != null) {
            renderMiniRadar(context, client, cfg, radarDots, pillBottomY);
        }

        if (cfg.showTargetHud && crosshair instanceof LivingEntity target && target != client.player) {
            if (EspPolicy.canRenderEntity(client, target)) {
                renderTargetHudCard(context, font, client, target, cfg);
            }
        }
    }

    private static void drawOffscreenPointer(DrawContext g, int sw, int sh, float vx, float vy, int rgb, float fade) {
        double len = Math.hypot(vx, vy);
        if (len < 1.0E-4) return;
        double nx = vx / len;
        double ny = vy / len;
        double radius = Math.min(sw, sh) * 0.24;
        int px = (int) Math.round(sw * 0.5 + nx * radius);
        int py = (int) Math.round(sh * 0.5 + ny * radius);

        int outer = ColorUtil.withAlpha(0x050912, 0.78f * fade);
        int inner = ColorUtil.withAlpha(rgb, 0.92f * fade);
        int tipX = (int) Math.round(px + nx * 3.0);
        int tipY = (int) Math.round(py + ny * 3.0);

        g.fill(px - 2, py - 2, px + 3, py + 3, outer);
        g.fill(px - 1, py - 1, px + 2, py + 2, inner);
        g.fill(tipX - 1, tipY - 1, tipX + 1, tipY + 1, ColorUtil.withAlpha(0xFFFFFF, 0.80f * fade));
    }

    private static void renderMiniRadar(DrawContext g, MinecraftClient client, EspConfig cfg, List<RadarDot> dots, int topAnchorY) {
        int size = cfg.radarSize;
        int margin = 6;
        int sw = g.getScaledWindowWidth();
        int sh = g.getScaledWindowHeight();
        boolean right = (cfg.statusHudCorner == HudCorner.TOP_RIGHT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT);
        boolean bottom = (cfg.statusHudCorner == HudCorner.BOTTOM_LEFT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT);

        int x = right ? sw - size - margin : margin;
        int y = bottom
            ? (cfg.showStatusHud ? topAnchorY - size - 4 : sh - size - margin)
            : (cfg.showStatusHud ? topAnchorY + 4 : margin);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 2 - 3;

        g.fill(x, y, x + size, y + size, 0xCC070D14);
        drawRectOutline(g, x, y, size, size, 0xCC1E3A34);
        g.fill(x + 1, y + 1, x + size - 1, y + 2, 0x4434D399);

        int tk = 4;
        int tickCol = 0xCC34D399;
        g.fill(x, y, x + tk, y + 1, tickCol);
        g.fill(x, y, x + 1, y + tk, tickCol);
        g.fill(x + size - tk, y, x + size, y + 1, tickCol);
        g.fill(x + size - 1, y, x + size, y + tk, tickCol);
        g.fill(x, y + size - 1, x + tk, y + size, tickCol);
        g.fill(x, y + size - tk, x + 1, y + size, tickCol);
        g.fill(x + size - tk, y + size - 1, x + size, y + size, tickCol);
        g.fill(x + size - 1, y + size - tk, x + size, y + size, tickCol);

        g.fill(x + 3, cy, x + size - 3, cy + 1, 0x3364748B);
        g.fill(cx, y + 3, cx + 1, y + size - 3, 0x3364748B);
        int ringHalf = half / 2;
        drawRectOutline(g, cx - ringHalf, cy - ringHalf, ringHalf * 2 + 1, ringHalf * 2 + 1, 0x2634D399);

        for (int i = 2; i <= half - 2; i += 2) {
            int spread = (i * 5) / 8;
            g.fill(cx - spread, cy - i, cx - spread + 1, cy - i + 1, 0x3838D6FC);
            g.fill(cx + spread, cy - i, cx + spread + 1, cy - i + 1, 0x3838D6FC);
        }

        double yawRad = Math.toRadians(client.player.getYaw());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double range = Math.max(16.0, Math.min(cfg.maxDistance, 64.0));

        for (RadarDot dot : dots) {
            double forward = -dot.dx() * sin + dot.dz() * cos;
            double rightOff = -dot.dx() * cos - dot.dz() * sin;

            double nx = MathHelper.clamp(rightOff / range, -1.0, 1.0);
            double ny = MathHelper.clamp(-forward / range, -1.0, 1.0);

            int px = cx + (int) Math.round(nx * half);
            int py = cy + (int) Math.round(ny * half);

            if (dot.locked()) {
                g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFFFFF);
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFF000000 | dot.rgb());
            } else {
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xCC050811);
                g.fill(px, py, px + 1, py + 1, 0xFF000000 | dot.rgb());
            }
        }

        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFE2F8F0);
        g.fill(cx, cy, cx + 1, cy + 1, 0xFF34D399);
    }

    private static void renderTargetHudCard(DrawContext g, TextRenderer font, MinecraftClient client, LivingEntity target, EspConfig cfg) {
        String name = target.getDisplayName().getString();
        double dist = client.player.distanceTo(target);
        float maxHp = Math.max(1.0f, target.getMaxHealth());
        float curHp = Math.max(0.0f, target.getHealth());
        float absHp = Math.max(0.0f, target.getAbsorptionAmount());
        float ratio = EspHudStats.smoothHealth(target.getId(), MathHelper.clamp(curHp / maxHp, 0.0f, 1.0f));
        int armor = target.getArmor();

        int accentRgb = EntityClassifier.getEntityColor(target, cfg);
        if (cfg.hitFlashHighlight && target.hurtTime > 0) {
            accentRgb = ColorUtil.lerpRgb(accentRgb, 0xFF2E4C, Math.min(1.0f, target.hurtTime / 10.0f) * 0.8f);
        }

        String hpText = absHp > 0.1f
            ? String.format(Locale.ROOT, "%.1f+%.0f HP", curHp, absHp)
            : String.format(Locale.ROOT, "%.1f / %.0f HP", curHp, maxHp);
        String metaText = armor > 0
            ? String.format(Locale.ROOT, "%.1fm · %d ARM", dist, armor)
            : String.format(Locale.ROOT, "%.1fm", dist);

        int textW = Math.max(font.getWidth(name) + 8 + font.getWidth(metaText), font.getWidth(hpText) + 36);
        int cardW = Math.max(122, textW + 14);
        int cardH = 30;

        int sw = g.getScaledWindowWidth();
        int sh = g.getScaledWindowHeight();
        int x = (sw - cardW) / 2;
        int y = sh / 2 + 18;
        if (y + cardH > sh - 42) {
            y = sh / 2 - cardH - 14;
        }

        g.fill(x, y, x + cardW, y + cardH, 0xD8080E16);
        drawRectOutline(g, x, y, cardW, cardH, 0xCC1E3246);
        g.fill(x + 1, y + 1, x + cardW - 1, y + 2, 0x33FFFFFF);
        g.fill(x, y, x + 2, y + cardH, 0xFF000000 | accentRgb);

        g.drawText(font, name, x + 6, y + 4, 0xFFF8FAFC, true);
        g.drawText(font, metaText, x + cardW - font.getWidth(metaText) - 5, y + 4, 0xFF94A3B8, true);

        int barX = x + 6;
        int barY = y + 16;
        int barW = cardW - 12;
        int barH = 4;
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC04070D);
        int fillW = Math.max(1, Math.round(barW * ratio));
        int hpCol = ColorUtil.healthColor(ratio);
        g.fill(barX, barY, barX + fillW, barY + barH, 0xFF000000 | hpCol);
        g.fill(barX, barY, barX + fillW, barY + 1, 0x55FFFFFF);

        if (absHp > 0.1f) {
            int absW = Math.min(barW, Math.max(2, Math.round(barW * Math.min(1.0f, absHp / maxHp))));
            g.fill(barX, barY + barH - 1, barX + absW, barY + barH, 0xFFFACC15);
        }

        g.drawText(font, hpText, x + 6, y + 21, 0xFFCBD5E1, false);
    }

    private static void drawRectOutline(DrawContext g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawCornerBrackets(DrawContext g, int x1, int y1, int x2, int y2, int color, int shadow, boolean locked) {
        int w = x2 - x1;
        int h = y2 - y1;
        int cx = Math.max(2, Math.min(locked ? 12 : 10, w / 3));
        int cy = Math.max(2, Math.min(locked ? 12 : 10, h / 3));

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
        int hpRgb = 0x34D399;
        if (cfg.showHealth && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0f) {
            float hp = living.getHealth() + living.getAbsorptionAmount();
            float ratio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0f, 1.0f);
            hpRgb = ColorUtil.healthColor(ratio);
            hpPart = String.format(Locale.ROOT, " %.0f♥", hp * 0.5f);
        }

        String distPart = cfg.showDistance ? String.format(Locale.ROOT, " · %.0fm", dist) : null;

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

        int bg = ColorUtil.withAlpha(0x080E16, 0.82f * fade);
        int border = ColorUtil.withAlpha(0x1E3246, 0.88f * fade);
        int accent = ColorUtil.withAlpha(rgb, 0.96f * fade);

        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);
        drawRectOutline(g, cardX, cardY, cardW, cardH, border);
        g.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 2, ColorUtil.withAlpha(0xFFFFFF, 0.10f * fade));
        g.fill(cardX, cardY, cardX + 2, cardY + cardH, accent);

        int tx = cardX + padX + 2;
        int ty = cardY + 2;
        g.drawText(font, nameText, tx, ty, ColorUtil.withAlpha(0xF8FAFC, 0.97f * fade), true);
        tx += nameW;

        if (hpPart != null) {
            g.drawText(font, hpPart, tx, ty, ColorUtil.withAlpha(hpRgb, 0.97f * fade), true);
            tx += hpW;
        }
        if (distPart != null) {
            g.drawText(font, distPart, tx, ty, ColorUtil.withAlpha(0x94A3B8, 0.92f * fade), true);
        }
    }

    private static int renderStatusPill(DrawContext g, MinecraftClient client, EspConfig cfg, boolean bypassWalls) {
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

        int accentRgb = !cfg.enabled ? 0x64748B : (bypassWalls ? 0x34D399 : 0xF5B841);

        g.fill(x, y, x + pillW, y + pillH, 0xD4080E16);
        drawRectOutline(g, x, y, pillW, pillH, 0xCC1E3538);
        g.fill(x + 1, y + 1, x + pillW - 1, y + 2, 0x26FFFFFF);
        g.fill(x, y, x + 2, y + pillH, 0xFF000000 | accentRgb);

        int ty = y + 3;
        int cx = x + 6;
        g.drawText(font, title, cx, ty, 0xFFF1F5F9, true);
        cx += titleW + 5;

        g.fill(cx, y + 2, cx + badgeW, y + pillH - 2, ColorUtil.withAlpha(accentRgb, 0.24f));
        drawRectOutline(g, cx, y + 2, badgeW, pillH - 4, ColorUtil.withAlpha(accentRgb, 0.68f));
        g.drawText(font, stateBadge, cx + 3, ty, 0xFF000000 | accentRgb, false);
        cx += badgeW;

        if (!countStr.isEmpty()) {
            g.drawText(font, countStr, cx + 5, ty, 0xFF94A3B8, true);
        }

        boolean bottom = (cfg.statusHudCorner == HudCorner.BOTTOM_LEFT || cfg.statusHudCorner == HudCorner.BOTTOM_RIGHT);
        return bottom ? y : (y + pillH);
    }
}
