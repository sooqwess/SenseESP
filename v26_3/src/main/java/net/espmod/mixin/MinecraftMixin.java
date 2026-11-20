package net.espmod.mixin;

import net.espmod.config.EspConfig;
import net.espmod.util.EntityClassifier;
import net.espmod.util.EspPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void espmod$shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || !cfg.renderOutlines) {
            return;
        }
        Minecraft self = (Minecraft) (Object) this;
        if (self.player == null || self.level == null || !EspPolicy.canBypassWalls(self)) {
            return;
        }
        if (EntityClassifier.shouldTrackEntity(entity, self.player, cfg)
            && self.player.distanceToSqr(entity) <= cfg.maxDistance * cfg.maxDistance) {
            cir.setReturnValue(true);
        }
    }
}
