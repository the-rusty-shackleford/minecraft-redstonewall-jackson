/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import static com.chunkworks.redstonewalljackson.gametest.Rig.dust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.floorDust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.power;
import static com.chunkworks.redstonewalljackson.gametest.Rig.repeater;
import static com.chunkworks.redstonewalljackson.gametest.Rig.stone;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Redstone on the underside of blocks, asked for by Rusty with the corners ("it should also
 * work on the underside of blocks, and corner upwards/downwards as well"). Partitions: a
 * ceiling run against the floor run it is a turned copy of, power and every joint block by
 * block, both lighting a lamp; the corner up from a wall onto the ceiling, with the ceiling
 * run in front of the top wall dust and with it directly above the wall run, and the same
 * corner down from the ceiling onto the wall; the outside corner from a ceiling run down the
 * side of the block it hangs from; a ceiling repeater's delay at one and four ticks; a click on
 * the underside of a block placing the ceiling forms and spending the item.
 *
 * <p>Ceilings are stone at y=4 with their dust at y=3, facing down; on a ceiling TOP is north,
 * BOTTOM south, LEFT east and RIGHT west.
 */
@GameTestHolder("redstonewalljackson")
@PrefixGameTestTemplate(false)
public final class CeilingGameTests {
    public CeilingGameTests() {}

    private static final Direction DOWN = Direction.DOWN;

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aCeilingRunIsTheFloorRunTurnedOver(GameTestHelper h) {
        for (int x = 1; x <= 7; x++) {
            stone(h, x, 0, 2);
            floorDust(h, new BlockPos(x, 1, 2));
        }
        stone(h, 8, 0, 2);
        h.setBlock(new BlockPos(8, 1, 2), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int x = 1; x <= 7; x++) {
            stone(h, x, 4, 5);
            dust(h, new BlockPos(x, 3, 5), DOWN);
        }
        h.setBlock(new BlockPos(8, 3, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(0, 3, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            for (int x = 1; x <= 7; x++) {
                BlockPos floor = new BlockPos(x, 1, 2), ceiling = new BlockPos(x, 3, 5);
                h.assertValueEqual(power(h, ceiling), power(h, floor), "power at x=" + x);
                h.assertValueEqual(power(h, floor), 16 - x, "vanilla's falloff at x=" + x);
                BlockState f = h.getBlockState(floor), c = h.getBlockState(ceiling);
                h.assertValueEqual(c.getValue(WallRedstoneWireBlock.FACING), DOWN, "facing down at x=" + x);
                h.assertValueEqual(c.getValue(WallRedstoneWireBlock.LEFT), f.getValue(RedStoneWireBlock.EAST), "east joint at x=" + x);
                h.assertValueEqual(c.getValue(WallRedstoneWireBlock.RIGHT), f.getValue(RedStoneWireBlock.WEST), "west joint at x=" + x);
                h.assertValueEqual(c.getValue(WallRedstoneWireBlock.TOP), f.getValue(RedStoneWireBlock.NORTH), "north joint at x=" + x);
                h.assertValueEqual(c.getValue(WallRedstoneWireBlock.BOTTOM), f.getValue(RedStoneWireBlock.SOUTH), "south joint at x=" + x);
            }
            h.assertBlockProperty(new BlockPos(8, 1, 2), RedstoneLampBlock.LIT, true);
            h.assertBlockProperty(new BlockPos(8, 3, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    /**
     * Two rigs of a wall at z=6 under a ceiling reaching from z=1 to z=5. At x=2 the wall dust runs to y=3, under
     * the ceiling, and the ceiling run starts in front of its top; at x=7 the wall dust stops at y=2 and the ceiling
     * run starts directly above it, against the wall.
     */
    private static void wallToCeiling(GameTestHelper h) {
        for (int x : new int[] {2, 7}) {
            for (int y = 1; y <= 3; y++) {
                stone(h, x, y, 6);
            }
            for (int z = 1; z <= 5; z++) {
                stone(h, x, 4, z);
            }
        }
        for (int y = 1; y <= 3; y++) {
            dust(h, new BlockPos(2, y, 5), Direction.NORTH);
        }
        for (int z = 4; z >= 1; z--) {
            dust(h, new BlockPos(2, 3, z), DOWN);
        }
        for (int y = 1; y <= 2; y++) {
            dust(h, new BlockPos(7, y, 5), Direction.NORTH);
        }
        for (int z = 5; z >= 1; z--) {
            dust(h, new BlockPos(7, 3, z), DOWN);
        }
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aWallRunTurnsUpOntoTheCeiling(GameTestHelper h) {
        wallToCeiling(h);
        h.setBlock(new BlockPos(2, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.setBlock(new BlockPos(7, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            // The ceiling run in front of the top wall dust.
            h.assertValueEqual(power(h, new BlockPos(2, 3, 5)), 13, "the top wall dust");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 4)), 12, "the ceiling dust in front of it, one less");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 1)), 9, "along the ceiling");
            BlockState top = h.getBlockState(new BlockPos(2, 3, 5));
            h.assertValueEqual(top.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.UP, "the wall dust climbs the ceiling block's face to the ceiling run");
            h.assertValueEqual(top.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "from the run below");
            BlockState first = h.getBlockState(new BlockPos(2, 3, 4));
            h.assertValueEqual(first.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "the ceiling run reaches south to the wall dust beside it");
            h.assertValueEqual(first.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "and on north");
            h.assertValueEqual(first.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.NONE, "not sideways");
            // The ceiling run directly above the wall run.
            h.assertValueEqual(power(h, new BlockPos(7, 2, 5)), 14, "the top wall dust");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 13, "the ceiling dust above it, one less");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 1)), 9, "along the ceiling");
            BlockState corner = h.getBlockState(new BlockPos(7, 3, 5));
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.UP, "the ceiling dust climbs the wall's face down to the wall run");
            h.assertValueEqual(corner.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "and reaches north");
            h.assertValueEqual(h.getBlockState(new BlockPos(7, 2, 5)).getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "the wall dust reaches up to it");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aCeilingRunTurnsDownOntoAWall(GameTestHelper h) {
        wallToCeiling(h);
        h.setBlock(new BlockPos(2, 3, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.setBlock(new BlockPos(7, 3, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(2, 3, 1)), 15, "beside the source on the ceiling");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 4)), 12, "the last ceiling dust");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 5)), 11, "the wall dust behind it takes it one less");
            h.assertValueEqual(power(h, new BlockPos(2, 1, 5)), 9, "and down the wall");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 11, "the ceiling dust against the wall");
            h.assertValueEqual(power(h, new BlockPos(7, 2, 5)), 10, "the wall dust below it takes it one less");
            h.assertValueEqual(power(h, new BlockPos(7, 1, 5)), 9, "and down the wall");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aCeilingRunTurnsTheOutsideCornerDownTheSideOfTheBlockItHangsFrom(GameTestHelper h) {
        for (int x = 2; x <= 5; x++) {
            stone(h, x, 3, 2);
            dust(h, new BlockPos(x, 2, 2), DOWN);
        }
        dust(h, new BlockPos(6, 3, 2), Direction.EAST);   // on the east face of the last ceiling block
        h.setBlock(new BlockPos(6, 4, 2), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(1, 2, 2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(5, 2, 2)), 12, "the last ceiling dust");
            h.assertValueEqual(power(h, new BlockPos(6, 3, 2)), 11, "round the edge, one less");
            h.assertValueEqual(h.getBlockState(new BlockPos(5, 2, 2)).getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.SIDE, "the ceiling run reaches east to the edge");
            h.assertValueEqual(h.getBlockState(new BlockPos(6, 3, 2)).getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "the wall dust reaches down to the edge");
            h.assertBlockProperty(new BlockPos(6, 4, 2), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aCeilingRepeaterDelaysAndOutputsAlongTheCeiling(GameTestHelper h) {
        for (int z : new int[] {2, 6}) {
            for (int x = 2; x <= 4; x++) {
                stone(h, x, 4, z);
            }
            dust(h, new BlockPos(2, 3, z), DOWN);
            h.setBlock(new BlockPos(3, 3, z), repeater(DOWN, Direction.WEST, z == 2 ? 1 : 4));   // input from the west
            dust(h, new BlockPos(4, 3, z), DOWN);
        }
        h.setBlock(new BlockPos(1, 3, 2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.setBlock(new BlockPos(1, 3, 6), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAtTickTime(1, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 3, 2)), 0, "nothing past the one-tick repeater yet");
            h.assertValueEqual(power(h, new BlockPos(4, 3, 6)), 0, "nor past the four-tick one");
        });
        h.runAtTickTime(4, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 3, 2)), 15, "the one-tick repeater has passed full power along the ceiling");
            h.assertValueEqual(power(h, new BlockPos(4, 3, 6)), 0, "the four-tick one is still counting");
            h.assertBlockProperty(new BlockPos(3, 3, 2), WallDiodeBlock.POWERED, true);
        });
        h.runAtTickTime(12, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 3, 6)), 15, "the four-tick repeater has passed it");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 40)
    public void aClickOnTheUndersideOfABlockPlacesTheCeilingForms(GameTestHelper h) {
        for (int x = 2; x <= 4; x++) {
            stone(h, x, 4, 2);
        }
        stone(h, 3, 0, 2);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);   // a creative click puts the item back
        Vec3 stand = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 2)));
        player.moveTo(stand.x, stand.y, stand.z, 0.0F, -70.0F);   // facing south, looking up at the ceiling
        player.setYHeadRot(0.0F);
        BlockPos dust = new BlockPos(2, 4, 2), repeater = new BlockPos(3, 4, 2), comparator = new BlockPos(4, 4, 2);
        for (BlockPos target : new BlockPos[] {dust, repeater, comparator}) {
            ItemStack stack = new ItemStack(target == dust ? Items.REDSTONE : target == repeater ? Items.REPEATER : Items.COMPARATOR, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos abs = h.absolutePos(target);
            Vec3 hit = new Vec3(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);   // the underside
            player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, DOWN, abs, false));
            h.assertValueEqual(stack.getCount(), 7, "one spent from " + stack.getItem());
        }
        BlockState placedDust = h.getBlockState(dust.below());
        h.assertTrue(placedDust.is(ModBlocks.WALL_REDSTONE_WIRE.get()), "dust on the ceiling");
        h.assertValueEqual(placedDust.getValue(WallRedstoneWireBlock.FACING), DOWN, "facing down");
        BlockState placedRepeater = h.getBlockState(repeater.below());
        h.assertTrue(placedRepeater.is(ModBlocks.WALL_REPEATER.get()), "a repeater on the ceiling");
        h.assertValueEqual(placedRepeater.getValue(WallDiodeBlock.WALL), DOWN, "hanging from it");
        h.assertValueEqual(placedRepeater.getValue(WallDiodeBlock.FACING), Direction.NORTH, "its output where the placer looks: south");
        BlockState placedComparator = h.getBlockState(comparator.below());
        h.assertTrue(placedComparator.is(ModBlocks.WALL_COMPARATOR.get()), "a comparator on the ceiling");
        h.assertValueEqual(placedComparator.getValue(WallDiodeBlock.WALL), DOWN, "hanging from it");
        h.succeed();
    }
}
