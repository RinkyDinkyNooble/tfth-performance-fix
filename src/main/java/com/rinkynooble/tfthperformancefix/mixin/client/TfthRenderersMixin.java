package com.rinkynooble.tfthperformancefix.mixin.client;

import com.rinkynooble.tfthperformancefix.client.OpaqueTextures;
import net.mcreator.thefleshthathates.block.renderer.FleshBoilPhaseTwoTileRenderer;
import net.mcreator.thefleshthathates.block.renderer.FleshBoilTileRenderer;
import net.mcreator.thefleshthathates.block.renderer.FleshForgeTileRenderer;
import net.mcreator.thefleshthathates.block.renderer.FleshSpikesTileRenderer;
import net.mcreator.thefleshthathates.client.renderer.BruteplaquecreatureoneRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshBoomerRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshCocoonRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshCommunityRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshCowRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshDogRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshHowlerRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshHumanHeadRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshHumanRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshHunterThreeRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshHunterTwoRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshJusticeRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshPieceRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshPigRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshPillagerRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshServantRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshSheepRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshSufferRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshVillagerRenderer;
import net.mcreator.thefleshthathates.client.renderer.FleshVindicatorRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaquecontaminatorRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaquecreaturebaseoneRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaquecreatureoneRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaquecreaturetwoRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaqueincubatoroneRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaqueincubatorstartRenderer;
import net.mcreator.thefleshthathates.client.renderer.PlaquethreelegcreatureRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TFTH's GeckoLib renderers for its mobs, flesh spikes, boils and forge. Each one's
 * {@code getRenderType} returns {@code RenderType.entityTranslucent(texture)}; that call goes
 * through {@link OpaqueTextures}, which keeps it for textures with partly transparent pixels and
 * uses cutout for the rest. The item renderers (blocks held or in inventory) are left alone.
 */
@Mixin(value = {
        BruteplaquecreatureoneRenderer.class, FleshBoomerRenderer.class, FleshCocoonRenderer.class, FleshCommunityRenderer.class,
        FleshCowRenderer.class, FleshDogRenderer.class, FleshHowlerRenderer.class, FleshHumanHeadRenderer.class, FleshHumanRenderer.class,
        FleshHunterThreeRenderer.class, FleshHunterTwoRenderer.class, FleshJusticeRenderer.class, FleshPieceRenderer.class,
        FleshPigRenderer.class, FleshPillagerRenderer.class, FleshServantRenderer.class, FleshSheepRenderer.class,
        FleshSufferRenderer.class, FleshVillagerRenderer.class, FleshVindicatorRenderer.class, PlaquecontaminatorRenderer.class,
        PlaquecreaturebaseoneRenderer.class, PlaquecreatureoneRenderer.class, PlaquecreaturetwoRenderer.class,
        PlaqueincubatoroneRenderer.class, PlaqueincubatorstartRenderer.class, PlaquethreelegcreatureRenderer.class,
        FleshBoilTileRenderer.class, FleshBoilPhaseTwoTileRenderer.class, FleshForgeTileRenderer.class, FleshSpikesTileRenderer.class
}, remap = false)
public abstract class TfthRenderersMixin {
    @Redirect(method = "getRenderType", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType tfthperformancefix$cutoutWhenOpaque(ResourceLocation texture) {
        return OpaqueTextures.entityRenderType(texture);
    }
}
