package net.espmod.util;

import net.espmod.config.EspConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;

public final class EntityClassifier {
    private EntityClassifier() {}

    public static boolean shouldTrackEntity(Entity entity, LocalPlayer self, EspConfig cfg) {
        if (entity == null || entity == self || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof Player) {
            return cfg.showPlayers;
        }
        if (entity instanceof ItemEntity) {
            return cfg.showItems;
        }
        if (entity instanceof VehicleEntity) {
            return cfg.showVehicles;
        }
        if (entity instanceof AbstractVillager || (entity instanceof TamableAnimal tamable && tamable.isTame())) {
            return cfg.showVillagersAndPets;
        }
        if (entity instanceof Enemy || entity instanceof EnderDragon) {
            return cfg.showHostileMobs;
        }
        if (entity instanceof Mob || entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof NeutralMob) {
            return cfg.showPassiveMobs;
        }
        return false;
    }

    public static int getEntityColor(Entity entity, EspConfig cfg) {
        if (entity instanceof Player) {
            return cfg.playerColor;
        }
        if (entity instanceof ItemEntity) {
            return cfg.itemColor;
        }
        if (entity instanceof VehicleEntity) {
            return cfg.vehicleColor;
        }
        if (entity instanceof AbstractVillager || (entity instanceof TamableAnimal tamable && tamable.isTame())) {
            return cfg.villagerPetColor;
        }
        if (entity instanceof Enemy || entity instanceof EnderDragon) {
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
        if (be instanceof SpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity || be instanceof VaultBlockEntity) {
            return cfg.showSpawners ? cfg.spawnerColor : -1;
        }
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof HopperBlockEntity) {
            return cfg.showChests ? cfg.chestColor : -1;
        }
        return -1;
    }
}
