package com.rinkynooble.tfthperformancefix;

import net.mcreator.thefleshthathates.entity.PlaqueincubatoroneEntity;
import net.mcreator.thefleshthathates.entity.PlaqueincubatorstartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Counts incubators joining and leaving levels. A cached "is an incubator nearby" answer is
 * reused only while this count hasn't changed. Incubators can't move, so a join or leave is the
 * only way the answer can change within a tick.
 */
@Mod.EventBusSubscriber(modid = TfthPerformanceFix.MODID)
public final class IncubatorTracker {
    private static int generation;

    private IncubatorTracker() {
    }

    public static int generation() {
        return generation;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (isIncubator(event.getEntity())) {
            generation++;
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (isIncubator(event.getEntity())) {
            generation++;
        }
    }

    private static boolean isIncubator(Entity entity) {
        return entity instanceof PlaqueincubatoroneEntity || entity instanceof PlaqueincubatorstartEntity;
    }
}
