package com.rinkynooble.tfthperformancefix.mixin.client;

import com.rinkynooble.tfthperformancefix.client.ZeroAreaFaces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib's model lookup, which every TFTH model uses. When a TFTH model switches to a baked
 * model it hasn't drawn before, the faces with no area are removed from it (once per model).
 * Other mods' models are left alone.
 */
@Mixin(value = GeoModel.class, remap = false)
public abstract class GeoModelMixin {
    @ModifyArg(method = "getBakedModel(Lnet/minecraft/resources/ResourceLocation;)Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;",
            at = @At(value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/core/animation/AnimationProcessor;setActiveModel(Lsoftware/bernie/geckolib/core/animatable/model/CoreBakedGeoModel;)V"))
    private CoreBakedGeoModel tfthperformancefix$removeZeroAreaFaces(CoreBakedGeoModel model) {
        if (model instanceof BakedGeoModel baked && ((Object) this).getClass().getName().startsWith("net.mcreator.thefleshthathates.")) {
            ZeroAreaFaces.removeFrom(baked);
        }
        return model;
    }
}
