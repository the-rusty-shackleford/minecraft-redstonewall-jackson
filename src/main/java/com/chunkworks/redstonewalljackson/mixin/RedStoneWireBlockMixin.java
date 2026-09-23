/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.mixin;

import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The one touch on vanilla: floor dust reads the power of the wires it touches, beside it and
 * diagonally up or down a step, through a check that the block is its own class. Wall dust at
 * those places is a wire too, so the seam between a floor run and a wall run loses one point
 * of power as every wire-to-wire step does, instead of being blind.
 */
@Mixin(RedStoneWireBlock.class)
abstract class RedStoneWireBlockMixin {

    @Inject(method = "getWireSignal", at = @At("HEAD"), cancellable = true)
    private void redstonewalljackson$wallDustIsAWire(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.getBlock() instanceof WallRedstoneWireBlock) {
            cir.setReturnValue(state.getValue(WallRedstoneWireBlock.POWER));
        }
    }
}
