package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.FConfig.TFTHConfiguration;
import net.mcreator.thefleshthathates.FleshWorldData;
import net.mcreator.thefleshthathates.IncubatorSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.PrintStream;

/** Starting incubators appearing near players, tried every "incubatorSleepTime" ticks for each player. */
@Mixin(value = IncubatorSpawn.class, remap = false)
public abstract class IncubatorSpawnMixin {
    /** Tries per spawn attempt to find a spot whose chunk is loaded. */
    @Unique
    private static final int TRIES = 10;

    /**
     * Picks the spot the way TFTH does (a random offset from the player, on the surface), but only
     * in a loaded chunk, trying a few spots before giving up until the next attempt. TFTH read the
     * surface height of the spot it picked, which loaded or generated that chunk on the spot: one
     * such read took 348 ms of a single tick in the ABZA profile.
     */
    @Inject(method = "processFirstTickEvent", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$spawnInLoadedChunks(ServerLevel world, Player player, FleshWorldData data, CallbackInfo ci) {
        ci.cancel();
        if (!TFTHConfiguration.isIncubatorSpawnEnabled()
                || TFTHConfiguration.INCUBATOR_DIMENSION_BLACKLIST.get().contains(world.dimension().location().toString())
                || data.getPlaqueIncubatorStartDeathCounter() > 0) {
            return;
        }
        int xRange = TFTHConfiguration.getIncubatorSpawnXRandomRange();
        int xOffset = TFTHConfiguration.getIncubatorSpawnXOffset();
        int zRange = TFTHConfiguration.getIncubatorSpawnZRandomRange();
        int zOffset = TFTHConfiguration.getIncubatorSpawnZOffset();
        for (int i = 0; i < TRIES; i++) {
            int x = (int) (player.getX() + world.random.nextInt(xRange) - xOffset);
            int z = (int) (player.getZ() + world.random.nextInt(zRange) - zOffset);
            if (world.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
                continue;
            }
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.getFluidState(pos).isEmpty() || !world.getFluidState(pos.below()).isEmpty()) {
                return;
            }
            if (!IncubatorSpawnAccessor.tfthperformancefix$hasNearbyIncubators(world, x, y, z, TFTHConfiguration.incubatormaxtonextspawn())) {
                IncubatorSpawnAccessor.tfthperformancefix$spawnIncubatorStart(world, x, y, z, data);
            }
            return;
        }
    }

    /** Drops the "[DEBUG] Attempting to spawn at: ..." line TFTH prints on every incubator spawn attempt. */
    @Redirect(method = "spawnIncubatorStart", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"))
    private static void tfthperformancefix$skipDebugPrint(PrintStream stream, String line) {
    }
}
