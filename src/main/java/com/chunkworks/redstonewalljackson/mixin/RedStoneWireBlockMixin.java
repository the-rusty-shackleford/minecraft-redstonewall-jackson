/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.mixin;

import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Floor dust meets wall dust in two places vanilla never looks. Beside it and diagonally up or
 * down a step, vanilla reads the power of the wires it touches through a check that the block
 * is its own class; wall dust at those places is a wire too. And directly above it, where a
 * floor run turns up a wall (the inside corner), vanilla never expects a wire: floor dust
 * counts the wall dust above it as a neighbour when that dust rests on the block beside, and
 * draws the joint that way as its climb up that block's face. Both the same one point of
 * power down that every wire-to-wire step loses.
 */
@Mixin(RedStoneWireBlock.class)
abstract class RedStoneWireBlockMixin {

    @Inject(method = "getWireSignal", at = @At("HEAD"), cancellable = true)
    private void redstonewalljackson$wallDustIsAWire(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.getBlock() instanceof WallRedstoneWireBlock) {
            cir.setReturnValue(state.getValue(WallRedstoneWireBlock.POWER));
        }
    }

    @Inject(method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;",
            at = @At("HEAD"), cancellable = true)
    private void redstonewalljackson$climbToTheWallDustAbove(BlockGetter level, BlockPos pos, Direction direction, boolean nonNormalCubeAbove,
                                                               CallbackInfoReturnable<RedstoneSide> cir) {
        if (WallRedstoneWireBlock.restsToward(level.getBlockState(pos.above())).filter(direction::equals).isPresent()) {
            BlockPos sidePos = pos.relative(direction);
            boolean sturdy = level.getBlockState(sidePos).isFaceSturdy(level, sidePos, direction.getOpposite());
            cir.setReturnValue(sturdy ? RedstoneSide.UP : RedstoneSide.SIDE);
        }
    }

    @Inject(method = "calculateTargetStrength", at = @At("RETURN"), cancellable = true)
    private void redstonewalljackson$readTheWallDustAbove(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BlockState above = level.getBlockState(pos.above());
        if (WallRedstoneWireBlock.restsToward(above).filter(d -> d.getAxis().isHorizontal()).isPresent()) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), above.getValue(WallRedstoneWireBlock.POWER) - 1));
        }
    }
}
