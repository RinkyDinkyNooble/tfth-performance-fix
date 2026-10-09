package com.rinkynooble.tfthperformancefix.mixin;

import com.rinkynooble.tfthperformancefix.Spreads;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TFTH's server-wide list of spreads, loaded when the server starts and ticked every server tick. */
@Mixin(targets = "net.mcreator.thefleshthathates.TickMethods$ForgeBusEvents", remap = false)
public abstract class TickMethodsMixin {
    /**
     * TFTH saves no dimension with a spread, and puts every saved spread in the Overworld when
     * the server starts. Spreads elsewhere stopped after each restart, and read their origin block
     * in the Overworld every tick, loading that Overworld chunk.
     */
    @Inject(method = "serverLoad", at = @At("RETURN"))
    private static void tfthperformancefix$restoreDimensions(ServerStartingEvent event, CallbackInfo ci) {
        Spreads.restoreDimensions(event.getServer());
    }

    /**
     * Removes spreads whose origin flesh block is gone. Such a spread does nothing but read that
     * block every tick, and TFTH kept every one forever, in memory and in the save.
     */
    @Inject(method = "onTick", at = @At("HEAD"))
    private static void tfthperformancefix$removeEndedSpreads(TickEvent.ServerTickEvent event, CallbackInfo ci) {
        if (event.phase == TickEvent.Phase.END) {
            Spreads.removeEnded();
        }
    }
}
