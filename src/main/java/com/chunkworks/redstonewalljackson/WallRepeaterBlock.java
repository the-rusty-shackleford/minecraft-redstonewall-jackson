/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A repeater on a wall: vanilla's, stood up. Click to cycle the delay; locked by a diode of either kind pointing into its side. */
public final class WallRepeaterBlock extends WallDiodeBlock {

    public static final MapCodec<WallRepeaterBlock> CODEC = simpleCodec(WallRepeaterBlock::new);
    public static final IntegerProperty DELAY = BlockStateProperties.DELAY;
    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;

    public WallRepeaterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WALL, Direction.NORTH).setValue(FACING, Direction.DOWN)
                .setValue(DELAY, 1).setValue(LOCKED, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<WallRepeaterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState placementState(LevelReader level, BlockPos pos, Direction normal, Vec3 look) {
        BlockState state = super.placementState(level, pos, normal, look);
        return state.setValue(LOCKED, isLocked(level, pos, state));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, state.cycle(DELAY), 3);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected int getDelay(BlockState state) {
        return state.getValue(DELAY) * 2;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction up = state.getValue(WALL);
        if (dir == up.getOpposite() && !canSurviveOn(level, neighborPos, neighbor, up)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!level.isClientSide() && dir.getAxis() != state.getValue(FACING).getAxis()) {
            return state.setValue(LOCKED, isLocked(level, pos, state));
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return getAlternateSignal(level, pos, state) > 0;
    }

    @Override
    protected boolean sideInputDiodesOnly() {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(Items.REPEATER);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        // Vanilla's torch sparkle, with the offset along the facing instead of across the floor
        // and the height above the base measured along the wall's normal.
        Direction up = state.getValue(WALL);
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float offset = -5.0F;
        if (random.nextBoolean()) {
            offset = state.getValue(DELAY) * 2 - 1;
        }
        offset /= 16.0F;
        double lift = -0.1;   // 0.4 up from a floor base: a tenth below the block's centre, along the normal
        level.addParticle(DustParticleOptions.REDSTONE,
                x + offset * facing.getStepX() + lift * up.getStepX(),
                y + offset * facing.getStepY() + lift * up.getStepY(),
                z + offset * facing.getStepZ() + lift * up.getStepZ(), 0.0, 0.0, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WALL, FACING, DELAY, LOCKED, POWERED);
    }
}
