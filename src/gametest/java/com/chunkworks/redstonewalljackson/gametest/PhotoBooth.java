/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.RedstonewallJackson;
import com.chunkworks.redstonewalljackson.WallComparatorBlock;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import com.chunkworks.redstonewalljackson.WallRepeaterBlock;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Eyes for the wall blocks: builds a wall with a powered dust run climbing it, a repeater and
 * two comparators on it, a floor run in front for comparison, then photographs the scene from
 * the front, from an angle and close up, and quits. Run with {@code ./gradlew runPhotoBooth};
 * the pictures land in {@code run/booth/screenshots}. What to look for is in the README.
 */
@EventBusSubscriber(modid = "redstonewalljackson_gametest", value = Dist.CLIENT)
public final class PhotoBooth {
    private PhotoBooth() {}

    private static final boolean ACTIVE = Boolean.getBoolean("redstonewalljackson.booth");
    private static int tick;
    private static int shot;
    private static String pending;
    private static boolean built;
    private static BlockPos origin;

    /** Camera poses: position relative to the origin, then yaw and pitch. */
    private static final double[][] POSES = {
            {5.5, 2.0, -6.0, 0, 8},       // front: the wall face-on
            {-3.0, 3.0, -4.0, -40, 20},   // angled from the west, looking down a little
            {9.0, 2.4, -3.6, 0, 5},       // close: the repeater and the two comparators
            {0.5, 1.5, -3.5, -35, 10},    // the seam where the floor run meets the wall run
            {6.5, 0.6, -3.2, 0, -35},     // from below: the climb onto the block standing out of the wall
    };
    private static final String[] NAMES = {"wall-front", "wall-angle", "wall-close", "wall-seam", "wall-climb"};

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tick++;
        if (!built) {
            built = true;
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if (sp == null) return;
                sp.setGameMode(GameType.CREATIVE);
                ServerLevel level = sp.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, sp.server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, sp.server);
                origin = sp.blockPosition().offset(0, 0, 6);
                build(level, origin);
            });
            return;
        }
        if (origin == null || tick < 60) return;
        if (shot >= POSES.length) {
            if (pending == null) {
                RedstonewallJackson.LOGGER.info("redstonewalljackson booth: COMPLETE {} photos", POSES.length);
                mc.stop();
            }
            return;
        }
        if (pending == null && tick % 30 == 0) {
            double[] pose = POSES[shot];
            mc.player.setPos(origin.getX() + pose[0], origin.getY() + pose[1], origin.getZ() + pose[2]);
            mc.player.setYRot((float) pose[3]);
            mc.player.setXRot((float) pose[4]);
            mc.player.setYHeadRot((float) pose[3]);
            mc.player.setDeltaMovement(0, 0, 0);
            mc.player.getAbilities().flying = true;
            pending = NAMES[shot];
        }
        if (tick > 1200) {
            RedstonewallJackson.LOGGER.error("redstonewalljackson booth: FAIL timed out at photo {}", shot);
            mc.stop();
        }
    }

    /** The scene: a stone wall along x at z, dust climbing it from a floor run, diodes on it, a lamp at the top. */
    private static void build(ServerLevel level, BlockPos o) {
        for (int x = -2; x <= 12; x++) {
            for (int z = -8; z <= 1; z++) {
                level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
            for (int y = 0; y <= 6; y++) {
                level.setBlock(o.offset(x, y, 0), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }
        // A floor run from a redstone block toward the wall, meeting a run climbing the wall to a lamp.
        // The sources go in last, so everything they reach updates as it would in play.
        for (int z = -5; z <= -2; z++) {
            level.setBlock(o.offset(2, 0, z), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        }
        for (int y = 0; y <= 4; y++) {
            BlockPos pos = o.offset(2, y, -1);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, Direction.NORTH), 3);
        }
        level.setBlock(o.offset(2, 5, -1), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        // A run along the wall through a repeater to a comparator pair, powered from a redstone block on the wall.
        for (int x = 6; x <= 7; x++) {
            BlockPos pos = o.offset(x, 2, -1);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, Direction.NORTH), 3);
        }
        level.setBlock(o.offset(8, 2, -1), diode(ModBlocks.WALL_REPEATER.get().defaultBlockState(), Direction.WEST).setValue(WallRepeaterBlock.DELAY, 3), 3);
        BlockPos after = o.offset(9, 2, -1);
        level.setBlock(after, WallRedstoneWireBlock.placementState(level, after, Direction.NORTH), 3);
        level.setBlock(o.offset(10, 2, -1), diode(ModBlocks.WALL_COMPARATOR.get().defaultBlockState(), Direction.WEST), 3);
        level.setBlock(o.offset(10, 4, -1), diode(ModBlocks.WALL_COMPARATOR.get().defaultBlockState(), Direction.DOWN).setValue(WallComparatorBlock.MODE, ComparatorMode.SUBTRACT), 3);
        BlockPos between = o.offset(10, 3, -1);
        level.setBlock(between, WallRedstoneWireBlock.placementState(level, between, Direction.NORTH), 3);
        // A block standing out of the wall with dust climbing onto its face.
        level.setBlock(o.offset(6, 3, -1), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        BlockPos onIt = o.offset(6, 3, -2);
        level.setBlock(onIt, WallRedstoneWireBlock.placementState(level, onIt, Direction.NORTH), 3);
        level.setBlock(o.offset(2, 0, -6), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(o.offset(5, 2, -1), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
    }

    private static BlockState diode(BlockState state, Direction facing) {
        return state.setValue(WallDiodeBlock.WALL, Direction.NORTH).setValue(WallDiodeBlock.FACING, facing);
    }

    @SubscribeEvent
    public static void rendered(RenderGuiEvent.Post event) {
        if (!ACTIVE || pending == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || tick % 30 < 8) return;   // a few frames for the chunks to rebuild after the move
        String name = pending;
        pending = null;
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {});
        RedstonewallJackson.LOGGER.info("redstonewalljackson booth: photo {}", name);
        shot++;
    }
}
