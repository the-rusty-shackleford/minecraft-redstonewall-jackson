/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.chunkworks.redstonewalljackson.domain.Cell;
import com.chunkworks.redstonewalljackson.domain.Frame;
import com.chunkworks.redstonewalljackson.domain.Planar;
import com.chunkworks.redstonewalljackson.domain.View;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the domain's {@link View} of a wire from the level, turned into the wire's frame:
 * the one place that reads neighbours for the wire rules. A wire is vanilla's dust or this
 * mod's wall dust alike; everything else is what the game says it is.
 */
final class Views {
    private Views() {}

    /** effects: returns whether {@code state} is a redstone wire of either kind */
    static boolean isWire(BlockState state) {
        return state.is(Blocks.REDSTONE_WIRE) || state.getBlock() instanceof WallRedstoneWireBlock;
    }

    /** effects: returns the power of the wire {@code state} is, or -1 if it is not one */
    static int wirePower(BlockState state) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return state.getValue(RedStoneWireBlock.POWER);
        }
        return state.getBlock() instanceof WallRedstoneWireBlock ? state.getValue(WallRedstoneWireBlock.POWER) : -1;
    }

    /** effects: returns whether a wire can rest on {@code state} at {@code pos} with {@code up} out of the plane: vanilla's canSurviveOn */
    static boolean standable(BlockGetter level, BlockPos pos, BlockState state, Direction up) {
        return state.isFaceSturdy(level, pos, up) || state.is(Blocks.HOPPER);
    }

    /** effects: returns the view of the wire at {@code pos} in frame {@code f}, with no signal read: enough for its shape */
    static View of(BlockGetter level, BlockPos pos, Frame f) {
        return of(level, pos, f, 0);
    }

    /** effects: returns the view of the wire at {@code pos} in frame {@code f} with {@code bestOtherSignal} as read by {@link #bestOtherSignal} */
    static View of(BlockGetter level, BlockPos pos, Frame f, int bestOtherSignal) {
        Direction up = Frames.dir(f.up());
        EnumMap<Planar, Cell> side = new EnumMap<>(Planar.class), sideUp = new EnumMap<>(Planar.class), sideDown = new EnumMap<>(Planar.class);
        for (Planar p : Planar.values()) {
            Direction d = Frames.world(f, p);
            BlockPos sp = pos.relative(d);
            BlockState ss = level.getBlockState(sp);
            side.put(p, new Cell(wirePower(ss), ss.isRedstoneConductor(level, sp),
                    ss.getBlock() instanceof TrapDoorBlock || standable(level, sp, ss, up),
                    ss.isFaceSturdy(level, sp, d.getOpposite()), ss.canRedstoneConnectTo(level, sp, d)));
            sideUp.put(p, beyond(level, sp.relative(up)));
            sideDown.put(p, beyond(level, sp.relative(up.getOpposite())));
        }
        BlockPos ap = pos.relative(up);
        BlockState as = level.getBlockState(ap);
        Cell above = new Cell(wirePower(as), as.isRedstoneConductor(level, ap), false, false, false);
        return new View(above, side, sideUp, sideDown, bestOtherSignal);
    }

    /** The block a step beyond a side, up or down: only whether it is a wire, conducts, and connects with no direction asked. */
    private static Cell beyond(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return new Cell(wirePower(s), s.isRedstoneConductor(level, pos), false, false, s.canRedstoneConnectTo(level, pos, null));
    }

    /**
     * effects: returns the strongest signal any of the six neighbours of {@code pos} that is not a
     * wire gives it: vanilla's best neighbour signal with its own wires silenced, since wires read
     * each other's power directly and lose a point across each step
     */
    static int bestOtherSignal(SignalGetter level, BlockPos pos) {
        int best = 0;
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            if (isWire(level.getBlockState(np))) {
                continue;
            }
            best = Math.max(best, level.getSignal(np, d));
            if (best >= 15) {
                return 15;
            }
        }
        return best;
    }
}
