package com.rinkynooble.tfthperformancefix.mixin.client;

import com.rinkynooble.tfthperformancefix.SectionScan;
import com.rinkynooble.tfthperformancefix.TfthBlocks;
import net.mcreator.thefleshthathates.FleshRenderBiome;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFTH's flesh fog. Every 40 ticks it counts TFTH blocks in the 61x61x61 cube around the player
 * and finds the nearest one, by reading all 226,981 positions.
 */
@Mixin(value = FleshRenderBiome.class, remap = false)
public abstract class FleshRenderBiomeMixin {
    @Shadow
    private static boolean isNearFleshBlocks;
    @Shadow
    private static float fleshBlockDensity;
    @Shadow
    private static double nearestFleshBlockDistance;

    /**
     * Only the local player's ticks run the check. Other players' ticks advanced the same counter,
     * and the scan then ran around whichever player hit it, so another player's surroundings
     * could set your fog.
     */
    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$onlyLocalPlayer(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
        if (event.player != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }

    /** The same count and nearest distance, skipping chunk sections that hold no TFTH block. */
    @Inject(method = "checkAndUpdateFog", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$fastFogScan(Player player, CallbackInfo ci) {
        SectionScan.CountResult result = SectionScan.countInCube(player.level(), player.blockPosition(), 30, TfthBlocks.MOD_BLOCK);
        nearestFleshBlockDistance = result.nearestDistSqr;
        isNearFleshBlocks = result.count > 2 && nearestFleshBlockDistance <= 72.0;
        fleshBlockDensity = result.count > 30 ? 1.0F : (float) result.count / 30.0F;
        ci.cancel();
    }
}
