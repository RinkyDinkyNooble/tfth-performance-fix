package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.init.TheFleshThatHatesModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TheFleshThatHatesModEntities.class, remap = false)
public abstract class TheFleshThatHatesModEntitiesMixin {
    /**
     * TFTH registers the Flesh Hunter III with a size of 0 by 0 (the only one) and sets its real
     * size, 1.5 by 3.3, in the entity's {@code getDimensions}. Code that compares the two divides
     * by zero: Presence Footsteps scales its search under a mob's feet by that ratio and searched
     * 81 by 5 by 81 blocks for each Hunter III step, which took over a third of the client's time
     * near a group of them. Spawn checks also tested a 0 by 0 box. It's now registered at its
     * real size.
     */
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> tfthperformancefix$fleshHunterThreeSize(EntityType.Builder<T> builder, float width, float height) {
        if (width == 0.0F && height == 0.0F) {
            return builder.sized(1.5F, 3.3F);
        }
        return builder.sized(width, height);
    }
}
