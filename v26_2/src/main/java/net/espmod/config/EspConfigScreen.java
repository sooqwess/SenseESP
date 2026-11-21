package net.espmod.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EspConfigScreen {
    private EspConfigScreen() {}

    public static Screen create(Screen parent) {
        EspConfig cfg = EspConfig.get();
        EspConfig defaults = new EspConfig();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.espmod.title"))
            .setSavingRunnable(EspConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.espmod.category.general"));
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.enabled"), cfg.enabled)
            .setDefaultValue(defaults.enabled).setSaveConsumer(v -> cfg.enabled = v).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_status_hud"), cfg.showStatusHud)
            .setDefaultValue(defaults.showStatusHud).setSaveConsumer(v -> cfg.showStatusHud = v).build());
        general.addEntry(eb.startEnumSelector(Component.translatable("config.espmod.status_hud_corner"), HudCorner.class, cfg.statusHudCorner)
            .setDefaultValue(defaults.statusHudCorner).setSaveConsumer(v -> cfg.statusHudCorner = v).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_target_hud"), cfg.showTargetHud)
            .setDefaultValue(defaults.showTargetHud).setSaveConsumer(v -> cfg.showTargetHud = v).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_radar"), cfg.showRadar)
            .setDefaultValue(defaults.showRadar).setSaveConsumer(v -> cfg.showRadar = v).build());
        general.addEntry(eb.startIntSlider(Component.translatable("config.espmod.radar_size"), cfg.radarSize, 48, 120)
            .setDefaultValue(defaults.radarSize).setSaveConsumer(v -> cfg.radarSize = v).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_pointers"), cfg.showDirectionPointers)
            .setDefaultValue(defaults.showDirectionPointers).setSaveConsumer(v -> cfg.showDirectionPointers = v).build());
        general.addEntry(eb.startDoubleField(Component.translatable("config.espmod.max_distance"), cfg.maxDistance)
            .setDefaultValue(defaults.maxDistance).setMin(8.0).setMax(256.0).setSaveConsumer(v -> cfg.maxDistance = v).build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.distance_fade"), cfg.distanceFade)
            .setDefaultValue(defaults.distanceFade).setSaveConsumer(v -> cfg.distanceFade = v).build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.espmod.category.visuals"));
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.render_boxes"), cfg.renderBoxes)
            .setDefaultValue(defaults.renderBoxes).setSaveConsumer(v -> cfg.renderBoxes = v).build());
        visuals.addEntry(eb.startEnumSelector(Component.translatable("config.espmod.box_style"), BoxStyle.class, cfg.boxStyle)
            .setDefaultValue(defaults.boxStyle).setSaveConsumer(v -> cfg.boxStyle = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.hit_flash"), cfg.hitFlashHighlight)
            .setDefaultValue(defaults.hitFlashHighlight).setSaveConsumer(v -> cfg.hitFlashHighlight = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.render_2d"), cfg.render2DOverlay)
            .setDefaultValue(defaults.render2DOverlay).setSaveConsumer(v -> cfg.render2DOverlay = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.corner_2d"), cfg.corner2DBox)
            .setDefaultValue(defaults.corner2DBox).setSaveConsumer(v -> cfg.corner2DBox = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.render_outlines"), cfg.renderOutlines)
            .setDefaultValue(defaults.renderOutlines).setSaveConsumer(v -> cfg.renderOutlines = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.render_tracers"), cfg.renderTracers)
            .setDefaultValue(defaults.renderTracers).setSaveConsumer(v -> cfg.renderTracers = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.render_nametags"), cfg.renderNametags)
            .setDefaultValue(defaults.renderNametags).setSaveConsumer(v -> cfg.renderNametags = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_health"), cfg.showHealth)
            .setDefaultValue(defaults.showHealth).setSaveConsumer(v -> cfg.showHealth = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_distance"), cfg.showDistance)
            .setDefaultValue(defaults.showDistance).setSaveConsumer(v -> cfg.showDistance = v).build());
        visuals.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_item_count"), cfg.showItemCount)
            .setDefaultValue(defaults.showItemCount).setSaveConsumer(v -> cfg.showItemCount = v).build());
        visuals.addEntry(eb.startFloatField(Component.translatable("config.espmod.line_width"), cfg.lineWidth)
            .setDefaultValue(defaults.lineWidth).setMin(1.0f).setMax(5.0f).setSaveConsumer(v -> cfg.lineWidth = v).build());
        visuals.addEntry(eb.startFloatField(Component.translatable("config.espmod.fill_opacity"), cfg.fillOpacity)
            .setDefaultValue(defaults.fillOpacity).setMin(0.05f).setMax(0.60f).setSaveConsumer(v -> cfg.fillOpacity = v).build());

        ConfigCategory entities = builder.getOrCreateCategory(Component.translatable("config.espmod.category.entities"));
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_players"), cfg.showPlayers)
            .setDefaultValue(defaults.showPlayers).setSaveConsumer(v -> cfg.showPlayers = v).build());
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_hostile"), cfg.showHostileMobs)
            .setDefaultValue(defaults.showHostileMobs).setSaveConsumer(v -> cfg.showHostileMobs = v).build());
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_passive"), cfg.showPassiveMobs)
            .setDefaultValue(defaults.showPassiveMobs).setSaveConsumer(v -> cfg.showPassiveMobs = v).build());
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_villagers_pets"), cfg.showVillagersAndPets)
            .setDefaultValue(defaults.showVillagersAndPets).setSaveConsumer(v -> cfg.showVillagersAndPets = v).build());
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_items"), cfg.showItems)
            .setDefaultValue(defaults.showItems).setSaveConsumer(v -> cfg.showItems = v).build());
        entities.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_vehicles"), cfg.showVehicles)
            .setDefaultValue(defaults.showVehicles).setSaveConsumer(v -> cfg.showVehicles = v).build());

        ConfigCategory storage = builder.getOrCreateCategory(Component.translatable("config.espmod.category.storage"));
        storage.addEntry(eb.startTextDescription(Component.translatable("config.espmod.storage_note")).build());
        storage.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_storage"), cfg.showStorage)
            .setDefaultValue(defaults.showStorage).setSaveConsumer(v -> cfg.showStorage = v).build());
        storage.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_chests"), cfg.showChests)
            .setDefaultValue(defaults.showChests).setSaveConsumer(v -> cfg.showChests = v).build());
        storage.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_ender_chests"), cfg.showEnderChests)
            .setDefaultValue(defaults.showEnderChests).setSaveConsumer(v -> cfg.showEnderChests = v).build());
        storage.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_shulkers"), cfg.showShulkers)
            .setDefaultValue(defaults.showShulkers).setSaveConsumer(v -> cfg.showShulkers = v).build());
        storage.addEntry(eb.startBooleanToggle(Component.translatable("config.espmod.show_spawners"), cfg.showSpawners)
            .setDefaultValue(defaults.showSpawners).setSaveConsumer(v -> cfg.showSpawners = v).build());

        ConfigCategory colors = builder.getOrCreateCategory(Component.translatable("config.espmod.category.colors"));
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.player_color"), cfg.playerColor)
            .setDefaultValue(defaults.playerColor).setSaveConsumer(v -> cfg.playerColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.hostile_color"), cfg.hostileColor)
            .setDefaultValue(defaults.hostileColor).setSaveConsumer(v -> cfg.hostileColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.passive_color"), cfg.passiveColor)
            .setDefaultValue(defaults.passiveColor).setSaveConsumer(v -> cfg.passiveColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.villager_pet_color"), cfg.villagerPetColor)
            .setDefaultValue(defaults.villagerPetColor).setSaveConsumer(v -> cfg.villagerPetColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.item_color"), cfg.itemColor)
            .setDefaultValue(defaults.itemColor).setSaveConsumer(v -> cfg.itemColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.vehicle_color"), cfg.vehicleColor)
            .setDefaultValue(defaults.vehicleColor).setSaveConsumer(v -> cfg.vehicleColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.chest_color"), cfg.chestColor)
            .setDefaultValue(defaults.chestColor).setSaveConsumer(v -> cfg.chestColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.ender_chest_color"), cfg.enderChestColor)
            .setDefaultValue(defaults.enderChestColor).setSaveConsumer(v -> cfg.enderChestColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.shulker_color"), cfg.shulkerColor)
            .setDefaultValue(defaults.shulkerColor).setSaveConsumer(v -> cfg.shulkerColor = v & 0xFFFFFF).build());
        colors.addEntry(eb.startColorField(Component.translatable("config.espmod.spawner_color"), cfg.spawnerColor)
            .setDefaultValue(defaults.spawnerColor).setSaveConsumer(v -> cfg.spawnerColor = v & 0xFFFFFF).build());

        return builder.build();
    }
}
