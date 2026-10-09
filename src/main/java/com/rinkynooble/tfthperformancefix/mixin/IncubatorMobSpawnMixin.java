package com.rinkynooble.tfthperformancefix.mixin;

import com.rinkynooble.tfthperformancefix.IncubatorSpawns;
import net.mcreator.thefleshthathates.IncubatorMobSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Incubators spawning mobs. Each mob spawned read every block in a 151x41x151 box (starting
 * incubator) or 251x41x251 box (Incubator One), and each count walked every entity in the level.
 */
@Mixin(value = IncubatorMobSpawn.class, remap = false)
public abstract class IncubatorMobSpawnMixin {
    @Inject(method = "getRandomFleshBlockPosition", at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$fastSpawnPosition(ServerLevel world, LivingEntity incubator, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(IncubatorSpawns.randomFleshBlockPosition(world, incubator));
    }

    @Inject(method = "countSpawnedEntitiesByIncubator(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;)I",
            at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$countByIncubator(ServerLevel world, UUID incubatorId, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(IncubatorSpawns.countSpawned(world, null, incubatorId, false));
    }

    @Inject(method = "countSpawnedEntitiesByIncubator(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;Lnet/minecraft/world/entity/EntityType;)I",
            at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$countTypeByIncubator(ServerLevel world, UUID incubatorId, EntityType<?> specificType,
                                                                CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(IncubatorSpawns.countSpawned(world, specificType, incubatorId, false));
    }

    /** Counts mobs of a type spawned by any incubator (TFTH ignores the incubator type it's given). */
    @Inject(method = "countSpawnedEntitiesByIncubator(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntityType;)I",
            at = @At("HEAD"), cancellable = true)
    private static void tfthperformancefix$countTypeByAnyIncubator(ServerLevel world, EntityType<?> incubatorType, EntityType<?> specificType,
                                                                   CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(IncubatorSpawns.countSpawned(world, specificType, null, true));
    }
}
