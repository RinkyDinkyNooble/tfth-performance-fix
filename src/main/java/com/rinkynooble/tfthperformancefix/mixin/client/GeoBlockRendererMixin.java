package com.rinkynooble.tfthperformancefix.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rinkynooble.tfthperformancefix.client.StaticSpikeRenderer;
import net.mcreator.thefleshthathates.block.entity.FleshSpikesTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** GeckoLib's block entity renderer, which TFTH uses for its flesh spikes. Only flesh spikes are affected. */
@Mixin(value = GeoBlockRenderer.class, remap = false)
public abstract class GeoBlockRendererMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = {"render", "m_6922_"}, at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$replayIdleSpikes(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                                     int packedLight, int packedOverlay, CallbackInfo ci) {
        if (blockEntity instanceof FleshSpikesTileEntity spike
                && StaticSpikeRenderer.render((GeoBlockRenderer<FleshSpikesTileEntity>) (Object) this, spike, partialTick, poseStack, bufferSource,
                packedLight, packedOverlay)) {
            ci.cancel();
        }
    }
}
