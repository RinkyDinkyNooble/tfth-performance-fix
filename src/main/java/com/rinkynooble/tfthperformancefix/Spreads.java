package com.rinkynooble.tfthperformancefix;

import net.mcreator.thefleshthathates.FleshBlockSpread;
import net.mcreator.thefleshthathates.TickMethods;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Upkeep of TFTH's list of spreads ({@code TickMethods.fleshBlockSpreads}). */
public final class Spreads {
    private Spreads() {
    }

    /** Puts each saved spread back in the dimension it was saved in. TFTH put them all in the Overworld. */
    public static void restoreDimensions(MinecraftServer server) {
        for (FleshBlockSpread spread : TickMethods.fleshBlockSpreads) {
            String dimension = ((SpreadAccess) spread).tfthperformancefix$getSavedDimension();
            ResourceLocation id = dimension == null ? null : ResourceLocation.tryParse(dimension);
            ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null) {
                spread.setLevel(level);
            }
        }
    }

    /** Removes the spreads whose origin flesh block is gone, as far as loaded chunks show. */
    public static void removeEnded() {
        TickMethods.fleshBlockSpreads.removeIf(Spreads::hasEnded);
    }

    private static boolean hasEnded(FleshBlockSpread spread) {
        ServerLevel level = ((SpreadAccess) spread).tfthperformancefix$getLevel();
        if (level == null) {
            return false;
        }
        BlockState origin = SectionScan.getIfLoaded(level, spread.origin);
        return origin != null && !TfthBlocks.FLESH_BLOCK.test(origin);
    }
}
