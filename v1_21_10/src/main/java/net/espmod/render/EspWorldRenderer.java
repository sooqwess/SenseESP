package net.espmod.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.espmod.config.BoxStyle;
import net.espmod.config.EspConfig;
import net.espmod.util.ColorUtil;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class EspWorldRenderer {
    private record BoxEntry(Box box, int rgb, float fade, boolean isBlock, int seed) {}

    private EspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();
        if (consumers == null || matrices == null) return;
        float td = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        renderDirect(matrices, consumers, context.worldState().cameraRenderState.pos, td);
    }

    public static void renderDirect(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camPos, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || client.world == null || client.player == null) {
            EspHudStats.update(0, 0);
            return;
        }

        boolean bypassWalls = EspPolicy.canBypassWalls(client);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        double animSec = ColorUtil.animSeconds();
        Entity crosshair = client.targetedEntity;
        List<BoxEntry> entries = new ArrayList<>();

        int entityCount = 0;
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
            entityCount++;

            if (!cfg.renderBoxes && !cfg.renderTracers) {
                continue;
            }

            float fade = ColorUtil.computeDistanceAlpha(Math.sqrt(distSq), cfg.maxDistance, cfg.distanceFade);
            int rgb = EntityClassifier.getEntityColor(entity, cfg);
            if (cfg.hitFlashHighlight && entity instanceof LivingEntity living && living.hurtTime > 0) {
                float ht = Math.min(1.0f, living.hurtTime / 10.0f);
                rgb = ColorUtil.lerpRgb(rgb, 0xFF2E4C, ht * 0.85f);
            }
            if (crosshair != null && crosshair.getId() == entity.getId()) {
                rgb = ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.25f);
            }

            double ix = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double iy = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double iz = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Box box = entity.getBoundingBox()
                .offset(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .expand(0.04);
            entries.add(new BoxEntry(box, rgb, fade, false, entity.getId()));
        }

        int blockCount = 0;
        if (bypassWalls && cfg.showStorage) {
            int chunkRadius = Math.min(8, Math.max(2, (int) Math.ceil(cfg.maxDistance / 16.0)));
            int pChunkX = client.player.getChunkPos().x;
            int pChunkZ = client.player.getChunkPos().z;

            for (int cx = pChunkX - chunkRadius; cx <= pChunkX + chunkRadius; cx++) {
                for (int cz = pChunkZ - chunkRadius; cz <= pChunkZ + chunkRadius; cz++) {
                    WorldChunk chunk = client.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        int rgb = EntityClassifier.getBlockEntityColor(be, cfg);
                        if (rgb < 0) {
                            continue;
                        }
                        BlockPos pos = be.getPos();
                        double distSq = client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (distSq > maxDistSq) {
                            continue;
                        }
                        blockCount++;
                        if (cfg.renderBoxes) {
                            float fade = ColorUtil.computeDistanceAlpha(Math.sqrt(distSq), cfg.maxDistance, cfg.distanceFade);
                            Box blockBox = new Box(
                                pos.getX() + 0.04, pos.getY() + 0.04, pos.getZ() + 0.04,
                                pos.getX() + 0.96, pos.getY() + 0.96, pos.getZ() + 0.96
                            );
                            entries.add(new BoxEntry(blockBox, rgb, fade, true, pos.hashCode()));
                        }
                    }
                }
            }
        }

        EspHudStats.update(entityCount, blockCount);

        if (entries.isEmpty() || (!cfg.renderBoxes && !cfg.renderTracers)) {
            return;
        }

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        if (bypassWalls) {
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
        } else {
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask(true);
        }
        GlStateManager._disableCull();
        GlStateManager._enableBlend();
        RenderSystem.lineWidth(cfg.lineWidth);

        if (cfg.renderBoxes && cfg.boxStyle == BoxStyle.FILLED) {
            VertexConsumer filled = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            for (BoxEntry e : entries) {
                Box box = e.box();
                float r = ColorUtil.red(e.rgb());
                float g = ColorUtil.green(e.rgb());
                float b = ColorUtil.blue(e.rgb());
                float fillA = (e.isBlock() ? cfg.fillOpacity * 0.85f : cfg.fillOpacity) * e.fade();
                VertexRendering.drawFilledBox(matrices, filled, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, fillA);
            }
            if (consumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw(RenderLayer.getDebugFilledBox());
            }
        }

        if (cfg.renderBoxes || cfg.renderTracers) {
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            Vec3d tracerOrigin = null;
            if (cfg.renderTracers) {
                Vec3d look = client.player.getRotationVec(tickDelta);
                tracerOrigin = camPos.add(look.multiply(0.35));
            }

            for (BoxEntry e : entries) {
                Box box = e.box();
                float r = ColorUtil.red(e.rgb());
                float g = ColorUtil.green(e.rgb());
                float b = ColorUtil.blue(e.rgb());
                float strokeA = 0.92f * e.fade();
                BoxStyle style = e.isBlock() && (cfg.boxStyle == BoxStyle.PULSE_RING || cfg.boxStyle == BoxStyle.SPIRAL)
                    ? BoxStyle.CORNERS
                    : cfg.boxStyle;

                if (cfg.renderBoxes) {
                    if (style == BoxStyle.CORNERS) {
                        drawCornerLines(matrices, lines, box, r, g, b, strokeA);
                    } else if (style == BoxStyle.PULSE_RING) {
                        drawPulseRing(matrices, lines, box, e.rgb(), e.fade(), animSec, e.seed());
                    } else if (style == BoxStyle.SPIRAL) {
                        drawSpiral(matrices, lines, box, e.rgb(), e.fade(), animSec, e.seed());
                    } else {
                        VertexRendering.drawBox(matrices.peek(), lines, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, strokeA);
                    }
                }

                if (cfg.renderTracers && tracerOrigin != null && !e.isBlock()) {
                    drawLine(
                        matrices, lines,
                        tracerOrigin.x, tracerOrigin.y, tracerOrigin.z,
                        (box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5,
                        r, g, b, 0.72f * e.fade()
                    );
                }
            }

            if (consumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw(RenderLayer.getLines());
            }
        }

        RenderSystem.lineWidth(1.0f);
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
        GlStateManager._enableCull();
        matrices.pop();
    }

    private static void drawPulseRing(MatrixStack matrices, VertexConsumer lines, Box b, int rgb, float fade, double animSec, int seed) {
        double cx = (b.minX + b.maxX) * 0.5;
        double cz = (b.minZ + b.maxZ) * 0.5;
        double radius = Math.max(0.32, Math.max(b.maxX - b.minX, b.maxZ - b.minZ) * 0.62);
        double phase = animSec * 2.1 + (seed & 31) * 0.37;
        double wave = 0.5 + 0.5 * Math.sin(phase);
        double ry = b.minY + 0.06 + (b.maxY - b.minY - 0.12) * wave;

        int hi = ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.28f);
        float hr = ColorUtil.red(hi), hg = ColorUtil.green(hi), hb = ColorUtil.blue(hi);
        float br = ColorUtil.red(rgb), bg = ColorUtil.green(rgb), bb = ColorUtil.blue(rgb);

        int segs = 20;
        for (int i = 0; i < segs; i++) {
            double a0 = (Math.PI * 2.0 * i) / segs + animSec * 0.8;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segs + animSec * 0.8;
            double x0 = cx + Math.cos(a0) * radius, z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius, z1 = cz + Math.sin(a1) * radius;
            drawLine(matrices, lines, x0, ry, z0, x1, ry, z1, hr, hg, hb, 0.94f * fade);
            if ((i & 1) == 0) {
                drawLine(matrices, lines, x0, b.minY + 0.03, z0, x1, b.minY + 0.03, z1, br, bg, bb, 0.42f * fade);
            }
        }
    }

    private static void drawSpiral(MatrixStack matrices, VertexConsumer lines, Box b, int rgb, float fade, double animSec, int seed) {
        double cx = (b.minX + b.maxX) * 0.5;
        double cz = (b.minZ + b.maxZ) * 0.5;
        double radius = Math.max(0.30, Math.max(b.maxX - b.minX, b.maxZ - b.minZ) * 0.58);
        double height = Math.max(0.25, b.maxY - b.minY);
        double rot = animSec * 2.8 + (seed & 15) * 0.4;
        int steps = 18;

        int hi = ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.22f);
        for (int strand = 0; strand < 2; strand++) {
            double offset = strand * Math.PI;
            int col = (strand == 0) ? hi : rgb;
            float cr = ColorUtil.red(col), cg = ColorUtil.green(col), cb = ColorUtil.blue(col);
            float ca = ((strand == 0) ? 0.92f : 0.68f) * fade;
            for (int i = 0; i < steps; i++) {
                double t0 = (double) i / steps;
                double t1 = (double) (i + 1) / steps;
                double a0 = rot + offset + t0 * Math.PI * 2.4;
                double a1 = rot + offset + t1 * Math.PI * 2.4;
                double r0 = radius * (0.88 + 0.12 * Math.sin(t0 * Math.PI));
                double r1 = radius * (0.88 + 0.12 * Math.sin(t1 * Math.PI));
                drawLine(
                    matrices, lines,
                    cx + Math.cos(a0) * r0, b.minY + height * t0, cz + Math.sin(a0) * r0,
                    cx + Math.cos(a1) * r1, b.minY + height * t1, cz + Math.sin(a1) * r1,
                    cr, cg, cb, ca
                );
            }
        }
    }

    private static void drawCornerLines(MatrixStack matrices, VertexConsumer lines, Box b, float r, float g, float bl, float a) {
        double lx = Math.max(0.08, (b.maxX - b.minX) * 0.28);
        double ly = Math.max(0.08, (b.maxY - b.minY) * 0.24);
        double lz = Math.max(0.08, (b.maxZ - b.minZ) * 0.28);

        double[] xs = {b.minX, b.maxX};
        double[] ys = {b.minY + 0.02, b.maxY};
        double[] zs = {b.minZ, b.maxZ};

        for (int xi = 0; xi < 2; xi++) {
            double x = xs[xi];
            double dx = (xi == 0) ? lx : -lx;
            for (int yi = 0; yi < 2; yi++) {
                double y = ys[yi];
                double dy = (yi == 0) ? ly : -ly;
                for (int zi = 0; zi < 2; zi++) {
                    double z = zs[zi];
                    double dz = (zi == 0) ? lz : -lz;
                    drawLine(matrices, lines, x, y, z, x + dx, y, z, r, g, bl, a);
                    drawLine(matrices, lines, x, y, z, x, y + dy, z, r, g, bl, a);
                    drawLine(matrices, lines, x, y, z, x, y, z + dz, r, g, bl, a);
                }
            }
        }
    }

    private static void drawLine(
        MatrixStack matrices, VertexConsumer buffer,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        float r, float g, float b, float a
    ) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6f) {
            return;
        }
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;
        buffer.vertex(posMat, (float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        buffer.vertex(posMat, (float) x2, (float) y2, (float) z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }
}
