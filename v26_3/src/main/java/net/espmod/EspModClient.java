package net.espmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.espmod.config.EspConfig;
import net.espmod.config.EspConfigScreen;
import net.espmod.render.EspHudRenderer;
import net.espmod.render.EspWorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EspModClient implements ClientModInitializer {
    public static final String MOD_ID = "espmod";

    private static KeyMapping toggleKey;
    private static KeyMapping configKey;
    private static KeyMapping hudKey;

    @Override
    public void onInitializeClient() {
        EspConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keys"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.espmod.toggle",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            category
        ));
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.espmod.config",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_O,
            category
        ));
        hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.espmod.hud",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_U,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                EspConfig cfg = EspConfig.get();
                cfg.enabled = !cfg.enabled;
                EspConfig.save();
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        Component.translatable(cfg.enabled ? "msg.espmod.enabled" : "msg.espmod.disabled")
                            .withStyle(cfg.enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                    );
                }
            }
            while (hudKey.consumeClick()) {
                EspConfig cfg = EspConfig.get();
                cfg.showStatusHud = !cfg.showStatusHud;
                EspConfig.save();
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        Component.translatable(cfg.showStatusHud ? "msg.espmod.hud_on" : "msg.espmod.hud_off")
                            .withStyle(cfg.showStatusHud ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                    );
                }
            }
            while (configKey.consumeClick()) {
                client.gui.setScreen(EspConfigScreen.create(client.gui.screen()));
            }
        });

        LevelRenderEvents.BEFORE_GIZMOS.register(EspWorldRenderer::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), EspHudRenderer::render);
    }
}
