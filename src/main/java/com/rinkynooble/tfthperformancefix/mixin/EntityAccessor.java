package com.rinkynooble.tfthperformancefix.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public interface EntityAccessor {
    /** Forge's persistent data, without creating an empty tag the way {@code getPersistentData()} does. */
    @Nullable
    @Accessor(value = "persistentData", remap = false)
    CompoundTag tfthperformancefix$getPersistentDataIfPresent();
}
