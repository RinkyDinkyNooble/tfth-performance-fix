package com.rinkynooble.tfthperformancefix;

import net.mcreator.thefleshthathates.block.FleshBlocks;
import net.mcreator.thefleshthathates.init.TheFleshThatHatesModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.function.Predicate;

/** Block checks shared by the patches. TFTH's classes are only touched when a check first runs. */
public final class TfthBlocks {
    /** Any block TFTH counts as its own for fog and music ({@code FleshBlocks.isModBlock}). */
    public static final Predicate<BlockState> MOD_BLOCK = state -> FleshBlocks.isModBlock(state.getBlock());

    /** TFTH's plain flesh block, in any state. */
    public static final Predicate<BlockState> FLESH_BLOCK = state -> state.is(TheFleshThatHatesModBlocks.FLESH_BLOCK.get());

    /** The order TFTH's nested x, then y, then z loops visit positions in. */
    public static final Comparator<BlockPos> LOOP_ORDER =
            Comparator.<BlockPos>comparingInt(pos -> pos.getX()).thenComparingInt(pos -> pos.getY()).thenComparingInt(pos -> pos.getZ());

    private TfthBlocks() {
    }
}
