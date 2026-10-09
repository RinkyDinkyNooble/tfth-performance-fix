package com.rinkynooble.tfthperformancefix.client;

import com.rinkynooble.tfthperformancefix.TfthPerformanceFix;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TfthPerformanceFix.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    private ClientEvents() {
    }

    /** Recorded spikes and texture checks are dropped when resources reload, since models or textures may change. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            OpaqueTextures.clear();
            StaticSpikeRenderer.clear();
        });
    }
}
