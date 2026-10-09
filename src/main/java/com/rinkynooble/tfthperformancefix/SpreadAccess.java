package com.rinkynooble.tfthperformancefix;

import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/** Added to TFTH's {@code FleshBlockSpread} by {@code FleshBlockSpreadMixin}. */
public interface SpreadAccess {
    /** The dimension the spread was saved in, or {@code null} if it was saved without this mod. */
    @Nullable
    String tfthperformancefix$getSavedDimension();

    @Nullable
    ServerLevel tfthperformancefix$getLevel();
}
