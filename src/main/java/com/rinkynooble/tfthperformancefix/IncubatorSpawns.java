package com.rinkynooble.tfthperformancefix;

import com.rinkynooble.tfthperformancefix.mixin.EntityAccessor;
import net.mcreator.thefleshthathates.init.TheFleshThatHatesModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Faster versions of the searches TFTH's incubators run when they spawn mobs. */
public final class IncubatorSpawns {
    private static final String SPAWNED_BY = "SpawnedByIncubator";
    private static final String SPAWNED_BY_TYPE = "SpawnedByIncubatorType";

    private IncubatorSpawns() {
    }

    /**
     * Picks a random flesh block near the incubator with air above it and on the four sides of
     * that air block, from the same box TFTH searches (75 blocks across for the starting
     * incubator, 125 for Incubator One, 20 up and down). Candidates are put in the order TFTH's
     * loops find them, so the same random number picks the same block.
     *
     * <p>Unlike TFTH, only loaded chunks are searched. TFTH's search loaded (or generated) every
     * chunk in the box, and could put mobs in chunks that unloaded straight after.
     */
    @Nullable
    public static BlockPos randomFleshBlockPosition(ServerLevel level, LivingEntity incubator) {
        int radius = incubator.getType() == TheFleshThatHatesModEntities.PLAQUEINCUBATORSTART.get() ? 75 : 125;
        int verticalRadius = 20;
        BlockPos center = incubator.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        SectionScan.forEachMatch(level,
                center.getX() - radius, center.getY() - verticalRadius, center.getZ() - radius,
                center.getX() + radius, center.getY() + verticalRadius, center.getZ() + radius,
                TfthBlocks.FLESH_BLOCK, (x, y, z, state) -> {
                    above.set(x, y + 1, z);
                    if (isLoadedAir(level, above)
                            && isLoadedAir(level, above.north())
                            && isLoadedAir(level, above.south())
                            && isLoadedAir(level, above.east())
                            && isLoadedAir(level, above.west())) {
                        candidates.add(new BlockPos(x, y, z));
                    }
                });
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(TfthBlocks.LOOP_ORDER);
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private static boolean isLoadedAir(Level level, BlockPos pos) {
        BlockState state = SectionScan.getIfLoaded(level, pos);
        return state != null && state.isAir();
    }

    /**
     * Counts the entities TFTH tagged as spawned by an incubator. Each filter is skipped when
     * {@code null} or {@code false}: the entity's type, the incubator's UUID, and whether the
     * incubator type tag must be present too.
     *
     * <p>TFTH read every entity's persistent data through {@code getPersistentData()}, which gives
     * every entity in the level an empty tag, and parsed the UUID text of each tagged one.
     */
    public static int countSpawned(ServerLevel level, @Nullable EntityType<?> type, @Nullable UUID incubatorId, boolean needsTypeTag) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (type != null && entity.getType() != type) {
                continue;
            }
            CompoundTag data = ((EntityAccessor) entity).tfthperformancefix$getPersistentDataIfPresent();
            if (data == null || !data.contains(SPAWNED_BY)) {
                continue;
            }
            if (needsTypeTag && !data.contains(SPAWNED_BY_TYPE)) {
                continue;
            }
            if (incubatorId != null && !isSameUuid(data.getString(SPAWNED_BY), incubatorId)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static boolean isSameUuid(String text, UUID id) {
        try {
            return UUID.fromString(text).equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
