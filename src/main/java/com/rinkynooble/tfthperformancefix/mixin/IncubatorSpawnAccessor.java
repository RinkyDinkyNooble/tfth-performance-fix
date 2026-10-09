package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.FleshWorldData;
import net.mcreator.thefleshthathates.IncubatorSpawn;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = IncubatorSpawn.class, remap = false)
public interface IncubatorSpawnAccessor {
    @Invoker("hasNearbyIncubators")
    static boolean tfthperformancefix$hasNearbyIncubators(ServerLevel world, double x, double y, double z, double radius) {
        throw new AssertionError();
    }

    @Invoker("spawnIncubatorStart")
    static void tfthperformancefix$spawnIncubatorStart(ServerLevel world, double x, double y, double z, FleshWorldData data) {
        throw new AssertionError();
    }
}
