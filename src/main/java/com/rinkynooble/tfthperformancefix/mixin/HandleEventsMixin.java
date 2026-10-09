package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.FEvents.HandleEvents;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = HandleEvents.class, remap = false)
public abstract class HandleEventsMixin {
    /**
     * Brutes share the damage of flesh mobs near them: the mob takes 70% and the brutes split
     * 30%. TFTH took those shares of the damage from before the flesh stage reduction, so a brute
     * nearby threw the reduction away, and above 30% reduction made flesh mobs take more damage.
     * The shares now come from the reduced damage.
     */
    @ModifyVariable(method = "onLivingHurt", name = "amount", at = @At(value = "INVOKE", ordinal = 0, remap = true,
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static float tfthperformancefix$shareReducedDamage(float amount, LivingHurtEvent event) {
        return event.getAmount();
    }
}
