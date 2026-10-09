package com.rinkynooble.tfthperformancefix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Finds blocks in a box without reading every position in it.
 *
 * <p>A chunk section keeps a palette of the block states it holds. If no state in the palette
 * matches, no block in the section can match, so the whole section is skipped. Sections that
 * might hold a match are read block by block, so the results are the same as reading every
 * position. Only loaded chunks are read: nothing here loads or generates a chunk.
 */
public final class SectionScan {
    @FunctionalInterface
    public interface Visitor {
        void visit(int x, int y, int z, BlockState state);
    }

    private SectionScan() {
    }

    /**
     * Calls {@code visitor} for every block in the box (bounds inclusive) that {@code match}
     * accepts. Unloaded chunks and positions outside the build height are skipped. The predicate
     * must never accept air, since all-air sections are skipped without checking.
     */
    public static void forEachMatch(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                    Predicate<BlockState> match, Visitor visitor) {
        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            int x0 = Math.max(minX, chunkX << 4);
            int x1 = Math.min(maxX, (chunkX << 4) + 15);
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                int z0 = Math.max(minZ, chunkZ << 4);
                int z1 = Math.min(maxZ, (chunkZ << 4) + 15);
                LevelChunkSection[] sections = chunk.getSections();
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(sectionY)];
                    if (section.hasOnlyAir() || !section.maybeHas(match)) {
                        continue;
                    }
                    int y0 = Math.max(minY, sectionY << 4);
                    int y1 = Math.min(maxY, (sectionY << 4) + 15);
                    for (int x = x0; x <= x1; x++) {
                        for (int y = y0; y <= y1; y++) {
                            for (int z = z0; z <= z1; z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (match.test(state)) {
                                    visitor.visit(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** How many blocks in a cube match, and the squared distance from the centre to the nearest one. */
    public static final class CountResult {
        public int count;
        /** {@code Double.MAX_VALUE} when nothing matched. */
        public double nearestDistSqr = Double.MAX_VALUE;
    }

    /**
     * Counts the matching blocks in the cube {@code center ± radius}, and finds the squared
     * distance to the nearest one, measured the way {@code BlockPos.distSqr} does.
     */
    public static CountResult countInCube(Level level, BlockPos center, int radius, Predicate<BlockState> match) {
        CountResult result = new CountResult();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        forEachMatch(level, cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius, match,
                (x, y, z, state) -> {
                    result.count++;
                    double dx = x - cx;
                    double dy = y - cy;
                    double dz = z - cz;
                    double distSqr = dx * dx + dy * dy + dz * dz;
                    if (distSqr < result.nearestDistSqr) {
                        result.nearestDistSqr = distSqr;
                    }
                });
        return result;
    }

    /**
     * The block at {@code pos} if its chunk is loaded, void air outside the build height (as
     * {@code Level.getBlockState} returns there), or {@code null} if the chunk isn't loaded.
     */
    @Nullable
    public static BlockState getIfLoaded(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
