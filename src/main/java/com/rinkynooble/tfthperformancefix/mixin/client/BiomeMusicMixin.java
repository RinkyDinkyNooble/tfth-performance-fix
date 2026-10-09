package com.rinkynooble.tfthperformancefix.mixin.client;

import com.rinkynooble.tfthperformancefix.SectionScan;
import com.rinkynooble.tfthperformancefix.TfthBlocks;
import net.mcreator.thefleshthathates.FEvents.BiomeMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFTH's flesh music. Every 50 ticks it reads the 61x61x61 cube around the player twice: once
 * to count TFTH blocks, once to find the nearest.
 */
@Mixin(value = BiomeMusic.class, remap = false)
public abstract class BiomeMusicMixin {
    /**
     * Only the local player's ticks run the check. Other players near flesh used to set the
     * "music playing" flag and pause your music, with no flesh music starting for you.
     */
    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$onlyLocalPlayer(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
        if (event.player != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }

    @Inject(method = "countSpecificBlocksAroundPlayer", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$fastCount(Player player, int radius, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SectionScan.countInCube(player.level(), player.blockPosition(), radius, TfthBlocks.MOD_BLOCK).count);
    }

    @Inject(method = "getClosestBlockDistance", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$fastNearest(Player player, CallbackInfoReturnable<Double> cir) {
        double nearestDistSqr = SectionScan.countInCube(player.level(), player.blockPosition(), 30, TfthBlocks.MOD_BLOCK).nearestDistSqr;
        cir.setReturnValue(Math.sqrt(nearestDistSqr));
    }
}
