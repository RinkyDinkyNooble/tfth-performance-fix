package com.rinkynooble.tfthperformancefix.mixin;

import com.rinkynooble.tfthperformancefix.SectionScan;
import com.rinkynooble.tfthperformancefix.TfthBlocks;
import net.mcreator.thefleshthathates.AI.DangerAssistGoal;
import net.mcreator.thefleshthathates.entity.FleshMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * An incubator calling for help puts out fire and lava within 30 blocks, which read all
 * 226,981 positions in the cube.
 */
@Mixin(value = DangerAssistGoal.class, remap = false)
public abstract class DangerAssistGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    /**
     * Same blocks removed, in the same order, then the same burning flesh mobs put out. The cube
     * always lies in loaded chunks, since the incubator's own chunk is ticking.
     */
    @Inject(method = "extinguishFiresAndEntities", at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$fastExtinguish(ServerLevel serverLevel, CallbackInfo ci) {
        BlockPos center = this.mob.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        SectionScan.forEachMatch(serverLevel,
                center.getX() - 30, center.getY() - 30, center.getZ() - 30,
                center.getX() + 30, center.getY() + 30, center.getZ() + 30,
                state -> state.is(Blocks.FIRE) || state.is(Blocks.LAVA),
                (x, y, z, state) -> found.add(new BlockPos(x, y, z)));
        found.sort(TfthBlocks.LOOP_ORDER);
        for (BlockPos pos : found) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, this.mob.getBoundingBox().inflate(30.0),
                e -> FleshMobs.ENTITY_TYPES.contains(e.getType()) && e.isOnFire())) {
            entity.clearFire();
        }
        ci.cancel();
    }
}
