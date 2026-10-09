package com.rinkynooble.tfthperformancefix.mixin;

import net.mcreator.thefleshthathates.potion.GOHMobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GOHMobEffect.class, remap = false)
public abstract class GOHMobEffectMixin {
    /**
     * Fixes the "Modifier is already applied on this attribute!" crash. GOH added its speed
     * modifier without checking for one left from before, so a mob that still had it crashed the
     * game when GOH was applied again. Vanilla effects remove their modifier before adding it too.
     */
    @Redirect(method = {"addAttributeModifiers", "m_6385_"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"))
    private void tfthperformancefix$replaceSpeedModifier(AttributeInstance attribute, AttributeModifier modifier) {
        attribute.removeModifier(modifier.getId());
        attribute.addTransientModifier(modifier);
    }
}
