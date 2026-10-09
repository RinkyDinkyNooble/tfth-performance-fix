package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.EntitySavedData;
import net.mcreator.thefleshthathates.entity.PlaqueincubatorstartEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** The starting incubator. */
@Mixin(value = PlaqueincubatorstartEntity.class, remap = false)
public abstract class PlaqueincubatorstartEntityMixin {
    @Unique
    private UUID tfthperformancefix$constructionUuid;

    @Unique
    private Entity tfthperformancefix$self() {
        return (Entity) (Object) this;
    }

    /**
     * The starting incubator force-loads its chunk when it spawns and releases it when it dies,
     * but evolving into Incubator One removes it without dying, so the chunk stayed force-loaded
     * (and kept ticking) forever. It's released here the same way dying does.
     */
    @Inject(method = "transformAfter10Minutes", at = @At("RETURN"))
    private void tfthperformancefix$releaseChunkAfterEvolving(CallbackInfo ci) {
        Entity self = this.tfthperformancefix$self();
        if (self.isRemoved() && self.getRemovalReason() == Entity.RemovalReason.DISCARDED
                && self.level() instanceof ServerLevel serverLevel) {
            ChunkPos chunk = new ChunkPos(self.blockPosition());
            serverLevel.setChunkForced(chunk.x, chunk.z, false);
        }
    }

    /*
     * TFTH lists every starting incubator's UUID in the Overworld's "TheFleshThatHates_Entities"
     * data. Nothing reads the list, but it only ever grows: the constructor also runs when an
     * incubator is loaded from disk, before its saved UUID replaces the random one it was given,
     * and dying removes the UUID from the incubator's own dimension's data instead of the
     * Overworld's. The two changes below remove those entries.
     */

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void tfthperformancefix$rememberConstructionUuid(EntityType<?> type, Level world, CallbackInfo ci) {
        if (!world.isClientSide) {
            this.tfthperformancefix$constructionUuid = this.tfthperformancefix$self().getUUID();
        }
    }

    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("HEAD"))
    private void tfthperformancefix$forgetConstructionUuid(CompoundTag compound, CallbackInfo ci) {
        Entity self = this.tfthperformancefix$self();
        UUID constructionUuid = this.tfthperformancefix$constructionUuid;
        this.tfthperformancefix$constructionUuid = null;
        if (constructionUuid != null && !constructionUuid.equals(self.getUUID()) && self.level() instanceof ServerLevel serverLevel) {
            EntitySavedData.get(serverLevel.getServer().overworld().getDataStorage()).removeEntity(constructionUuid);
        }
    }

    @Inject(method = {"die", "m_6667_"}, at = @At("RETURN"))
    private void tfthperformancefix$forgetUuidOnDeath(DamageSource cause, CallbackInfo ci) {
        Entity self = this.tfthperformancefix$self();
        if (self.level() instanceof ServerLevel serverLevel) {
            EntitySavedData.get(serverLevel.getServer().overworld().getDataStorage()).removeEntity(self.getUUID());
        }
    }
}
