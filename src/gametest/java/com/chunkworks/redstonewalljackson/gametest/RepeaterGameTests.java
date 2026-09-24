/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import static com.chunkworks.redstonewalljackson.gametest.Rig.dust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.floorDust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.power;
import static com.chunkworks.redstonewalljackson.gametest.Rig.repeater;
import static com.chunkworks.redstonewalljackson.gametest.Rig.stone;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
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
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A wall repeater fed the ways a player feeds one, after Rusty's "the wall mounted redstone
 * repeater doesn't seem to be powered properly". Partitions: placed by the item with the
 * placer looking up, down, east and west along a north wall, each then fed from behind by
 * wall dust and read in front by wall dust and a lamp; fed from below by vanilla's floor
 * dust at the wall's foot; fed from behind by a solid block that wall dust points into;
 * output into a solid block that wall dust reads beyond. Every case checks the repeater's
 * POWERED and the power past it.
 *
 * <p>Walls stand at z=6 with their dust on the north face (z=5, facing north).
 */
@GameTestHolder("redstonewalljackson")
@PrefixGameTestTemplate(false)
public final class RepeaterGameTests {
    public RepeaterGameTests() {}

    private static final Direction NORTH = Direction.NORTH;

    /** A survival mock player at {@code stand} facing south at the wall, looking along {@code look} (pitch up is negative). */
    private static ServerPlayer placer(GameTestHelper h, BlockPos stand, float yaw, float pitch) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(stand));
        player.moveTo(at.x, at.y, at.z, yaw, pitch);
        player.setYHeadRot(yaw);
        return player;
    }

    /** Clicks the north face of the wall block at {@code wall} with a repeater in hand. */
    private static void clickWithRepeater(GameTestHelper h, ServerPlayer player, BlockPos wall) {
        ItemStack stack = new ItemStack(Items.REPEATER, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(wall);
        Vec3 hit = new Vec3(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ());
        player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, NORTH, abs, false));
        h.assertValueEqual(stack.getCount(), 3, "one repeater spent");
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterPlacedLookingUpTheWallTakesInputBelowAndPowersAbove(GameTestHelper h) {
        for (int y = 1; y <= 4; y++) {
            stone(h, 3, y, 6);
        }
        stone(h, 3, 0, 4);   // to stand on
        ServerPlayer player = placer(h, new BlockPos(3, 1, 4), 0.0F, -35.0F);   // facing south, looking up
        clickWithRepeater(h, player, new BlockPos(3, 2, 6));
        BlockState placed = h.getBlockState(new BlockPos(3, 2, 5));
        h.assertTrue(placed.is(ModBlocks.WALL_REPEATER.get()), "a wall repeater");
        h.assertValueEqual(placed.getValue(WallDiodeBlock.FACING), Direction.DOWN, "its input below, its output above where the placer looks");
        dust(h, new BlockPos(3, 1, 5), NORTH);
        dust(h, new BlockPos(3, 3, 5), NORTH);
        h.setBlock(new BlockPos(3, 4, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(3, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertBlockProperty(new BlockPos(3, 2, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(3, 3, 5)), 15, "full power above the repeater");
            h.assertBlockProperty(new BlockPos(3, 4, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterPlacedLookingDownTheWallTakesInputAboveAndPowersBelow(GameTestHelper h) {
        for (int y = 1; y <= 5; y++) {
            stone(h, 3, y, 6);
        }
        for (int y = 0; y <= 2; y++) {
            stone(h, 3, y, 4);   // a stack to stand on, level with the repeater's cell
        }
        ServerPlayer player = placer(h, new BlockPos(3, 3, 4), 0.0F, 35.0F);   // looking down
        clickWithRepeater(h, player, new BlockPos(3, 3, 6));
        BlockState placed = h.getBlockState(new BlockPos(3, 3, 5));
        h.assertTrue(placed.is(ModBlocks.WALL_REPEATER.get()), "a wall repeater");
        h.assertValueEqual(placed.getValue(WallDiodeBlock.FACING), Direction.UP, "its input above, its output below");
        dust(h, new BlockPos(3, 4, 5), NORTH);
        dust(h, new BlockPos(3, 2, 5), NORTH);
        h.setBlock(new BlockPos(3, 1, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(3, 5, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertBlockProperty(new BlockPos(3, 3, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(3, 2, 5)), 15, "full power below the repeater");
            h.assertBlockProperty(new BlockPos(3, 1, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterPlacedLookingAlongTheWallTakesInputBehindAndPowersAhead(GameTestHelper h) {
        for (int x = 1; x <= 7; x++) {
            stone(h, x, 2, 6);
        }
        stone(h, 4, 0, 4);
        // Looking east along the wall (yaw -90 is east), a little up at the block: the output goes east.
        ServerPlayer player = placer(h, new BlockPos(4, 1, 4), -60.0F, -20.0F);
        clickWithRepeater(h, player, new BlockPos(4, 2, 6));
        BlockState placed = h.getBlockState(new BlockPos(4, 2, 5));
        h.assertTrue(placed.is(ModBlocks.WALL_REPEATER.get()), "a wall repeater");
        h.assertValueEqual(placed.getValue(WallDiodeBlock.FACING), Direction.WEST, "its input from the west, its output east where the placer looks");
        dust(h, new BlockPos(3, 2, 5), NORTH);
        dust(h, new BlockPos(5, 2, 5), NORTH);
        h.setBlock(new BlockPos(6, 2, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(2, 2, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertBlockProperty(new BlockPos(4, 2, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(5, 2, 5)), 15, "full power past the repeater");
            h.assertBlockProperty(new BlockPos(6, 2, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterAboveTheFloorDustAtTheWallsFootIsFedByIt(GameTestHelper h) {
        for (int z = 1; z <= 5; z++) {
            stone(h, 3, 0, z);
        }
        for (int y = 1; y <= 4; y++) {
            stone(h, 3, y, 6);
        }
        for (int z = 2; z <= 5; z++) {
            floorDust(h, new BlockPos(3, 1, z));
        }
        h.setBlock(new BlockPos(3, 2, 5), repeater(NORTH, Direction.DOWN, 1));   // input from the floor dust below it
        dust(h, new BlockPos(3, 3, 5), NORTH);
        h.setBlock(new BlockPos(3, 4, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(3, 1, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 12, "the floor dust at the foot");
            h.assertBlockProperty(new BlockPos(3, 2, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(3, 3, 5)), 15, "full power above the repeater");
            h.assertBlockProperty(new BlockPos(3, 4, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterReadsASolidBlockBehindItThatWallDustPointsInto(GameTestHelper h) {
        for (int y = 1; y <= 5; y++) {
            stone(h, 3, y, 6);
        }
        // Up the wall: dust at y=1 pointing up into a stone at y=2 (its cell in front of the wall), the repeater at y=3
        // reading that stone from below, dust at y=4, a lamp at y=5.
        dust(h, new BlockPos(3, 1, 5), NORTH);
        h.setBlock(new BlockPos(3, 2, 5), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(3, 3, 5), repeater(NORTH, Direction.DOWN, 1));
        dust(h, new BlockPos(3, 4, 5), NORTH);
        h.setBlock(new BlockPos(3, 5, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(3, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 15, "the dust beside the source");
            h.assertBlockProperty(new BlockPos(3, 3, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 15, "full power past the repeater");
            h.assertBlockProperty(new BlockPos(3, 5, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aRepeaterPowersASolidBlockAheadThatWallDustReadsBeyond(GameTestHelper h) {
        for (int y = 1; y <= 5; y++) {
            stone(h, 3, y, 6);
        }
        dust(h, new BlockPos(3, 1, 5), NORTH);
        h.setBlock(new BlockPos(3, 2, 5), repeater(NORTH, Direction.DOWN, 1));
        h.setBlock(new BlockPos(3, 3, 5), Blocks.STONE.defaultBlockState());   // strongly powered by the repeater
        dust(h, new BlockPos(3, 4, 5), NORTH);
        h.setBlock(new BlockPos(3, 5, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(3, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertBlockProperty(new BlockPos(3, 2, 5), WallDiodeBlock.POWERED, true);
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 15, "the dust beyond the strongly powered block");
            h.assertBlockProperty(new BlockPos(3, 5, 5), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }
}
