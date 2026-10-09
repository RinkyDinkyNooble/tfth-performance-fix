package com.rinkynooble.tfthperformancefix.mixin;

import com.rinkynooble.tfthperformancefix.CompactPosSet;
import com.rinkynooble.tfthperformancefix.IncubatorTracker;
import com.rinkynooble.tfthperformancefix.SpreadAccess;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.mcreator.thefleshthathates.FleshBlockSpread;
import net.mcreator.thefleshthathates.entity.PlaqueincubatoroneEntity;
import net.mcreator.thefleshthathates.entity.PlaqueincubatorstartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * One spreading infection, started by the flesh block under an incubator. It walks outward from
 * that block through every position (air and underground included), and converts the ones that
 * qualify while an incubator is nearby.
 *
 * <p>Keys this adds to a spread's saved data start with {@code tfthperformancefix:}. TFTH ignores
 * keys it doesn't know, so a world still loads in TFTH without this mod.
 */
@Mixin(value = FleshBlockSpread.class, remap = false)
public abstract class FleshBlockSpreadMixin implements SpreadAccess {
    @Unique
    private static final String DIMENSION_KEY = "tfthperformancefix:dimension";
    @Unique
    private static final String VISITED_KEY = "tfthperformancefix:visited";
    @Unique
    private static final Pattern BLOCK_POS_TEXT = Pattern.compile("BlockPos\\{x=(-?\\d+), y=(-?\\d+), z=(-?\\d+)}");

    @Shadow
    protected ServerLevel level;
    @Shadow
    public BlockPos origin;
    @Shadow
    public BlockPos currentPosition;
    @Shadow
    public Set<Long> visitedPositions;
    @Shadow
    int CHECK_RADIUS;
    @Shadow
    int MAX_DISTANCE;
    @Shadow
    LinkedList<Map.Entry<BlockPos, Double>> queue;

    @Shadow
    public abstract void setCurrentPoints();

    @Unique
    @Nullable
    private String tfthperformancefix$savedDimension;

    /** Queue entries waiting for their chunks to load, by chunk. */
    @Unique
    private final Long2ObjectOpenHashMap<List<Map.Entry<BlockPos, Double>>> tfthperformancefix$waiting = new Long2ObjectOpenHashMap<>();

    /** Whether a chunk and its eight neighbours are loaded, this tick: 1 yes, -1 no. */
    @Unique
    private final Long2ByteOpenHashMap tfthperformancefix$loadedCache = new Long2ByteOpenHashMap();
    @Unique
    private long tfthperformancefix$loadedCacheTime = Long.MIN_VALUE;

    @Unique
    private long tfthperformancefix$cacheTime = Long.MIN_VALUE;
    @Unique
    private int tfthperformancefix$cacheGeneration;
    @Unique
    private int tfthperformancefix$cacheRadius;
    @Unique
    private BlockPos tfthperformancefix$cachePos;
    @Unique
    private boolean tfthperformancefix$incubatorOneNearby;
    @Unique
    private boolean tfthperformancefix$startingIncubatorNearby;

    @Override
    @Nullable
    public String tfthperformancefix$getSavedDimension() {
        return this.tfthperformancefix$savedDimension;
    }

    @Override
    @Nullable
    public ServerLevel tfthperformancefix$getLevel() {
        return this.level;
    }

    /**
     * Every position the spread has looked at, which only grows (by about half a million an hour
     * while it runs). One bit per position instead of a {@code HashSet<Long>} entry.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void tfthperformancefix$compactVisitedSet(ServerLevel level, BlockPos origin, CallbackInfo ci) {
        this.visitedPositions = new CompactPosSet();
    }

    /**
     * Pauses the spread while its origin block's chunk isn't loaded, or while no incubator is
     * near it. TFTH read the origin block every tick, which kept that chunk loaded for good; and
     * with no incubator nearby it kept walking outward without converting anything, using up the
     * positions it would convert once an incubator came back.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$pauseWhenIdle(CallbackInfo ci) {
        if (this.level == null || this.level.getChunkSource().getChunkNow(this.origin.getX() >> 4, this.origin.getZ() >> 4) == null) {
            ci.cancel();
            return;
        }
        // The incubator check's range grows with the infection points, which the paused tick would have updated.
        this.setCurrentPoints();
        this.tfthperformancefix$refreshIncubatorCache();
        if (!this.tfthperformancefix$incubatorOneNearby && !this.tfthperformancefix$startingIncubatorNearby) {
            ci.cancel();
            return;
        }
        if (!this.tfthperformancefix$waiting.isEmpty() && this.level.getGameTime() % 20 == 0) {
            this.tfthperformancefix$resumeLoaded();
        }
    }

    /*
     * Waiting at unloaded chunks. The spread reads and changes blocks around each position it
     * handles; in TFTH that loaded (or generated) any chunk its edge reached, on the main thread.
     * Positions whose surrounding chunks aren't all loaded now wait, and go back in the queue once
     * they are.
     */

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;isEmpty()Z"))
    private boolean tfthperformancefix$setAsideUnloadedHead(LinkedList<Map.Entry<BlockPos, Double>> queue) {
        while (!queue.isEmpty()) {
            Map.Entry<BlockPos, Double> head = queue.peek();
            if (this.tfthperformancefix$canHandle(head.getKey())) {
                return false;
            }
            queue.poll();
            long chunk = ChunkPos.asLong(head.getKey().getX() >> 4, head.getKey().getZ() >> 4);
            this.tfthperformancefix$waiting.computeIfAbsent(chunk, key -> new ArrayList<>()).add(head);
        }
        return true;
    }

    /** The queue only counts as empty when nothing is waiting either. */
    @Redirect(method = "searchTick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;isEmpty()Z"))
    private boolean tfthperformancefix$emptyIncludingWaiting(LinkedList<Map.Entry<BlockPos, Double>> queue) {
        return queue.isEmpty() && this.tfthperformancefix$waiting.isEmpty();
    }

    /** The block the search grows from is picked among positions it can handle now. */
    @Redirect(method = "searchTick", at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;stream()Ljava/util/stream/Stream;"))
    private Stream<Map.Entry<BlockPos, Double>> tfthperformancefix$streamHandleable(LinkedList<Map.Entry<BlockPos, Double>> queue) {
        return queue.stream().filter(entry -> this.tfthperformancefix$canHandle(entry.getKey()));
    }

    /** Positions past the spread's range are only noted, without reading the world, so they never wait. */
    @Unique
    private boolean tfthperformancefix$canHandle(BlockPos pos) {
        if (pos.distSqr(this.origin) > (double) (this.MAX_DISTANCE * this.MAX_DISTANCE)) {
            return true;
        }
        long time = this.level.getGameTime();
        if (time != this.tfthperformancefix$loadedCacheTime) {
            this.tfthperformancefix$loadedCache.clear();
            this.tfthperformancefix$loadedCacheTime = time;
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        long key = ChunkPos.asLong(chunkX, chunkZ);
        byte cached = this.tfthperformancefix$loadedCache.get(key);
        if (cached != 0) {
            return cached > 0;
        }
        boolean loaded = true;
        for (int dx = -1; dx <= 1 && loaded; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (this.level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz) == null) {
                    loaded = false;
                    break;
                }
            }
        }
        this.tfthperformancefix$loadedCache.put(key, loaded ? (byte) 1 : (byte) -1);
        return loaded;
    }

    @Unique
    private void tfthperformancefix$resumeLoaded() {
        ObjectIterator<Long2ObjectMap.Entry<List<Map.Entry<BlockPos, Double>>>> it = this.tfthperformancefix$waiting.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            List<Map.Entry<BlockPos, Double>> entries = it.next().getValue();
            if (this.tfthperformancefix$canHandle(entries.get(0).getKey())) {
                this.queue.addAll(entries);
                it.remove();
            }
        }
    }

    /** Waiting positions are saved as part of TFTH's queue, so nothing is lost if this mod is removed. */
    @Inject(method = "saveStateToNBT", at = @At("HEAD"))
    private void tfthperformancefix$saveWaitingInQueue(CallbackInfoReturnable<CompoundTag> cir) {
        for (List<Map.Entry<BlockPos, Double>> entries : this.tfthperformancefix$waiting.values()) {
            this.queue.addAll(entries);
        }
        this.tfthperformancefix$waiting.clear();
    }

    /*
     * Saving and loading. TFTH saved two maps' keys as BlockPos text ("BlockPos{x=1, y=2, z=3}")
     * but read them back as numbers, so loading threw, the whole spread list failed to load, and
     * every spread was lost at each restart. Keys are now saved as the numbers TFTH reads, and old
     * text keys are read too.
     */

    @Redirect(method = "saveStateToNBT", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;toString()Ljava/lang/String;"))
    private String tfthperformancefix$saveKeyAsNumber(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    @Redirect(method = "loadStateFromNBT", at = @At(value = "INVOKE", target = "Ljava/lang/Long;parseLong(Ljava/lang/String;)J"))
    private long tfthperformancefix$readKey(String key) {
        Matcher text = BLOCK_POS_TEXT.matcher(key);
        if (text.matches()) {
            return BlockPos.asLong(Integer.parseInt(text.group(1)), Integer.parseInt(text.group(2)), Integer.parseInt(text.group(3)));
        }
        return Long.parseLong(key);
    }

    /**
     * The visited positions are saved in this mod's compact form instead of TFTH's list of one
     * tag per position. Without this mod, TFTH loads the spread with no visited positions and
     * walks the area again, without changing blocks it already converted.
     */
    @Redirect(method = "saveStateToNBT", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private Iterator<?> tfthperformancefix$skipVisitedList(Set<?> set) {
        return set == this.visitedPositions && set instanceof CompactPosSet ? Collections.emptyIterator() : set.iterator();
    }

    @Inject(method = "saveStateToNBT", at = @At("RETURN"))
    private void tfthperformancefix$saveExtras(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (this.level != null) {
            tag.putString(DIMENSION_KEY, this.level.dimension().location().toString());
        }
        if (this.visitedPositions instanceof CompactPosSet visited) {
            tag.putLongArray(VISITED_KEY, visited.toLongArray());
        }
    }

    @Inject(method = "loadStateFromNBT", at = @At("RETURN"))
    private void tfthperformancefix$loadExtras(CompoundTag tag, CallbackInfo ci) {
        this.tfthperformancefix$savedDimension = tag.contains(DIMENSION_KEY, Tag.TAG_STRING) ? tag.getString(DIMENSION_KEY) : null;
        if (tag.contains(VISITED_KEY, Tag.TAG_LONG_ARRAY) && this.visitedPositions instanceof CompactPosSet visited) {
            visited.addFromLongArray(tag.getLongArray(VISITED_KEY));
        }
    }

    /*
     * Whether incubators are near the spread is checked twice for every block it handles, each
     * check an entity search out to the spread's range. The answer can only change when an
     * incubator joins or leaves the level (they can't move), so it's worked out once per tick and
     * again only after such a change.
     */

    @Inject(method = "areIncubatorsNearby", at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$cachedAnyIncubator(CallbackInfoReturnable<Boolean> cir) {
        this.tfthperformancefix$refreshIncubatorCache();
        cir.setReturnValue(this.tfthperformancefix$incubatorOneNearby || this.tfthperformancefix$startingIncubatorNearby);
    }

    @Inject(method = "isNearIncubatorOne", at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$cachedIncubatorOne(CallbackInfoReturnable<Boolean> cir) {
        this.tfthperformancefix$refreshIncubatorCache();
        cir.setReturnValue(this.tfthperformancefix$incubatorOneNearby);
    }

    @Inject(method = "isPlaqueincubatorstartNearby", at = @At("HEAD"), cancellable = true)
    private void tfthperformancefix$cachedStartingIncubator(CallbackInfoReturnable<Boolean> cir) {
        this.tfthperformancefix$refreshIncubatorCache();
        cir.setReturnValue(this.tfthperformancefix$startingIncubatorNearby);
    }

    @Unique
    private void tfthperformancefix$refreshIncubatorCache() {
        long time = this.level.getGameTime();
        int generation = IncubatorTracker.generation();
        if (time == this.tfthperformancefix$cacheTime
                && generation == this.tfthperformancefix$cacheGeneration
                && this.CHECK_RADIUS == this.tfthperformancefix$cacheRadius
                && this.currentPosition.equals(this.tfthperformancefix$cachePos)) {
            return;
        }
        AABB area = new AABB(this.currentPosition).inflate(this.CHECK_RADIUS);
        this.tfthperformancefix$incubatorOneNearby = !this.level.getEntitiesOfClass(PlaqueincubatoroneEntity.class, area).isEmpty();
        this.tfthperformancefix$startingIncubatorNearby = !this.level.getEntitiesOfClass(PlaqueincubatorstartEntity.class, area).isEmpty();
        this.tfthperformancefix$cacheTime = time;
        this.tfthperformancefix$cacheGeneration = generation;
        this.tfthperformancefix$cacheRadius = this.CHECK_RADIUS;
        this.tfthperformancefix$cachePos = this.currentPosition;
    }
}
