/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import com.chunkworks.redstonewalljackson.WallRepeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The pieces every rig is built from: stone, dust of either kind placed as the items place it, a diode, a power reading. */
final class Rig {
    private Rig() {}

    static void stone(GameTestHelper h, int x, int y, int z) {
        h.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
    }

    /** This mod's dust at {@code pos} on the block whose face points {@code normal}, placed as the item places it. */
    static void dust(GameTestHelper h, BlockPos pos, Direction normal) {
        h.setBlock(pos, WallRedstoneWireBlock.placementState(h.getLevel(), h.absolutePos(pos), normal));
    }

    /** Vanilla's dust at {@code pos}, placed as the item places it: a cross drawn against its neighbours. */
    static void floorDust(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, Blocks.REDSTONE_WIRE.getStateForPlacement(new BlockPlaceContext(
                h.makeMockPlayer(GameType.SURVIVAL), InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE),
                new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos).below(), false))));
    }

    /** A repeater on the plane whose face points {@code normal}, its input from {@code facing}. */
    static BlockState repeater(Direction normal, Direction facing, int delay) {
        return ModBlocks.WALL_REPEATER.get().defaultBlockState().setValue(WallDiodeBlock.WALL, normal)
                .setValue(WallDiodeBlock.FACING, facing).setValue(WallRepeaterBlock.DELAY, delay);
    }

    /** The power of the dust of either kind at {@code pos}. */
    static int power(GameTestHelper h, BlockPos pos) {
        BlockState s = h.getBlockState(pos);
        return s.getValue(s.is(Blocks.REDSTONE_WIRE) ? RedStoneWireBlock.POWER : WallRedstoneWireBlock.POWER);
    }
}
