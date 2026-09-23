/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The three wall forms. No items: vanilla's redstone, repeater and comparator items place them
 * ({@link Placement}) and they drop those items, so nothing new appears in a tab or a recipe.
 * Each copies its floor block's properties whole, so hardness, sounds and piston behaviour match.
 */
public final class ModBlocks {
    private ModBlocks() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstonewallJackson.MOD_ID);

    public static final DeferredBlock<WallRedstoneWireBlock> WALL_REDSTONE_WIRE = BLOCKS.registerBlock(
            "wall_redstone_wire", WallRedstoneWireBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE));
    public static final DeferredBlock<WallRepeaterBlock> WALL_REPEATER = BLOCKS.registerBlock(
            "wall_repeater", WallRepeaterBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER));
    public static final DeferredBlock<WallComparatorBlock> WALL_COMPARATOR = BLOCKS.registerBlock(
            "wall_comparator", WallComparatorBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.COMPARATOR));

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RedstonewallJackson.MOD_ID);

    /** The wall comparator's output, kept in vanilla's own comparator entity under this mod's type. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallComparatorBlockEntity>> WALL_COMPARATOR_ENTITY =
            BLOCK_ENTITIES.register("wall_comparator", () -> BlockEntityType.Builder.of(WallComparatorBlockEntity::new, WALL_COMPARATOR.get()).build(null));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
