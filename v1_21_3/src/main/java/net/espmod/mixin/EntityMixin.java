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

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void espmod$getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        EspConfig cfg = EspConfig.get();
        if (!cfg.enabled || !cfg.renderOutlines) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Entity self = (Entity) (Object) this;
        if (client.player == null || client.world == null || !EspPolicy.canBypassWalls(client)) {
            return;
        }
        if (EntityClassifier.shouldTrackEntity(self, client.player, cfg)
            && client.player.squaredDistanceTo(self) <= cfg.maxDistance * cfg.maxDistance) {
            cir.setReturnValue(EntityClassifier.getEntityColor(self, cfg));
        }
    }
}
