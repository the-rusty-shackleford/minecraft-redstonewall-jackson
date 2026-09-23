/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.mixin;

import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Vanilla's dust silences every wire while any wire computes its power, through one private
 * flag on the block, so no wire reads itself or another wire back through a strongly powered
 * block. Wall dust must share that silence in both directions: read it while vanilla computes,
 * and set it while wall dust computes.
 */
@Mixin(RedStoneWireBlock.class)
public interface RedStoneWireBlockAccessor {

    @Accessor("shouldSignal")
    boolean redstonewalljackson$shouldSignal();

    @Accessor("shouldSignal")
    void redstonewalljackson$setShouldSignal(boolean shouldSignal);
}
