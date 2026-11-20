package net.espmod.util;

import net.espmod.config.EspConfig;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;

public final class EntityClassifier {
    private EntityClassifier() {}

    public static boolean shouldTrackEntity(Entity entity, ClientPlayerEntity self, EspConfig cfg) {
        if (entity == null || entity == self || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return cfg.showPlayers;
        }
        if (entity instanceof ItemEntity) {
            return cfg.showItems;
        }
        if (entity instanceof VehicleEntity) {
            return cfg.showVehicles;
        }
        if (entity instanceof MerchantEntity || (entity instanceof TameableEntity tameable && tameable.isTamed())) {
            return cfg.showVillagersAndPets;
        }
        if (entity instanceof Monster || entity instanceof EnderDragonEntity) {
            return cfg.showHostileMobs;
        }
        if (entity instanceof MobEntity || entity instanceof AnimalEntity || entity instanceof AmbientEntity || entity instanceof Angerable) {
            return cfg.showPassiveMobs;
        }
        return false;
    }

    public static int getEntityColor(Entity entity, EspConfig cfg) {
        if (entity instanceof PlayerEntity) {
            return cfg.playerColor;
        }
        if (entity instanceof ItemEntity) {
            return cfg.itemColor;
        }
        if (entity instanceof VehicleEntity) {
            return cfg.vehicleColor;
        }
        if (entity instanceof MerchantEntity || (entity instanceof TameableEntity tameable && tameable.isTamed())) {
            return cfg.villagerPetColor;
        }
        if (entity instanceof Monster || entity instanceof EnderDragonEntity) {
            return cfg.hostileColor;
        }
        return cfg.passiveColor;
    }

    public static int getBlockEntityColor(BlockEntity be, EspConfig cfg) {
        if (!cfg.showStorage || be == null) {
            return -1;
        }
        if (be instanceof EnderChestBlockEntity) {
            return cfg.showEnderChests ? cfg.enderChestColor : -1;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return cfg.showShulkers ? cfg.shulkerColor : -1;
        }
        if (be instanceof MobSpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity || be instanceof VaultBlockEntity) {
            return cfg.showSpawners ? cfg.spawnerColor : -1;
        }
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof HopperBlockEntity) {
            return cfg.showChests ? cfg.chestColor : -1;
        }
        return -1;
    }
}
