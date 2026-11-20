package net.espmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class EspPolicy {
    private EspPolicy() {}

    public static boolean canBypassWalls(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        if (client.hasSingleplayerServer()) {
            return true;
        }
        LocalPlayer player = client.player;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean canRenderEntity(Minecraft client, Entity target) {
        if (client == null || client.player == null || client.level == null || target == null) {
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

    public static boolean hasLineOfSight(Minecraft client, Entity target) {
        Vec3 eyePos = client.player.getEyePosition(1.0f);
        AABB box = target.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;

        double[] sampleHeights = new double[] {
            target.getEyeY(),
            (box.minY + box.maxY) * 0.5,
            box.minY + Math.min(0.25, (box.maxY - box.minY) * 0.5)
        };

        for (double y : sampleHeights) {
            Vec3 end = new Vec3(cx, y, cz);
            HitResult hit = client.level.clip(new ClipContext(
                eyePos,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                client.player
            ));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }
}
