/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Redstone dust, repeaters and comparators on walls, behaving exactly as on a floor. The same
 * items place the wall forms; the wall forms drop the same items; the rules are vanilla's,
 * written once against a plane and turned upright.
 */
@Mod(RedstonewallJackson.MOD_ID)
public final class RedstonewallJackson {
    public static final String MOD_ID = "redstonewalljackson";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstonewallJackson(IEventBus modBus) {
        ModBlocks.register(modBus);
        // Placement is a game-bus event: the vanilla items, clicked on a wall face, place the wall forms.
        NeoForge.EVENT_BUS.addListener(Placement::useItemOnBlock);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
