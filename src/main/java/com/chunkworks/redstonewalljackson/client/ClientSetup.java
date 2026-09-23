/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.client;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.RedstonewallJackson;
import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Wall dust takes vanilla's own colour for its power, so it glows exactly as floor dust does. */
@EventBusSubscriber(modid = RedstonewallJackson.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> RedStoneWireBlock.getColorForPower(state.getValue(WallRedstoneWireBlock.POWER)),
                ModBlocks.WALL_REDSTONE_WIRE.get());
    }
}
