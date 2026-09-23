/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

/**
 * A comparator on a wall: vanilla's, stood up, keeping its output in vanilla's own comparator
 * block entity under this mod's entity type. It reads a container or an item frame behind it
 * along the wall, as a floor comparator reads behind it.
 */
public final class WallComparatorBlock extends WallDiodeBlock implements EntityBlock {

    public static final MapCodec<WallComparatorBlock> CODEC = simpleCodec(WallComparatorBlock::new);
    public static final EnumProperty<ComparatorMode> MODE = BlockStateProperties.MODE_COMPARATOR;

    public WallComparatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WALL, Direction.NORTH).setValue(FACING, Direction.DOWN)
                .setValue(POWERED, false).setValue(MODE, ComparatorMode.COMPARE));
    }

    @Override
    protected MapCodec<WallComparatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction up = state.getValue(WALL);
        if (dir == up.getOpposite() && !canSurviveOn(level, neighborPos, neighbor, up)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof ComparatorBlockEntity entity ? entity.getOutputSignal() : 0;
    }

    private int calculateOutputSignal(Level level, BlockPos pos, BlockState state) {
        int input = getInputSignal(level, pos, state);
        if (input == 0) {
            return 0;
        }
        int side = getAlternateSignal(level, pos, state);
        if (side > input) {
            return 0;
        }
        return state.getValue(MODE) == ComparatorMode.SUBTRACT ? input - side : input;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        int input = getInputSignal(level, pos, state);
        if (input == 0) {
            return false;
        }
        int side = getAlternateSignal(level, pos, state);
        return input > side || input == side && state.getValue(MODE) == ComparatorMode.COMPARE;
    }

    @Override
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        int input = super.getInputSignal(level, pos, state);
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing);
        BlockState behindState = level.getBlockState(behind);
        if (behindState.hasAnalogOutputSignal()) {
            input = behindState.getAnalogOutputSignal(level, behind);
        } else if (input < 15 && behindState.isRedstoneConductor(level, behind)) {
            behind = behind.relative(facing);
            behindState = level.getBlockState(behind);
            ItemFrame frame = getItemFrame(level, facing, behind);
            int measured = Math.max(frame == null ? Integer.MIN_VALUE : frame.getAnalogOutput(),
                    behindState.hasAnalogOutputSignal() ? behindState.getAnalogOutputSignal(level, behind) : Integer.MIN_VALUE);
            if (measured != Integer.MIN_VALUE) {
                input = measured;
            }
        }
        return input;
    }

    @Nullable
    private static ItemFrame getItemFrame(Level level, Direction facing, BlockPos pos) {
        List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                frame -> frame != null && frame.getDirection() == facing);
        return frames.size() == 1 ? frames.get(0) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        state = state.cycle(MODE);
        float pitch = state.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, pitch);
        level.setBlock(pos, state, 2);
        refreshOutputState(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockTicks().willTickThisTick(pos, this)) {
            return;
        }
        int output = calculateOutputSignal(level, pos, state);
        int stored = level.getBlockEntity(pos) instanceof ComparatorBlockEntity entity ? entity.getOutputSignal() : 0;
        if (output != stored || state.getValue(POWERED) != shouldTurnOn(level, pos, state)) {
            TickPriority priority = shouldPrioritize(level, pos, state) ? TickPriority.HIGH : TickPriority.NORMAL;
            level.scheduleTick(pos, this, 2, priority);
        }
    }

    private void refreshOutputState(Level level, BlockPos pos, BlockState state) {
        int output = calculateOutputSignal(level, pos, state);
        int stored = 0;
        if (level.getBlockEntity(pos) instanceof ComparatorBlockEntity entity) {
            stored = entity.getOutputSignal();
            entity.setOutputSignal(output);
        }
        if (stored != output || state.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean shouldTurnOn = shouldTurnOn(level, pos, state);
            boolean powered = state.getValue(POWERED);
            if (powered && !shouldTurnOn) {
                level.setBlock(pos, state.setValue(POWERED, false), 2);
            } else if (!powered && shouldTurnOn) {
                level.setBlock(pos, state.setValue(POWERED, true), 2);
            }
            updateNeighborsInFront(level, pos, state);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refreshOutputState(level, pos, state);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallComparatorBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(Items.COMPARATOR);
    }

    @Override
    public boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
        return state.is(this);
    }

    /** effects: re-reads the input when the container behind, one or two blocks along FACING, changed */
    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction facing = state.getValue(FACING);
        boolean behind = neighbor.equals(pos.relative(facing)) || neighbor.equals(pos.relative(facing, 2));
        if (behind && level instanceof Level world && !world.isClientSide()) {
            state.handleNeighborChanged(world, pos, level.getBlockState(neighbor).getBlock(), neighbor, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL, FACING, MODE, POWERED);
    }
}
