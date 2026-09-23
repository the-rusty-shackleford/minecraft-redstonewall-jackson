/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's comparator block entity, one integer of output, reporting this mod's own entity
 * type so it saves and loads under the wall comparator. Nothing else is changed: the output
 * signal, its NBT and its events are vanilla's.
 */
public final class WallComparatorBlockEntity extends ComparatorBlockEntity {

    public WallComparatorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlocks.WALL_COMPARATOR_ENTITY.get();
    }
}
