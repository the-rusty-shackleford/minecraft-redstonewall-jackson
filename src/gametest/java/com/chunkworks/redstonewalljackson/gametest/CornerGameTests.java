/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import static com.chunkworks.redstonewalljackson.gametest.Rig.dust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.floorDust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.power;
import static com.chunkworks.redstonewalljackson.gametest.Rig.stone;

import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Corners, reported by Rusty on the first evening ("seems like wall redstone does not
 * corner"). Partitions: an L on one wall, up then along, placed in both orders; the inside
 * corner where a floor run turns up a wall from the dust at the wall's foot, power up and
 * power down (vanilla's dust below, through the mixin); the inside corner where a run along
 * one wall turns onto the wall it meets, the corner cell on either wall; the outside corner
 * round a pillar, where the two runs are diagonal neighbours. Each checks the joints drawn,
 * the inside corners' being vanilla's climb up the face of the block beside, and the power
 * arriving past the corner.
 */
@GameTestHolder("redstonewalljackson")
@PrefixGameTestTemplate(false)
public final class CornerGameTests {
    public CornerGameTests() {}

    @GameTest(template = "wall", timeoutTicks = 60)
    public void anLOnOneWallCornersUpThenAlong(GameTestHelper h) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) {
                stone(h, x, y, 6);
            }
        }
        // Up the wall at x=2 from y=1 to y=3, then along it at y=3 from x=3 to x=6: placed in that order.
        for (int y = 1; y <= 3; y++) {
            dust(h, new BlockPos(2, y, 5), Direction.NORTH);
        }
        for (int x = 3; x <= 6; x++) {
            dust(h, new BlockPos(x, 3, 5), Direction.NORTH);
        }
        h.setBlock(new BlockPos(2, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            BlockState corner = h.getBlockState(new BlockPos(2, 3, 5));
            // On a north wall LEFT is east and RIGHT is west: the run continues east, the LEFT joint.
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "the corner reaches down the run it came from");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "and along the run it turns into");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.NONE, "not up");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.NONE, "not the other way");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 5)), 13, "power at the corner");
            h.assertValueEqual(power(h, new BlockPos(6, 3, 5)), 9, "power at the end of the run past the corner");
            BlockState along = h.getBlockState(new BlockPos(4, 3, 5));
            h.assertValueEqual(along.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "the run along is a line");
            h.assertValueEqual(along.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.SIDE, "both ways");
            h.assertValueEqual(along.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.NONE, "and not up");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void anLOnOneWallCornersWhenTheAlongRunIsPlacedFirst(GameTestHelper h) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) {
                stone(h, x, y, 6);
            }
        }
        for (int x = 6; x >= 3; x--) {
            dust(h, new BlockPos(x, 3, 5), Direction.NORTH);
        }
        for (int y = 3; y >= 1; y--) {
            dust(h, new BlockPos(2, y, 5), Direction.NORTH);
        }
        h.setBlock(new BlockPos(2, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            BlockState corner = h.getBlockState(new BlockPos(2, 3, 5));
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "down");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "along");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.NONE, "not up");
            h.assertValueEqual(power(h, new BlockPos(6, 3, 5)), 9, "power past the corner");
            h.succeed();
        });
    }

    /** A floor at y=0 from z=1 to z=5 against a wall at z=6: a floor run to the wall's foot, then dust up the wall above it. */
    private static void floorToWall(GameTestHelper h) {
        for (int z = 1; z <= 5; z++) {
            stone(h, 3, 0, z);
        }
        for (int y = 1; y <= 4; y++) {
            stone(h, 3, y, 6);
        }
        for (int z = 2; z <= 5; z++) {
            floorDust(h, new BlockPos(3, 1, z));
        }
        for (int y = 2; y <= 4; y++) {
            dust(h, new BlockPos(3, y, 5), Direction.NORTH);
        }
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aFloorRunTurnsUpAWallFromTheDustAtItsFoot(GameTestHelper h) {
        floorToWall(h);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 1, 2)), 15, "beside the source");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 12, "the floor dust at the wall's foot");
            h.assertValueEqual(power(h, new BlockPos(3, 2, 5)), 11, "the wall dust above it, one less");
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 9, "and up the wall");
            BlockState foot = h.getBlockState(new BlockPos(3, 1, 5));
            h.assertValueEqual(foot.getValue(RedStoneWireBlock.SOUTH), RedstoneSide.UP, "the floor dust climbs the wall block's face to the wall dust");
            h.assertValueEqual(foot.getValue(RedStoneWireBlock.NORTH), RedstoneSide.SIDE, "from the run it came along");
            h.assertValueEqual(foot.getValue(RedStoneWireBlock.EAST), RedstoneSide.NONE, "and nowhere else");
            BlockState lowest = h.getBlockState(new BlockPos(3, 2, 5));
            h.assertValueEqual(lowest.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "the wall dust reaches down to it");
            h.assertValueEqual(lowest.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "and up the wall");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aWallRunTurnsDownOntoTheFloorDustAtItsFoot(GameTestHelper h) {
        floorToWall(h);
        h.setBlock(new BlockPos(3, 5, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 15, "the top wall dust beside the source");
            h.assertValueEqual(power(h, new BlockPos(3, 2, 5)), 13, "down the wall");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 12, "the floor dust at the foot takes it one less");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 2)), 9, "and carries it away");
            h.assertValueEqual(h.getBlockState(new BlockPos(3, 1, 5)).getValue(RedStoneWireBlock.SOUTH), RedstoneSide.UP, "drawn as the climb");
            h.succeed();
        });
    }

    /** Wall A along x at z=6 faces north; wall B along z at x=0 faces east; they meet at the corner cell (1, 2, 5). */
    private static void twoWalls(GameTestHelper h) {
        for (int x = 0; x <= 6; x++) {
            stone(h, x, 2, 6);
        }
        for (int z = 0; z <= 6; z++) {
            stone(h, 0, 2, z);
        }
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aRunTurnsTheInsideCornerOntoTheWallItMeets(GameTestHelper h) {
        twoWalls(h);
        for (int x = 6; x >= 2; x--) {
            dust(h, new BlockPos(x, 2, 5), Direction.NORTH);   // along wall A toward the corner
        }
        dust(h, new BlockPos(1, 2, 5), Direction.NORTH);       // the corner cell, on wall A
        for (int z = 4; z >= 1; z--) {
            dust(h, new BlockPos(1, 2, z), Direction.EAST);    // along wall B away from the corner
        }
        h.setBlock(new BlockPos(7, 2, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(2, 2, 5)), 11, "along wall A");
            h.assertValueEqual(power(h, new BlockPos(1, 2, 5)), 10, "at the corner");
            h.assertValueEqual(power(h, new BlockPos(1, 2, 4)), 9, "first on wall B");
            h.assertValueEqual(power(h, new BlockPos(1, 2, 1)), 6, "end of wall B");
            BlockState corner = h.getBlockState(new BlockPos(1, 2, 5));
            // On a north wall LEFT is east and RIGHT is west: the run arrives from the east and climbs wall B's face to the west.
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "the corner reaches back along wall A");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.UP, "and climbs wall B's face to the run there");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.NONE, "not up");
            BlockState first = h.getBlockState(new BlockPos(1, 2, 4));
            // On an east wall LEFT is south and RIGHT is north: the corner is to the south, the LEFT joint.
            h.assertValueEqual(first.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "wall B's run reaches back to the corner cell");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aRunTurnsTheInsideCornerWithTheCornerCellOnTheWallItMeets(GameTestHelper h) {
        twoWalls(h);
        for (int x = 6; x >= 2; x--) {
            dust(h, new BlockPos(x, 2, 5), Direction.NORTH);   // along wall A toward the corner
        }
        for (int z = 5; z >= 1; z--) {
            dust(h, new BlockPos(1, 2, z), Direction.EAST);    // the corner cell and the run away, all on wall B
        }
        h.setBlock(new BlockPos(7, 2, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(2, 2, 5)), 11, "along wall A");
            h.assertValueEqual(power(h, new BlockPos(1, 2, 5)), 10, "at the corner");
            h.assertValueEqual(power(h, new BlockPos(1, 2, 1)), 6, "end of wall B");
            BlockState corner = h.getBlockState(new BlockPos(1, 2, 5));
            // On an east wall LEFT is south: the corner cell climbs wall A's face to the run there and reaches north along wall B.
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.UP, "the corner climbs wall A's face to the run there");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.SIDE, "and reaches along wall B");
            BlockState last = h.getBlockState(new BlockPos(2, 2, 5));
            h.assertValueEqual(last.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.SIDE, "wall A's run reaches the corner cell beside it");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aRunTurnsTheOutsideCornerRoundAPillar(GameTestHelper h) {
        // A pillar of stone at (5, 2, 6): dust on its north face at (5, 2, 5) and on its west face at (4, 2, 6).
        for (int x = 5; x <= 8; x++) {
            stone(h, x, 2, 6);
        }
        for (int z = 7; z <= 10; z++) {
            stone(h, 5, 2, z);
        }
        for (int x = 8; x >= 5; x--) {
            dust(h, new BlockPos(x, 2, 5), Direction.NORTH);   // along the north face toward the corner
        }
        for (int z = 6; z <= 10; z++) {
            dust(h, new BlockPos(4, 2, z), Direction.WEST);    // along the west face away from it
        }
        h.setBlock(new BlockPos(9, 2, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(5, 2, 5)), 12, "at the corner on the north face");
            h.assertValueEqual(power(h, new BlockPos(4, 2, 6)), 11, "round the corner on the west face");
            h.assertValueEqual(power(h, new BlockPos(4, 2, 10)), 7, "to the end");
            BlockState north = h.getBlockState(new BlockPos(5, 2, 5));
            h.assertValueEqual(north.getValue(WallRedstoneWireBlock.RIGHT), RedstoneSide.SIDE, "the north-face run reaches west, round the edge");
            BlockState west = h.getBlockState(new BlockPos(4, 2, 6));
            h.assertValueEqual(west.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "the west-face run reaches north, round the edge");
            h.succeed();
        });
    }
}
