package net.espmod.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class EspPolicy {
    private EspPolicy() {}

    public static boolean canBypassWalls(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return false;
        }
        if (client.isInSingleplayer()) {
            return true;
        }
        ClientPlayerEntity player = client.player;
        return player.hasPermissionLevel(2);
    }

    public static boolean canRenderEntity(MinecraftClient client, Entity target) {
        if (client == null || client.player == null || client.world == null || target == null) {
            return false;
        }
        if (canBypassWalls(client)) {
            return true;
        }
        if (target.isInvisible() || target.isInvisibleTo(client.player)) {
            return false;
        }
        return hasLineOfSight(client, target);
    }

    public static boolean hasLineOfSight(MinecraftClient client, Entity target) {
        Vec3d eyePos = client.player.getCameraPosVec(1.0f);
        Box box = target.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;

        double[] sampleHeights = new double[] {
            target.getEyeY(),
            (box.minY + box.maxY) * 0.5,
            box.minY + Math.min(0.25, (box.maxY - box.minY) * 0.5)
        };

        for (double y : sampleHeights) {
            Vec3d end = new Vec3d(cx, y, cz);
            HitResult hit = client.world.raycast(new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
            ));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }
}
