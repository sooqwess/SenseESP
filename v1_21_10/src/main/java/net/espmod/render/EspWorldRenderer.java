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
    private record BoxEntry(Box box, int rgb, float fade, boolean isBlock) {}

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

            double ix = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double iy = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double iz = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Box box = entity.getBoundingBox()
                .offset(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .expand(0.04);
            entries.add(new BoxEntry(box, rgb, fade, false));
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
                            entries.add(new BoxEntry(blockBox, rgb, fade, true));
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

                if (cfg.renderBoxes) {
                    if (cfg.boxStyle == BoxStyle.CORNERS) {
                        drawCornerLines(matrices, lines, box, r, g, b, strokeA);
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

    private static void drawCornerLines(MatrixStack matrices, VertexConsumer lines, Box b, float r, float g, float bl, float a) {
        double lx = Math.max(0.08, (b.maxX - b.minX) * 0.28);
        double ly = Math.max(0.08, (b.maxY - b.minY) * 0.24);
        double lz = Math.max(0.08, (b.maxZ - b.minZ) * 0.28);

        double[] xs = {b.minX, b.maxX};
        double[] ys = {b.minY, b.maxY};
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
