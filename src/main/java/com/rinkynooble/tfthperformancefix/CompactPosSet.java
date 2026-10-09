package com.rinkynooble.tfthperformancefix;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A set of block positions, given as {@link BlockPos#asLong} values, kept as one bit per
 * position in 16x16x16 sections. A section someone filled costs 512 bytes, a sixtieth of what
 * the same 4,096 positions take in a {@code HashSet<Long>}.
 *
 * <p>Works as a {@code Set<Long>} for TFTH's code: {@code add}, {@code contains},
 * {@code clear}, {@code size} and iteration. Removing single positions isn't supported; TFTH
 * never does.
 */
public final class CompactPosSet extends AbstractSet<Long> {
    private static final int WORDS = 64;

    private final Long2ObjectOpenHashMap<long[]> sections = new Long2ObjectOpenHashMap<>();
    private int size;

    private static long sectionKey(long pos) {
        return SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4);
    }

    private static int bitIndex(long pos) {
        return ((BlockPos.getY(pos) & 15) << 8) | ((BlockPos.getZ(pos) & 15) << 4) | (BlockPos.getX(pos) & 15);
    }

    public boolean containsPos(long pos) {
        long[] words = this.sections.get(sectionKey(pos));
        if (words == null) {
            return false;
        }
        int bit = bitIndex(pos);
        return (words[bit >>> 6] & (1L << bit)) != 0;
    }

    public boolean addPos(long pos) {
        long[] words = this.sections.computeIfAbsent(sectionKey(pos), key -> new long[WORDS]);
        int bit = bitIndex(pos);
        long mask = 1L << bit;
        if ((words[bit >>> 6] & mask) != 0) {
            return false;
        }
        words[bit >>> 6] |= mask;
        this.size++;
        return true;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Long pos && this.containsPos(pos);
    }

    @Override
    public boolean add(Long pos) {
        return this.addPos(pos);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        this.sections.clear();
        this.size = 0;
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<>() {
            private final ObjectIterator<Long2ObjectMap.Entry<long[]>> sectionIterator = sections.long2ObjectEntrySet().fastIterator();
            private long sectionKey;
            private long[] words;
            private int nextBit = 4096;

            {
                this.advance();
            }

            private void advance() {
                while (true) {
                    if (this.words != null) {
                        while (this.nextBit < 4096) {
                            if ((this.words[this.nextBit >>> 6] & (1L << this.nextBit)) != 0) {
                                return;
                            }
                            this.nextBit++;
                        }
                    }
                    if (!this.sectionIterator.hasNext()) {
                        this.words = null;
                        return;
                    }
                    Long2ObjectMap.Entry<long[]> entry = this.sectionIterator.next();
                    this.sectionKey = entry.getLongKey();
                    this.words = entry.getValue();
                    this.nextBit = 0;
                }
            }

            @Override
            public boolean hasNext() {
                return this.words != null;
            }

            @Override
            public Long next() {
                if (this.words == null) {
                    throw new NoSuchElementException();
                }
                int bit = this.nextBit++;
                long pos = BlockPos.asLong(
                        SectionPos.sectionToBlockCoord(SectionPos.x(this.sectionKey), bit & 15),
                        SectionPos.sectionToBlockCoord(SectionPos.y(this.sectionKey), bit >>> 8),
                        SectionPos.sectionToBlockCoord(SectionPos.z(this.sectionKey), (bit >>> 4) & 15));
                this.advance();
                return pos;
            }
        };
    }

    /** Each section as its key followed by its 64 words. */
    public long[] toLongArray() {
        long[] data = new long[this.sections.size() * (WORDS + 1)];
        int i = 0;
        for (Long2ObjectMap.Entry<long[]> entry : this.sections.long2ObjectEntrySet()) {
            data[i++] = entry.getLongKey();
            System.arraycopy(entry.getValue(), 0, data, i, WORDS);
            i += WORDS;
        }
        return data;
    }

    /** Adds the positions from {@link #toLongArray()}'s output. */
    public void addFromLongArray(long[] data) {
        for (int i = 0; i + WORDS < data.length; i += WORDS + 1) {
            long[] words = this.sections.computeIfAbsent(data[i], key -> new long[WORDS]);
            for (int w = 0; w < WORDS; w++) {
                long added = data[i + 1 + w] & ~words[w];
                words[w] |= added;
                this.size += Long.bitCount(added);
            }
        }
    }
}
