/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla's redstone, repeater and comparator items place the wall and ceiling forms. The rule
 * is the torch's: after the clicked block has had its chance, the directions the placer looks
 * along are tried nearest first, with the clicked face's inward direction ahead of them all.
 * Down means the floor form, which vanilla places itself if it can stand there; a horizontal
 * direction means the wall form on the block that way, if it can hang there; up means the
 * ceiling form under the block above. Clicking a wall face therefore gives the wall form,
 * clicking a floor the floor form and clicking the underside of a block the ceiling form, as
 * with torches.
 */
public final class Placement {
    private Placement() {}

    static void useItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
            return;
        }
        ItemStack stack = event.getItemStack();
        Block floor = floorFormOf(stack);
        if (floor == null) {
            return;
        }
        UseOnContext use = event.getUseOnContext();
        Player player = use.getPlayer();
        if (player == null || !player.getAbilities().mayBuild) {
            return;
        }
        BlockPlaceContext context = new BlockPlaceContext(use);
        if (!context.canPlace()) {
            return;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction d : context.getNearestLookingDirections()) {
            if (d == Direction.DOWN) {
                BlockState floorState = floor.getStateForPlacement(context);
                if (floorState != null && floorState.canSurvive(level, pos)) {
                    return;   // vanilla places its floor form
                }
                continue;
            }
            BlockState state = wallState(floor, level, pos, d.getOpposite(), player);
            if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.empty())) {
                continue;
            }
            place(level, pos, state, player, stack);
            event.cancelWithResult(ItemInteractionResult.sidedSuccess(level.isClientSide));
            return;
        }
    }

    /** effects: returns the floor block the item places, or null for any other item */
    @Nullable
    static Block floorFormOf(ItemStack stack) {
        if (stack.is(Items.REDSTONE)) return Blocks.REDSTONE_WIRE;
        if (stack.is(Items.REPEATER)) return Blocks.REPEATER;
        if (stack.is(Items.COMPARATOR)) return Blocks.COMPARATOR;
        return null;
    }

    /**
     * effects: returns the form of {@code floor} placed at {@code pos} on the block whose face
     * points {@code normal} (a wall for a horizontal normal, the ceiling for down), by {@code placer}
     */
    @Nullable
    static BlockState wallState(Block floor, Level level, BlockPos pos, Direction normal, Player placer) {
        if (floor == Blocks.REDSTONE_WIRE) {
            return WallRedstoneWireBlock.placementState(level, pos, normal);
        }
        if (floor == Blocks.REPEATER) {
            return ModBlocks.WALL_REPEATER.get().placementState(level, pos, normal, placer.getLookAngle());
        }
        if (floor == Blocks.COMPARATOR) {
            return ModBlocks.WALL_COMPARATOR.get().placementState(level, pos, normal, placer.getLookAngle());
        }
        return null;
    }

    /** effects: puts {@code state} in the world the way a block item does: placed-by hooks, game event, sound, stat, advancement, one item spent */
    private static void place(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        level.setBlock(pos, state, 11);
        BlockState placed = level.getBlockState(pos);
        placed.getBlock().setPlacedBy(level, pos, placed, player, stack);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
        SoundType sound = placed.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
    }
}
