/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import static com.chunkworks.redstonewalljackson.gametest.Rig.dust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.floorDust;
import static com.chunkworks.redstonewalljackson.gametest.Rig.power;
import static com.chunkworks.redstonewalljackson.gametest.Rig.repeater;
import static com.chunkworks.redstonewalljackson.gametest.Rig.stone;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.WallComparatorBlock;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A wall diode reads the block it hangs on (D-0003), after Rusty's build on the box: dust
 * along the top of a block row, a repeater on the row's face just below the edge, off. Their
 * geometry to the block: a floor run climbing onto a raised block, the repeater hanging on that
 * block's far face with its output down into the ground, a lamp beside the ground block.
 * Partitions: dust on top of the block (on); wall dust pointing into the block from another
 * face (on, vanilla's strong power crossing a block); a torch standing on the block (off, a
 * torch never powers what it is attached to, on walls as on floors); a comparator hanging on
 * a powered block (on); the diode going off again when the dust is taken away.
 */
@GameTestHolder("redstonewalljackson")
@PrefixGameTestTemplate(false)
public final class SupportGameTests {
    public SupportGameTests() {}

    private static final Direction NORTH = Direction.NORTH;

    /** Ground at y=0 from z=1 to z=6 along x=3; the raised block at (3, 1, 3); the repeater on its north face at (3, 1, 2), output down. */
    private static void rustysStep(GameTestHelper h) {
        for (int z = 1; z <= 6; z++) {
            stone(h, 3, 0, z);
        }
        stone(h, 3, 1, 3);
        h.setBlock(new BlockPos(3, 1, 2), repeater(NORTH, Direction.UP, 1));
        h.setBlock(new BlockPos(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState());   // beside the ground block the repeater powers
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void dustOnTopOfTheBlockARepeaterHangsOnFeedsIt(GameTestHelper h) {
        rustysStep(h);
        floorDust(h, new BlockPos(3, 1, 5));
        floorDust(h, new BlockPos(3, 1, 4));
        floorDust(h, new BlockPos(3, 2, 3));   // on top of the raised block, up the step
        h.setBlock(new BlockPos(3, 1, 6), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 2, 3)), 13, "the dust on top of the block");
            h.assertBlockProperty(new BlockPos(3, 1, 2), WallDiodeBlock.POWERED, true);
            h.assertBlockProperty(new BlockPos(3, 0, 1), RedstoneLampBlock.LIT, true);
            h.setBlock(new BlockPos(3, 1, 6), Blocks.AIR.defaultBlockState());   // and off again
        });
        h.runAfterDelay(14, () -> {
            h.assertBlockProperty(new BlockPos(3, 1, 2), WallDiodeBlock.POWERED, false);
            h.assertBlockProperty(new BlockPos(3, 0, 1), RedstoneLampBlock.LIT, false);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void wallDustPointingIntoTheBlockFromAnotherFaceFeedsTheRepeaterOnIt(GameTestHelper h) {
        rustysStep(h);
        // Wall dust on the raised block's east face, powered from a redstone block beside it along that face.
        dust(h, new BlockPos(4, 1, 3), Direction.EAST);
        h.setBlock(new BlockPos(4, 1, 4), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 1, 3)), 15, "the wall dust on the east face");
            h.assertBlockProperty(new BlockPos(3, 1, 2), WallDiodeBlock.POWERED, true);
            h.assertBlockProperty(new BlockPos(3, 0, 1), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aTorchStandingOnTheBlockDoesNotFeedTheRepeaterHangingOnIt(GameTestHelper h) {
        rustysStep(h);
        h.setBlock(new BlockPos(3, 2, 3), Blocks.REDSTONE_TORCH.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertBlockProperty(new BlockPos(3, 1, 2), WallDiodeBlock.POWERED, false);
            h.assertBlockProperty(new BlockPos(3, 0, 1), RedstoneLampBlock.LIT, false);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aComparatorHangingOnAPoweredBlockPassesItsPower(GameTestHelper h) {
        for (int z = 1; z <= 6; z++) {
            stone(h, 3, 0, z);
        }
        stone(h, 3, 1, 3);
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.WALL_COMPARATOR.get().defaultBlockState()
                .setValue(WallDiodeBlock.WALL, NORTH).setValue(WallDiodeBlock.FACING, Direction.UP).setValue(WallComparatorBlock.MODE, ComparatorMode.COMPARE));
        h.setBlock(new BlockPos(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState());
        floorDust(h, new BlockPos(3, 1, 5));
        floorDust(h, new BlockPos(3, 1, 4));
        floorDust(h, new BlockPos(3, 2, 3));
        h.setBlock(new BlockPos(3, 1, 6), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(8, () -> {
            h.assertBlockProperty(new BlockPos(3, 1, 2), WallDiodeBlock.POWERED, true);
            h.assertBlockProperty(new BlockPos(3, 0, 1), RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }
}
