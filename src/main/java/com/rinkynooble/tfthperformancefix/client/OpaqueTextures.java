package com.rinkynooble.tfthperformancefix.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks the render type for TFTH's mobs, flesh spikes, boils and forge.
 *
 * <p>TFTH draws all of them as translucent (the MCreator default), which makes the game sort
 * every one of their faces by distance, every frame. Their textures are almost all either fully
 * solid or fully clear, and for such a texture the cutout render type gives the same pixels:
 * both discard the clear ones and draw the solid ones. So a texture with no partly transparent
 * pixel is drawn as cutout, which needs no sorting. A texture with any partly transparent pixel
 * (several of TFTH's have a few) stays translucent. The check reads the texture the game
 * actually uses, so a resource pack's replacement is checked too.
 */
public final class OpaqueTextures {
    private static final Logger LOGGER = LogManager.getLogger("TFTH Performance Fix");
    private static final Map<ResourceLocation, Boolean> OPAQUE = new ConcurrentHashMap<>();

    private OpaqueTextures() {
    }

    /** Called when resources reload, since a resource pack may have changed the textures. */
    public static void clear() {
        OPAQUE.clear();
    }

    /** What {@code RenderType.entityTranslucent(texture)} is replaced with. */
    public static RenderType entityRenderType(ResourceLocation texture) {
        return OPAQUE.computeIfAbsent(texture, OpaqueTextures::hasOnlySolidOrClearPixels)
                ? RenderType.entityCutoutNoCull(texture)
                : RenderType.entityTranslucent(texture);
    }

    private static boolean hasOnlySolidOrClearPixels(ResourceLocation texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return false;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getPixelRGBA(x, y) >>> 24;
                    if (alpha != 0 && alpha != 255) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Couldn't read {}, so it stays translucent.", texture, e);
            return false;
        }
    }
}
