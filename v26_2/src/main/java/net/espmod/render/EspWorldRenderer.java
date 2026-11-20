package net.espmod.render;

import net.espmod.config.BoxStyle;
import net.espmod.config.EspConfig;
import net.espmod.util.ColorUtil;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EspWorldRenderer {
    private EspWorldRenderer() {}

    public static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || client.level == null || client.player == null) {
            EspHudStats.update(0, 0);
            return;
        }

        boolean bypassWalls = EspPolicy.canBypassWalls(client);
        double maxDistSq = cfg.maxDistance * cfg.maxDistance;
        Vec3 camPos = context.levelState().cameraRenderState.pos;
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Vec3 tracerOrigin = null;
        if (cfg.renderTracers) {
            Vec3 look = client.player.getViewVector(partialTick);
            tracerOrigin = camPos.add(look.scale(0.35));
        }

        int entityCount = 0;
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
            entityCount++;

            if (!cfg.renderBoxes && !cfg.renderTracers) {
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

            if (cfg.renderBoxes) {
                emitBox(box, rgb, fade, cfg.lineWidth, cfg.fillOpacity, cfg.boxStyle, bypassWalls);
            }
            if (cfg.renderTracers && tracerOrigin != null) {
                Vec3 center = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
                GizmoProperties line = Gizmos.line(tracerOrigin, center, ColorUtil.withAlpha(rgb, 0.72f * fade), Math.max(1.0f, cfg.lineWidth * 0.85f));
                if (bypassWalls) {
                    line.setAlwaysOnTop();
                }
            }
        }

        int blockCount = 0;
        if (bypassWalls && cfg.showStorage) {
            int chunkRadius = Math.min(8, Math.max(2, (int) Math.ceil(cfg.maxDistance / 16.0)));
            int pChunkX = client.player.chunkPosition().x();
            int pChunkZ = client.player.chunkPosition().z();

            for (int cx = pChunkX - chunkRadius; cx <= pChunkX + chunkRadius; cx++) {
                for (int cz = pChunkZ - chunkRadius; cz <= pChunkZ + chunkRadius; cz++) {
                    LevelChunk chunk = client.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        int rgb = EntityClassifier.getBlockEntityColor(be, cfg);
                        if (rgb < 0) {
                            continue;
                        }
                        BlockPos pos = be.getBlockPos();
                        double distSq = client.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (distSq > maxDistSq) {
                            continue;
                        }
                        blockCount++;
                        float fade = ColorUtil.computeDistanceAlpha(Math.sqrt(distSq), cfg.maxDistance, cfg.distanceFade);
                        AABB blockBox = new AABB(
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

    private static void emitBox(AABB box, int rgb, float fade, float lineWidth, float fillOpacity, BoxStyle style, boolean alwaysOnTop) {
        int strokeArgb = ColorUtil.withAlpha(rgb, 0.92f * fade);
        if (style == BoxStyle.CORNERS) {
            emitCornerBox(box, strokeArgb, lineWidth, alwaysOnTop);
            return;
        }
        GizmoStyle gizmoStyle = (style == BoxStyle.FILLED)
            ? GizmoStyle.strokeAndFill(strokeArgb, lineWidth, ColorUtil.withAlpha(rgb, fillOpacity * fade))
            : GizmoStyle.stroke(strokeArgb, lineWidth);
        GizmoProperties cuboid = Gizmos.cuboid(box, gizmoStyle);
        if (alwaysOnTop) {
            cuboid.setAlwaysOnTop();
        }
    }

    private static void emitCornerBox(AABB b, int argb, float width, boolean alwaysOnTop) {
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
                    Vec3 corner = new Vec3(x, y, z);
                    addSeg(corner, new Vec3(x + dx, y, z), argb, width, alwaysOnTop);
                    addSeg(corner, new Vec3(x, y + dy, z), argb, width, alwaysOnTop);
                    addSeg(corner, new Vec3(x, y, z + dz), argb, width, alwaysOnTop);
                }
            }
        }
    }

    private static void addSeg(Vec3 a, Vec3 b, int argb, float width, boolean alwaysOnTop) {
        GizmoProperties p = Gizmos.line(a, b, argb, width);
        if (alwaysOnTop) {
            p.setAlwaysOnTop();
        }
    }
}
