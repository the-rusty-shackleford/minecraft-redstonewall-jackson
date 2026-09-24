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
 * Eyes for the wall and ceiling blocks: builds a wall with a powered dust run climbing it, a
 * repeater and two comparators on it, a floor run in front for comparison; a pillar with an L
 * of dust on each face; a run from the floor up a wall, along a ceiling and round its front
 * edge to a lamp, through a ceiling repeater; and two walls meeting at an inside corner with a
 * run turning it. Then it photographs each from where its joints show, and quits. Run with
 * {@code ./gradlew runPhotoBooth}; the pictures land in {@code run/booth/screenshots}. What to
 * look for is in the README.
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
            // The pillar at x 16..18, z -8..-6: an L on each face, seen face-on from each side.
            {17.5, 2.5, -12.0, 0, 10},    // its north face, looking south
            {23.0, 2.5, -7.0, 90, 10},    // its east face, looking west
            {17.5, 2.5, -2.0, 180, 10},   // its south face, looking north
            {12.0, 2.5, -7.0, 270, 10},   // its west face, looking east
            // The stair at x=-6: floor run, up the wall, along the roof's underside, round its front edge to the lamp.
            {-6.0, 0.6, -11.0, 0, 2},     // from the front: the floor run to the wall's foot, the climb, the roof's front with the lamp
            {-6.0, 0.0, -6.8, 0, -32},    // from under the roof's edge, looking up: the ceiling run and the corner onto the wall
            {-4.2, 0.4, -2.2, 35, -28},   // close, from beside: the wall run meeting the ceiling run under the roof
            // Two walls meeting at x=23, z=-7: a run along one turning onto the other.
            {27.5, 1.0, -12.5, 39, 8},    // from outside the corner, looking into it
            // The diodes' faces, close enough to read the arrow: the repeater along the wall at x=8,
            // and a vertical run at x=-3 with a repeater whose output points up.
            {8.5, 1.5, -2.3, 0, -18},     // the along-the-wall repeater, its arrow should point east (the viewer's left)
            {-2.5, 1.6, -2.3, 0, -20},    // the upward repeater, its arrow should point up
            // Rusty's step at x=31: dust along the top of a block row, a repeater hanging on the row's face below
            // the edge, its output down into the ground, a lamp in the ground beside it.
            {31.5, 2.2, -8.0, 0, 28},     // from in front and above, looking down at the face, the top and the lamp
    };
    private static final String[] NAMES = {"wall-front", "wall-angle", "wall-close", "wall-seam", "wall-climb",
            "corner-north", "corner-east", "corner-south", "corner-west",
            "stair-front", "stair-under", "stair-close", "inside-corner",
            "repeater-along", "repeater-up", "row-repeater"};

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

    /** The scene: a stone wall along x at z, dust climbing it from a floor run, diodes on it, a lamp at the top; the pillar; the stair; the inside corner. */
    private static void build(ServerLevel level, BlockPos o) {
        for (int x = -10; x <= 12; x++) {
            for (int z = -12; z <= 1; z++) {
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
        // A pillar with an L of dust on each of its four faces: up from a redstone block at the foot, then
        // along to the right as seen facing that wall, so every wall's LEFT and RIGHT are in the picture.
        for (int x = 16; x <= 18; x++) {
            for (int z = -8; z <= -6; z++) {
                for (int y = -1; y <= 5; y++) {
                    level.setBlock(o.offset(x, y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
        }
        for (int y = 0; y <= 5; y++) {
            for (int dx = -1; dx <= 3; dx++) {
                level.setBlock(o.offset(15 + dx, y, -9), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        pillarCorner(level, o.offset(17, 0, -9), Direction.NORTH, Direction.WEST);   // north face: along to the west (the viewer's right)
        pillarCorner(level, o.offset(19, 0, -7), Direction.EAST, Direction.NORTH);   // east face: along to the north
        pillarCorner(level, o.offset(17, 0, -5), Direction.SOUTH, Direction.EAST);   // south face: along to the east
        pillarCorner(level, o.offset(15, 0, -7), Direction.WEST, Direction.SOUTH);   // west face: along to the south
        // The stair at x=-6: a roof at y=3 out from the wall over z -4..-1. A floor run to the wall's foot, wall dust
        // up to the roof, a ceiling run out along the roof's underside through a repeater, and round the roof's
        // front edge up its face to a lamp on top.
        for (int x = -8; x <= -4; x++) {
            for (int z = -4; z <= -1; z++) {
                level.setBlock(o.offset(x, 3, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }
        for (int z = -7; z <= -1; z++) {
            level.setBlock(o.offset(-6, 0, z), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        }
        for (int y = 1; y <= 2; y++) {
            BlockPos pos = o.offset(-6, y, -1);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, Direction.NORTH), 3);
        }
        BlockPos underFirst = o.offset(-6, 2, -2);
        level.setBlock(underFirst, WallRedstoneWireBlock.placementState(level, underFirst, Direction.DOWN), 3);
        level.setBlock(o.offset(-6, 2, -3), ModBlocks.WALL_REPEATER.get().defaultBlockState()
                .setValue(WallDiodeBlock.WALL, Direction.DOWN).setValue(WallDiodeBlock.FACING, Direction.SOUTH).setValue(WallRepeaterBlock.DELAY, 2), 3);
        BlockPos underLast = o.offset(-6, 2, -4);
        level.setBlock(underLast, WallRedstoneWireBlock.placementState(level, underLast, Direction.DOWN), 3);
        BlockPos roofFace = o.offset(-6, 3, -5);
        level.setBlock(roofFace, WallRedstoneWireBlock.placementState(level, roofFace, Direction.NORTH), 3);
        level.setBlock(o.offset(-6, 4, -5), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        // Two walls meeting: wall A along x at z=-6 (its north face at z=-7), wall B along z at x=22 (its east face at
        // x=23). A run along A turns the inside corner onto B and ends at a lamp.
        for (int y = 0; y <= 3; y++) {
            for (int x = 22; x <= 26; x++) {
                level.setBlock(o.offset(x, y, -6), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
            for (int z = -7; z >= -11; z--) {
                level.setBlock(o.offset(22, y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }
        for (int z = -12; z <= -7; z++) {
            for (int x = 22; x <= 28; x++) {
                level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int x = 25; x >= 23; x--) {
            BlockPos pos = o.offset(x, 0, -7);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, Direction.NORTH), 3);
        }
        for (int z = -8; z >= -10; z--) {
            BlockPos pos = o.offset(23, 0, z);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, Direction.EAST), 3);
        }
        level.setBlock(o.offset(23, 0, -11), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        // A vertical run at x=-3 with a one-tick repeater whose output points up, to a lamp.
        BlockPos below = o.offset(-3, 1, -1);
        level.setBlock(below, WallRedstoneWireBlock.placementState(level, below, Direction.NORTH), 3);
        level.setBlock(o.offset(-3, 2, -1), diode(ModBlocks.WALL_REPEATER.get().defaultBlockState(), Direction.DOWN).setValue(WallRepeaterBlock.DELAY, 1), 3);
        BlockPos above = o.offset(-3, 3, -1);
        level.setBlock(above, WallRedstoneWireBlock.placementState(level, above, Direction.NORTH), 3);
        level.setBlock(o.offset(-3, 4, -1), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        // Rusty's step at x 30..33, z=-3: ground at y=-1, a row of blocks at y=0, dust along its top fed from a
        // redstone block on the row's east end, a repeater on the row's north face at x=31 hanging below the
        // edge with its output down into the ground block, and a lamp set in the ground north of that block.
        for (int x = 29; x <= 34; x++) {
            for (int z = -7; z <= -1; z++) {
                level.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int x = 30; x <= 33; x++) {
            level.setBlock(o.offset(x, 0, -3), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        }
        for (int x = 30; x <= 32; x++) {
            level.setBlock(o.offset(x, 1, -3), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        }
        level.setBlock(o.offset(31, 0, -4), diode(ModBlocks.WALL_REPEATER.get().defaultBlockState(), Direction.UP).setValue(WallRepeaterBlock.DELAY, 1), 3);
        level.setBlock(o.offset(31, -1, -5), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        // The sources, last.
        level.setBlock(o.offset(33, 1, -3), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(o.offset(-3, 0, -1), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(o.offset(2, 0, -6), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(o.offset(5, 2, -1), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        for (BlockPos foot : new BlockPos[] {o.offset(17, -1, -9), o.offset(19, -1, -7), o.offset(17, -1, -5), o.offset(15, -1, -7)}) {
            level.setBlock(foot, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        }
        level.setBlock(o.offset(-6, 0, -8), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(o.offset(26, 0, -7), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
    }

    /** An L on the face pointing {@code normal}: three up from {@code foot}, then two along {@code along}. */
    private static void pillarCorner(ServerLevel level, BlockPos foot, Direction normal, Direction along) {
        for (int y = 0; y <= 2; y++) {
            BlockPos pos = foot.above(y);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, normal), 3);
        }
        for (int step = 1; step <= 1; step++) {
            BlockPos pos = foot.above(2).relative(along, step);
            level.setBlock(pos, WallRedstoneWireBlock.placementState(level, pos, normal), 3);
        }
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
