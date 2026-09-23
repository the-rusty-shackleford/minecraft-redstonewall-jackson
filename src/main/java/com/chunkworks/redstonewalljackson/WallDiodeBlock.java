/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.chunkworks.redstonewalljackson.domain.Diode;
import com.chunkworks.redstonewalljackson.domain.Dir;
import com.chunkworks.redstonewalljackson.domain.Frame;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A repeater or comparator on a wall: vanilla's {@link DiodeBlock} with its plane stood up.
 * {@code WALL} is the wall's outward normal, {@code FACING} the direction its input comes from
 * (a planar direction of that wall: up, down, or along it), {@code POWERED} its output. The
 * input is read from the block toward FACING, the output given toward its opposite, the lock
 * and side inputs read beside it in the plane, and it rests on the wall block as a floor diode
 * rests on the block below. Every timing is vanilla's.
 *
 * <p>RI: FACING lies in the wall's plane, never along its normal.
 */
public abstract class WallDiodeBlock extends Block {

    public static final DirectionProperty WALL = DirectionProperty.create("wall", Direction.Plane.HORIZONTAL);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction wall : Direction.Plane.HORIZONTAL) {
            SHAPES.put(wall, Frames.box(Frames.wall(wall), 0, 0, 0, 16, 16, 2));
        }
    }

    protected WallDiodeBlock(Properties properties) {
        super(properties);
    }

    static Frame frame(BlockState state) {
        return Frames.wall(state.getValue(WALL));
    }

    /** effects: returns the state of this diode placed on the wall whose face points {@code normal}, its output where {@code look} points along the wall */
    public BlockState placementState(LevelReader level, BlockPos pos, Direction normal, Vec3 look) {
        Dir facing = Diode.facingFromLook(Frames.wall(normal), look.x, look.y, look.z);
        return defaultBlockState().setValue(WALL, normal).setValue(FACING, Frames.dir(facing));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(WALL));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(WALL).getOpposite());
        return canSurviveOn(level, support, level.getBlockState(support), state.getValue(WALL));
    }

    /** effects: returns whether the block at {@code pos} bears a diode on its face toward {@code up}, as a floor's block bears one on top */
    protected static boolean canSurviveOn(LevelReader level, BlockPos pos, BlockState state, Direction up) {
        return state.isFaceSturdy(level, pos, up, SupportType.RIGID);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isLocked(level, pos, state)) {
            return;
        }
        boolean powered = state.getValue(POWERED);
        boolean shouldTurnOn = shouldTurnOn(level, pos, state);
        if (powered && !shouldTurnOn) {
            level.setBlock(pos, state.setValue(POWERED, false), 2);
        } else if (!powered) {
            level.setBlock(pos, state.setValue(POWERED, true), 2);
            if (!shouldTurnOn) {
                level.scheduleTick(pos, this, getDelay(state), TickPriority.VERY_HIGH);
            }
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getSignal(level, pos, side);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (!state.getValue(POWERED)) {
            return 0;
        }
        return state.getValue(FACING) == side ? getOutputSignal(level, pos, state) : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (state.canSurvive(level, pos)) {
            checkTickOnNeighbor(level, pos, state);
        } else {
            BlockEntity entity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            dropResources(state, level, pos, entity);
            level.removeBlock(pos, false);
            for (Direction d : Direction.values()) {
                level.updateNeighborsAt(pos.relative(d), this);
            }
        }
    }

    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (isLocked(level, pos, state)) {
            return;
        }
        boolean powered = state.getValue(POWERED);
        boolean shouldTurnOn = shouldTurnOn(level, pos, state);
        if (powered != shouldTurnOn && !level.getBlockTicks().willTickThisTick(pos, this)) {
            TickPriority priority = TickPriority.HIGH;
            if (shouldPrioritize(level, pos, state)) {
                priority = TickPriority.EXTREMELY_HIGH;
            } else if (powered) {
                priority = TickPriority.VERY_HIGH;
            }
            level.scheduleTick(pos, this, getDelay(state), priority);
        }
    }

    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        return getInputSignal(level, pos, state) > 0;
    }

    /** effects: returns the signal from the block toward FACING, a wire's power counting even when it points elsewhere, as vanilla reads it */
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos input = pos.relative(facing);
        int signal = level.getSignal(input, facing);
        if (signal >= 15) {
            return signal;
        }
        return Math.max(signal, Math.max(0, Views.wirePower(level.getBlockState(input))));
    }

    /** effects: returns the stronger of the two side inputs beside FACING in the plane */
    protected int getAlternateSignal(SignalGetter level, BlockPos pos, BlockState state) {
        List<Dir> sides = Diode.sides(frame(state), Frames.dir(state.getValue(FACING)));
        boolean diodesOnly = sideInputDiodesOnly();
        int best = 0;
        for (Dir side : sides) {
            Direction d = Frames.dir(side);
            best = Math.max(best, controlInputSignal(level, pos.relative(d), d, diodesOnly));
        }
        return best;
    }

    /**
     * effects: vanilla's control input signal, with this mod's wall dust and wall diodes counted
     * as vanilla counts its own: a redstone block is 15, a wire its power, a diode (when only
     * diodes count) or any signal source its direct signal
     */
    static int controlInputSignal(SignalGetter level, BlockPos pos, Direction direction, boolean diodesOnly) {
        BlockState state = level.getBlockState(pos);
        if (diodesOnly) {
            return isDiode(state) ? level.getDirectSignal(pos, direction) : 0;
        }
        if (state.is(Blocks.REDSTONE_BLOCK)) {
            return 15;
        }
        if (Views.isWire(state)) {
            return Views.wirePower(state);
        }
        return state.isSignalSource() ? level.getDirectSignal(pos, direction) : 0;
    }

    /** effects: returns whether {@code state} is a repeater or comparator of either kind */
    static boolean isDiode(BlockState state) {
        return DiodeBlock.isDiode(state) || state.getBlock() instanceof WallDiodeBlock;
    }

    /** effects: returns the input direction of a diode of either kind; requires: {@link #isDiode} */
    static Direction facingOf(BlockState state) {
        return state.getBlock() instanceof WallDiodeBlock ? state.getValue(FACING) : state.getValue(DiodeBlock.FACING);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (shouldTurnOn(level, pos, state)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        updateNeighborsInFront(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            updateNeighborsInFront(level, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing.getOpposite());
        if (EventHooks.onNeighborNotify(level, pos, level.getBlockState(pos), EnumSet.of(facing.getOpposite()), false).isCanceled()) {
            return;
        }
        level.neighborChanged(front, this, pos);
        level.updateNeighborsAtExceptFromFacing(front, this, facing);
    }

    protected boolean sideInputDiodesOnly() {
        return false;
    }

    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return 15;
    }

    /** effects: returns whether a diode of either kind stands in front, not pointing back at this one: vanilla's reason to tick earlier */
    public boolean shouldPrioritize(BlockGetter level, BlockPos pos, BlockState state) {
        Direction front = state.getValue(FACING).getOpposite();
        BlockState ahead = level.getBlockState(pos.relative(front));
        return isDiode(ahead) && facingOf(ahead) != front;
    }

    protected abstract int getDelay(BlockState state);

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(WALL, rotation.rotate(state.getValue(WALL))).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(WALL, mirror.mirror(state.getValue(WALL))).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** effects: returns whether {@code state} is vanilla's redstone dust, for the callers that read its power by name */
    static boolean isVanillaWire(BlockState state) {
        return state.getBlock() instanceof RedStoneWireBlock;
    }
}
