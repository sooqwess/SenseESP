package net.espmod.render;

import net.espmod.config.BoxStyle;
import net.espmod.config.EspConfig;
import net.espmod.util.ColorUtil;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

public final class EspWorldRenderer {
    private EspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || client.world == null || client.player == null) {
            EspHudStats.update(0, 0);
            return;
        }

        boolean bypassWalls = EspPolicy.canBypassWalls(client);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        Vec3d camPos = context.worldState().cameraRenderState.pos;
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);

        Vec3d tracerOrigin = null;
        if (cfg.renderTracers) {
            Vec3d look = client.player.getRotationVec(tickDelta);
            tracerOrigin = camPos.add(look.multiply(0.35));
        }

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

            double dist = Math.sqrt(distSq);
            float fade = ColorUtil.computeDistanceAlpha(dist, cfg.maxDistance, cfg.distanceFade);
            int rgb = EntityClassifier.getEntityColor(entity, cfg);

            double ix = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double iy = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double iz = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Box box = entity.getBoundingBox()
                .offset(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .expand(0.04);

            if (cfg.renderBoxes) {
                emitBox(box, rgb, fade, cfg.lineWidth, cfg.fillOpacity, cfg.boxStyle, bypassWalls);
            }
            if (cfg.renderTracers && tracerOrigin != null) {
                Vec3d center = new Vec3d((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
                VisibilityConfigurable line = GizmoDrawing.line(tracerOrigin, center, ColorUtil.withAlpha(rgb, 0.72f * fade), Math.max(1.0f, cfg.lineWidth * 0.85f));
                if (bypassWalls) {
                    line.ignoreOcclusion();
                }
            }
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
                        float fade = ColorUtil.computeDistanceAlpha(Math.sqrt(distSq), cfg.maxDistance, cfg.distanceFade);
                        Box blockBox = new Box(
                            pos.getX() + 0.04, pos.getY() + 0.04, pos.getZ() + 0.04,
                            pos.getX() + 0.96, pos.getY() + 0.96, pos.getZ() + 0.96
                        );
                        emitBox(blockBox, rgb, fade, cfg.lineWidth, cfg.fillOpacity * 0.85f, cfg.boxStyle, true);
                    }
                }
            }
        }

        EspHudStats.update(entityCount, blockCount);
    }

    private static void emitBox(Box box, int rgb, float fade, float lineWidth, float fillOpacity, BoxStyle style, boolean ignoreOcclusion) {
        int strokeArgb = ColorUtil.withAlpha(rgb, 0.92f * fade);
        if (style == BoxStyle.CORNERS) {
            emitCornerBox(box, strokeArgb, lineWidth, ignoreOcclusion);
            return;
        }
        DrawStyle drawStyle = (style == BoxStyle.FILLED)
            ? DrawStyle.filledAndStroked(strokeArgb, lineWidth, ColorUtil.withAlpha(rgb, fillOpacity * fade))
            : DrawStyle.stroked(strokeArgb, lineWidth);
        VisibilityConfigurable gizmo = GizmoDrawing.box(box, drawStyle);
        if (ignoreOcclusion) {
            gizmo.ignoreOcclusion();
        }
    }

    private static void emitCornerBox(Box b, int argb, float width, boolean ignoreOcclusion) {
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
                    Vec3d corner = new Vec3d(x, y, z);
                    addSeg(corner, new Vec3d(x + dx, y, z), argb, width, ignoreOcclusion);
                    addSeg(corner, new Vec3d(x, y + dy, z), argb, width, ignoreOcclusion);
                    addSeg(corner, new Vec3d(x, y, z + dz), argb, width, ignoreOcclusion);
                }
            }
        }
    }

    private static void addSeg(Vec3d a, Vec3d b, int argb, float width, boolean ignoreOcclusion) {
        VisibilityConfigurable p = GizmoDrawing.line(a, b, argb, width);
        if (ignoreOcclusion) {
            p.ignoreOcclusion();
        }
    }
}
