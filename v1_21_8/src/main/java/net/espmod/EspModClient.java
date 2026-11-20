package net.espmod;

import net.espmod.config.EspConfig;
import net.espmod.config.EspConfigScreen;
import net.espmod.render.EspHudRenderer;
import net.espmod.render.EspWorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EspModClient implements ClientModInitializer {
    public static final String MOD_ID = "espmod";

    private static KeyBinding toggleKey;
    private static KeyBinding configKey;
    private static KeyBinding hudKey;

    @Override
    public void onInitializeClient() {
        EspConfig.load();

        
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.espmod.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.espmod"
        ));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.espmod.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.espmod"
        ));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.espmod.hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.espmod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                EspConfig cfg = EspConfig.get();
                cfg.enabled = !cfg.enabled;
                EspConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.translatable(cfg.enabled ? "msg.espmod.enabled" : "msg.espmod.disabled")
                            .formatted(cfg.enabled ? Formatting.AQUA : Formatting.GRAY),
                        true
                    );
                }
            }
            while (hudKey.wasPressed()) {
                EspConfig cfg = EspConfig.get();
                cfg.showStatusHud = !cfg.showStatusHud;
                EspConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.translatable(cfg.showStatusHud ? "msg.espmod.hud_on" : "msg.espmod.hud_off")
                            .formatted(cfg.showStatusHud ? Formatting.AQUA : Formatting.GRAY),
                        true
                    );
                }
            }
            while (configKey.wasPressed()) {
                client.setScreen(EspConfigScreen.create(client.currentScreen));
            }
        });

        WorldRenderEvents.LAST.register(EspWorldRenderer::render);
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"), EspHudRenderer::render);
    }
}
