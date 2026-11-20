package net.espmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EspConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("espmod.json");

    private static EspConfig instance;

    // General & HUD
    public boolean enabled = true;
    public boolean showStatusHud = true;
    public HudCorner statusHudCorner = HudCorner.TOP_LEFT;
    public double maxDistance = 96.0;
    public boolean distanceFade = true;

    // Visual Styles
    public boolean renderBoxes = true;
    public BoxStyle boxStyle = BoxStyle.FILLED;
    public boolean render2DOverlay = true;
    public boolean corner2DBox = true;
    public boolean renderOutlines = false;
    public boolean renderTracers = false;
    public boolean renderNametags = true;
    public boolean showHealth = true;
    public boolean showDistance = true;
    public boolean showItemCount = true;
    public float lineWidth = 2.0f;
    public float fillOpacity = 0.18f;

    // Entity Categories
    public boolean showPlayers = true;
    public boolean showHostileMobs = true;
    public boolean showPassiveMobs = false;
    public boolean showVillagersAndPets = true;
    public boolean showItems = true;
    public boolean showVehicles = false;

    // Storage & Blocks (Singleplayer / OP Level 2+ only)
    public boolean showStorage = false;
    public boolean showChests = true;
    public boolean showEnderChests = true;
    public boolean showShulkers = true;
    public boolean showSpawners = true;

    // Category Colors (0xRRGGBB)
    public int playerColor = 0x38D6FC;
    public int hostileColor = 0xFF4D5A;
    public int passiveColor = 0x4CE68A;
    public int villagerPetColor = 0xFFD15C;
    public int itemColor = 0xC084FC;
    public int vehicleColor = 0xFB923C;
    public int chestColor = 0xF5B841;
    public int enderChestColor = 0xA855F7;
    public int shulkerColor = 0xEC4899;
    public int spawnerColor = 0xEF4444;

    public static EspConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                EspConfig loaded = GSON.fromJson(reader, EspConfig.class);
                if (loaded != null) {
                    if (loaded.boxStyle == null) loaded.boxStyle = BoxStyle.FILLED;
                    if (loaded.statusHudCorner == null) loaded.statusHudCorner = HudCorner.TOP_LEFT;
                    loaded.lineWidth = Math.max(1.0f, Math.min(5.0f, loaded.lineWidth));
                    loaded.fillOpacity = Math.max(0.05f, Math.min(0.60f, loaded.fillOpacity));
                    loaded.maxDistance = Math.max(8.0, Math.min(256.0, loaded.maxDistance));
                    instance = loaded;
                    return;
                }
            } catch (IOException ignored) {
            }
        }
        instance = new EspConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException ignored) {
        }
    }
}
