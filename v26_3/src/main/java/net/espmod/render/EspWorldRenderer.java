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
import net.minecraft.world.entity.LivingEntity;
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
        double animSec = ColorUtil.animSeconds();
        Entity crosshair = client.crosshairPickEntity;

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
            float extraWidth = 0.0f;

            if (cfg.hitFlashHighlight && entity instanceof LivingEntity living && living.hurtTime > 0) {
                float ht = Math.min(1.0f, living.hurtTime / 10.0f);
                rgb = ColorUtil.lerpRgb(rgb, 0xFF2E4C, ht * 0.85f);
                extraWidth += ht * 0.8f;
            }
            boolean isLocked = (crosshair != null && crosshair.getId() == entity.getId());
            if (isLocked) {
                rgb = ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.25f);
                extraWidth += 0.6f;
            }

            double ix = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double iy = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double iz = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            AABB box = entity.getBoundingBox()
                .move(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ())
                .inflate(0.04);

            if (cfg.renderBoxes) {
                emitBox(box, rgb, fade, cfg.lineWidth + extraWidth, cfg.fillOpacity, cfg.boxStyle, bypassWalls, animSec, entity.getId());
            }
            if (cfg.renderTracers && tracerOrigin != null) {
                Vec3 center = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
                GizmoProperties line = Gizmos.line(tracerOrigin, center, ColorUtil.withAlpha(rgb, 0.72f * fade), Math.max(1.0f, (cfg.lineWidth + extraWidth) * 0.85f));
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
                        BoxStyle bStyle = (cfg.boxStyle == BoxStyle.PULSE_RING || cfg.boxStyle == BoxStyle.SPIRAL) ? BoxStyle.CORNERS : cfg.boxStyle;
                        emitBox(blockBox, rgb, fade, cfg.lineWidth, cfg.fillOpacity * 0.85f, bStyle, true, animSec, pos.hashCode());
                    }
                }
            }
        }

        EspHudStats.update(entityCount, blockCount);
    }

    private static void emitBox(AABB box, int rgb, float fade, float lineWidth, float fillOpacity, BoxStyle style, boolean alwaysOnTop, double animSec, int seed) {
        int strokeArgb = ColorUtil.withAlpha(rgb, 0.92f * fade);
        if (style == BoxStyle.CORNERS) {
            emitCornerBox(box, strokeArgb, lineWidth, alwaysOnTop);
            return;
        }
        if (style == BoxStyle.PULSE_RING) {
            emitPulseRing(box, rgb, fade, lineWidth, alwaysOnTop, animSec, seed);
            return;
        }
        if (style == BoxStyle.SPIRAL) {
            emitSpiral(box, rgb, fade, lineWidth, alwaysOnTop, animSec, seed);
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

    private static void emitPulseRing(AABB b, int rgb, float fade, float width, boolean alwaysOnTop, double animSec, int seed) {
        double cx = (b.minX + b.maxX) * 0.5;
        double cz = (b.minZ + b.maxZ) * 0.5;
        double radius = Math.max(0.32, Math.max(b.maxX - b.minX, b.maxZ - b.minZ) * 0.62);
        double phase = animSec * 2.1 + (seed & 31) * 0.37;
        double wave = 0.5 + 0.5 * Math.sin(phase);
        double ry = b.minY + 0.06 + (b.maxY - b.minY - 0.12) * wave;

        int ringMain = ColorUtil.withAlpha(ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.28f), 0.94f * fade);
        int ringBase = ColorUtil.withAlpha(rgb, 0.42f * fade);
        int segs = 20;
        for (int i = 0; i < segs; i++) {
            double a0 = (Math.PI * 2.0 * i) / segs + animSec * 0.8;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segs + animSec * 0.8;
            double x0 = cx + Math.cos(a0) * radius, z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius, z1 = cz + Math.sin(a1) * radius;
            addSeg(new Vec3(x0, ry, z0), new Vec3(x1, ry, z1), ringMain, width + 0.4f, alwaysOnTop);
            if ((i & 1) == 0) {
                addSeg(new Vec3(x0, b.minY + 0.03, z0), new Vec3(x1, b.minY + 0.03, z1), ringBase, Math.max(1.0f, width * 0.75f), alwaysOnTop);
            }
        }
    }

    private static void emitSpiral(AABB b, int rgb, float fade, float width, boolean alwaysOnTop, double animSec, int seed) {
        double cx = (b.minX + b.maxX) * 0.5;
        double cz = (b.minZ + b.maxZ) * 0.5;
        double radius = Math.max(0.30, Math.max(b.maxX - b.minX, b.maxZ - b.minZ) * 0.58);
        double height = Math.max(0.25, b.maxY - b.minY);
        double rot = animSec * 2.8 + (seed & 15) * 0.4;
        int steps = 18;
        int colA = ColorUtil.withAlpha(ColorUtil.lerpRgb(rgb, 0xFFFFFF, 0.22f), 0.92f * fade);
        int colB = ColorUtil.withAlpha(rgb, 0.68f * fade);

        for (int strand = 0; strand < 2; strand++) {
            double offset = strand * Math.PI;
            int col = (strand == 0) ? colA : colB;
            for (int i = 0; i < steps; i++) {
                double t0 = (double) i / steps;
                double t1 = (double) (i + 1) / steps;
                double a0 = rot + offset + t0 * Math.PI * 2.4;
                double a1 = rot + offset + t1 * Math.PI * 2.4;
                double r0 = radius * (0.88 + 0.12 * Math.sin(t0 * Math.PI));
                double r1 = radius * (0.88 + 0.12 * Math.sin(t1 * Math.PI));
                Vec3 p0 = new Vec3(cx + Math.cos(a0) * r0, b.minY + height * t0, cz + Math.sin(a0) * r0);
                Vec3 p1 = new Vec3(cx + Math.cos(a1) * r1, b.minY + height * t1, cz + Math.sin(a1) * r1);
                addSeg(p0, p1, col, width, alwaysOnTop);
            }
        }
    }

    private static void emitCornerBox(AABB b, int argb, float width, boolean alwaysOnTop) {
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
