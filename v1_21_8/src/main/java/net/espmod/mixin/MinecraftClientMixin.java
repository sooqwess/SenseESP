package net.espmod.mixin;

import net.espmod.config.EspConfig;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void espmod$hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || !cfg.renderOutlines) {
            return;
        }
        MinecraftClient self = (MinecraftClient) (Object) this;
        if (self.player == null || self.world == null || !EspPolicy.canBypassWalls(self)) {
            return;
        }
        if (EntityClassifier.shouldTrackEntity(entity, self.player, cfg)
            && self.player.squaredDistanceTo(entity) <= cfg.maxDistance * cfg.maxDistance) {
            cir.setReturnValue(true);
        }
    }
}
