package net.espmod.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.espmod.render.EspWorldRenderer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Inject(
        method = "renderMain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void espmod$afterMainRender(
        FrameGraphBuilder frameGraphBuilder,
        Frustum frustum,
        Matrix4f posMatrix,
        GpuBufferSlice fogBuffer,
        boolean renderBlockOutline,
        WorldRenderState state,
        RenderTickCounter tickCounter,
        Profiler profiler,
        CallbackInfo ci
    ) {
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        MatrixStack matrices = new MatrixStack();
        EspWorldRenderer.renderDirect(matrices, immediate, state.cameraRenderState.pos, tickCounter.getTickProgress(false));
    }
}
