/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.gametest;

import com.chunkworks.redstonewalljackson.ModBlocks;
import com.chunkworks.redstonewalljackson.WallComparatorBlock;
import com.chunkworks.redstonewalljackson.WallDiodeBlock;
import com.chunkworks.redstonewalljackson.WallRedstoneWireBlock;
import com.chunkworks.redstonewalljackson.WallRepeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wall redstone on a real server. Partitions: a wall run against the floor run it is a turned
 * copy of, power and shape block by block, both lighting a lamp; power falling by one a step
 * up a wall; the seam up from a floor run onto a wall, down from a wall onto a floor run, and
 * from a wall onto a floor run along the wall's top; a wall repeater's delay at one and four
 * ticks; a wall repeater locked by a sideways wall repeater; a wall comparator reading a chest
 * behind it along the wall, comparing and subtracting a side comparator; the wall block going
 * takes the dust with it and drops the item; a click on a wall face with dust, a repeater and a
 * comparator places the wall forms and spends the item, a click on a floor places vanilla's;
 * the wall forms pick as the vanilla items.
 *
 * <p>Walls stand at z=6 with their dust on the north face (z=5, facing north); floors are at
 * y=0 with their dust at y=1.
 */
@GameTestHolder("redstonewalljackson")
@PrefixGameTestTemplate(false)
public final class WallGameTests {
    public WallGameTests() {}

    private static final Direction NORTH = Direction.NORTH;

    private static void wall(GameTestHelper h, int x, int y) {
        h.setBlock(new BlockPos(x, y, 6), Blocks.STONE.defaultBlockState());
    }

    private static void wallDust(GameTestHelper h, int x, int y) {
        BlockPos pos = new BlockPos(x, y, 5);
        h.setBlock(pos, WallRedstoneWireBlock.placementState(h.getLevel(), h.absolutePos(pos), NORTH));
    }

    private static void floorDust(GameTestHelper h, int x, int z) {
        h.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
        floorDust(h, new BlockPos(x, 1, z));
    }

    /** Vanilla's dust at {@code pos}, placed as the item places it: a cross drawn against its neighbours. */
    private static void floorDust(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, Blocks.REDSTONE_WIRE.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(
                h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE),
                new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos).below(), false))));
    }

    private static int power(GameTestHelper h, BlockPos pos) {
        BlockState s = h.getBlockState(pos);
        return s.getValue(s.is(Blocks.REDSTONE_WIRE) ? RedStoneWireBlock.POWER : WallRedstoneWireBlock.POWER);
    }

    private static BlockState repeater(int wallToward, Direction facing, int delay) {
        return ModBlocks.WALL_REPEATER.get().defaultBlockState().setValue(WallDiodeBlock.WALL, NORTH)
                .setValue(WallDiodeBlock.FACING, facing).setValue(WallRepeaterBlock.DELAY, delay);
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void aWallRunIsTheFloorRunTurned(GameTestHelper h) {
        for (int x = 1; x <= 7; x++) {
            floorDust(h, x, 2);
        }
        h.setBlock(new BlockPos(8, 0, 2), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(8, 1, 2), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int x = 1; x <= 7; x++) {
            wall(h, x, 1);
            wallDust(h, x, 1);
        }
        h.setBlock(new BlockPos(8, 1, 5), Blocks.REDSTONE_LAMP.defaultBlockState());
        h.setBlock(new BlockPos(0, 1, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            for (int x = 1; x <= 7; x++) {
                BlockPos floor = new BlockPos(x, 1, 2), wall = new BlockPos(x, 1, 5);
                h.assertValueEqual(power(h, wall), power(h, floor), "power at x=" + x);
                h.assertValueEqual(power(h, floor), 16 - x, "vanilla's falloff at x=" + x);
                BlockState f = h.getBlockState(floor), w = h.getBlockState(wall);
                // On a north wall LEFT is east and RIGHT is west, TOP up and BOTTOM down.
                h.assertValueEqual(w.getValue(WallRedstoneWireBlock.LEFT), f.getValue(RedStoneWireBlock.EAST), "east joint at x=" + x);
                h.assertValueEqual(w.getValue(WallRedstoneWireBlock.RIGHT), f.getValue(RedStoneWireBlock.WEST), "west joint at x=" + x);
                h.assertValueEqual(w.getValue(WallRedstoneWireBlock.TOP), f.getValue(RedStoneWireBlock.NORTH), "top joint at x=" + x);
                h.assertValueEqual(w.getValue(WallRedstoneWireBlock.BOTTOM), f.getValue(RedStoneWireBlock.SOUTH), "bottom joint at x=" + x);
            }
            h.assertBlockProperty(new BlockPos(8, 1, 2), net.minecraft.world.level.block.RedstoneLampBlock.LIT, true);
            h.assertBlockProperty(new BlockPos(8, 1, 5), net.minecraft.world.level.block.RedstoneLampBlock.LIT, true);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void powerFallsByOneAStepUpTheWall(GameTestHelper h) {
        for (int y = 1; y <= 10; y++) {
            wall(h, 2, y);
        }
        for (int y = 1; y <= 10; y++) {
            wallDust(h, 2, y);
        }
        h.setBlock(new BlockPos(2, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            for (int y = 1; y <= 10; y++) {
                h.assertValueEqual(power(h, new BlockPos(2, y, 5)), 16 - y, "power at y=" + y);
            }
            BlockState mid = h.getBlockState(new BlockPos(2, 5, 5));
            h.assertValueEqual(mid.getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "a line up the wall");
            h.assertValueEqual(mid.getValue(WallRedstoneWireBlock.BOTTOM), RedstoneSide.SIDE, "and down it");
            h.assertValueEqual(mid.getValue(WallRedstoneWireBlock.LEFT), RedstoneSide.NONE, "reaching nothing sideways");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void theSeamCarriesPowerUpFromAFloorRunOntoTheWall(GameTestHelper h) {
        for (int z = 2; z <= 4; z++) {
            floorDust(h, 3, z);
        }
        for (int y = 1; y <= 4; y++) {
            wall(h, 3, y);
        }
        for (int y = 1; y <= 4; y++) {
            wallDust(h, 3, y);
        }
        h.setBlock(new BlockPos(3, 0, 1), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(3, 1, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 1, 4)), 13, "the floor run ends at 13 in front of the wall");
            h.assertValueEqual(h.getBlockState(new BlockPos(3, 1, 4)).getValue(RedStoneWireBlock.SOUTH), RedstoneSide.SIDE, "and reaches toward the wall dust");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 12, "the lowest wall dust takes it one less");
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 9, "and it climbs, losing one a step");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void theSeamCarriesPowerDownFromTheWallOntoAFloorRun(GameTestHelper h) {
        for (int z = 2; z <= 4; z++) {
            floorDust(h, 3, z);
        }
        for (int y = 1; y <= 4; y++) {
            wall(h, 3, y);
        }
        for (int y = 1; y <= 4; y++) {
            wallDust(h, 3, y);
        }
        h.setBlock(new BlockPos(3, 5, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(3, 4, 5)), 15, "the top wall dust sits beside the source");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 5)), 12, "down the wall, one a step");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 4)), 11, "the floor run in front takes it one less");
            h.assertValueEqual(power(h, new BlockPos(3, 1, 2)), 9, "and carries it away");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 60)
    public void theSeamCarriesPowerFromTheWallOntoARunAlongItsTop(GameTestHelper h) {
        for (int y = 1; y <= 3; y++) {
            wall(h, 6, y);
        }
        for (int y = 1; y <= 3; y++) {
            wallDust(h, 6, y);
        }
        BlockPos top = new BlockPos(6, 4, 6);
        floorDust(h, top);
        h.setBlock(new BlockPos(6, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(power(h, new BlockPos(6, 3, 5)), 13, "the top wall dust");
            h.assertValueEqual(power(h, top), 12, "the floor dust on the wall's top takes it one less, a step up and back");
            h.assertValueEqual(h.getBlockState(top).getValue(RedStoneWireBlock.NORTH), RedstoneSide.SIDE, "and reaches down toward it");
            h.assertValueEqual(h.getBlockState(new BlockPos(6, 3, 5)).getValue(WallRedstoneWireBlock.TOP), RedstoneSide.SIDE, "as the wall dust reaches up");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aWallRepeaterDelaysAndOutputsAlongTheWall(GameTestHelper h) {
        for (int x : new int[] {4, 7}) {
            for (int y = 1; y <= 3; y++) {
                wall(h, x, y);
            }
            wallDust(h, x, 1);
            h.setBlock(new BlockPos(x, 2, 5), repeater(0, Direction.DOWN, x == 4 ? 1 : 4));
            wallDust(h, x, 3);
        }
        h.setBlock(new BlockPos(4, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.setBlock(new BlockPos(7, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAtTickTime(1, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 3, 5)), 0, "nothing above the one-tick repeater yet");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 0, "nor above the four-tick one");
        });
        h.runAtTickTime(4, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 3, 5)), 15, "the one-tick repeater has passed full power up the wall");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 0, "the four-tick one is still counting");
            h.assertBlockProperty(new BlockPos(4, 2, 5), WallDiodeBlock.POWERED, true);
        });
        h.runAtTickTime(12, () -> {
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 15, "the four-tick repeater has passed it");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aWallRepeaterIsLockedBySidewaysWallRepeater(GameTestHelper h) {
        for (int x = 4; x <= 6; x++) {
            for (int y = 1; y <= 3; y++) {
                wall(h, x, y);
            }
        }
        wallDust(h, 4, 1);
        h.setBlock(new BlockPos(4, 2, 5), repeater(0, Direction.DOWN, 1));   // the locked one: input below, output above
        wallDust(h, 4, 3);
        h.setBlock(new BlockPos(5, 2, 5), repeater(0, Direction.EAST, 1));   // the lock: input from the east, output west into the first
        h.setBlock(new BlockPos(6, 2, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAtTickTime(6, () -> {
            h.assertBlockProperty(new BlockPos(5, 2, 5), WallDiodeBlock.POWERED, true);
            h.assertBlockProperty(new BlockPos(4, 2, 5), WallRepeaterBlock.LOCKED, true);
            h.setBlock(new BlockPos(4, 0, 5), Blocks.REDSTONE_BLOCK.defaultBlockState());   // now feed the locked one
        });
        h.runAtTickTime(14, () -> {
            h.assertValueEqual(power(h, new BlockPos(4, 1, 5)), 15, "its input is powered");
            h.assertBlockProperty(new BlockPos(4, 2, 5), WallDiodeBlock.POWERED, false);
            h.assertValueEqual(power(h, new BlockPos(4, 3, 5)), 0, "but a locked repeater passes nothing");
            h.succeed();
        });
    }

    /** A chest whose comparator reading is {@code signal}: vanilla's fill formula solved for full stacks. */
    private static void chest(GameTestHelper h, BlockPos pos, int signal) {
        h.setBlock(pos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(pos);
        int stacks = switch (signal) {
            case 10 -> 18;   // floor(14 * 18/27) + 1
            case 4 -> 6;     // floor(14 * 6/27) + 1
            default -> throw new IllegalArgumentException("no stack count prepared for " + signal);
        };
        for (int i = 0; i < stacks; i++) {
            chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    @GameTest(template = "wall", timeoutTicks = 80)
    public void aWallComparatorReadsAChestBehindItAndComparesOrSubtractsASide(GameTestHelper h) {
        for (int x : new int[] {2, 7}) {
            for (int y = 1; y <= 3; y++) {
                wall(h, x, y);
                wall(h, x + 1, y);
                wall(h, x + 2, y);
            }
            ComparatorMode mode = x == 2 ? ComparatorMode.COMPARE : ComparatorMode.SUBTRACT;
            chest(h, new BlockPos(x, 1, 5), 10);                                             // behind the comparator, down the wall
            h.setBlock(new BlockPos(x, 2, 5), ModBlocks.WALL_COMPARATOR.get().defaultBlockState()
                    .setValue(WallDiodeBlock.WALL, NORTH).setValue(WallDiodeBlock.FACING, Direction.DOWN).setValue(WallComparatorBlock.MODE, mode));
            wallDust(h, x, 3);                                                               // its output, up the wall
            chest(h, new BlockPos(x + 2, 2, 5), 4);                                          // the side comparator's chest
            h.setBlock(new BlockPos(x + 1, 2, 5), ModBlocks.WALL_COMPARATOR.get().defaultBlockState()
                    .setValue(WallDiodeBlock.WALL, NORTH).setValue(WallDiodeBlock.FACING, Direction.EAST));   // output west, into the side
        }
        h.runAtTickTime(10, () -> {
            h.assertValueEqual(((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(3, 2, 5))).getOutputSignal(), 4, "the side comparator reads its chest");
            h.assertValueEqual(((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 5))).getOutputSignal(), 10, "compare: ten beats four");
            h.assertValueEqual(power(h, new BlockPos(2, 3, 5)), 10, "and the dust above carries ten");
            h.assertValueEqual(((ComparatorBlockEntity) h.getBlockEntity(new BlockPos(7, 2, 5))).getOutputSignal(), 6, "subtract: ten less four");
            h.assertValueEqual(power(h, new BlockPos(7, 3, 5)), 6, "and the dust above carries six");
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 40)
    public void theWallGoingTakesTheDustWithItAndDropsTheItem(GameTestHelper h) {
        wall(h, 5, 2);
        wallDust(h, 5, 2);
        h.runAtTickTime(2, () -> h.setBlock(new BlockPos(5, 2, 6), Blocks.AIR.defaultBlockState()));
        h.runAtTickTime(6, () -> {
            h.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 5));
            h.assertItemEntityPresent(Items.REDSTONE, new BlockPos(5, 2, 5), 2.0);
            h.succeed();
        });
    }

    @GameTest(template = "wall", timeoutTicks = 40)
    public void aClickOnAWallFacePlacesTheWallFormsAndAClickOnAFloorPlacesVanillas(GameTestHelper h) {
        for (int x = 2; x <= 4; x++) {
            wall(h, x, 2);
        }
        h.setBlock(new BlockPos(6, 0, 2), Blocks.STONE.defaultBlockState());
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);   // a creative click puts the item back
        Vec3 stand = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        player.moveTo(stand.x, stand.y, stand.z, 0.0F, -30.0F);   // facing south at the wall, looking up a little
        player.setYHeadRot(0.0F);
        BlockPos wallDust = new BlockPos(2, 2, 6), wallRepeater = new BlockPos(3, 2, 6), wallComparator = new BlockPos(4, 2, 6);
        for (BlockPos target : new BlockPos[] {wallDust, wallRepeater, wallComparator}) {
            ItemStack stack = new ItemStack(target == wallDust ? Items.REDSTONE : target == wallRepeater ? Items.REPEATER : Items.COMPARATOR, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos abs = h.absolutePos(target);
            Vec3 hit = new Vec3(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ());   // the north face
            player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, NORTH, abs, false));
            h.assertValueEqual(stack.getCount(), 7, "one spent from " + stack.getItem());
        }
        BlockState dust = h.getBlockState(wallDust.north());
        h.assertTrue(dust.is(ModBlocks.WALL_REDSTONE_WIRE.get()), "dust on the wall");
        h.assertValueEqual(dust.getValue(WallRedstoneWireBlock.FACING), NORTH, "facing out of it");
        BlockState repeater = h.getBlockState(wallRepeater.north());
        h.assertTrue(repeater.is(ModBlocks.WALL_REPEATER.get()), "a repeater on the wall");
        h.assertValueEqual(repeater.getValue(WallDiodeBlock.FACING), Direction.DOWN, "its output where the placer looks: up");
        h.assertTrue(h.getBlockState(wallComparator.north()).is(ModBlocks.WALL_COMPARATOR.get()), "a comparator on the wall");
        // The floor: the top face of a block gives vanilla's own dust.
        ItemStack dustStack = new ItemStack(Items.REDSTONE, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, dustStack);
        BlockPos floor = h.absolutePos(new BlockPos(6, 0, 2));
        player.gameMode.useItemOn(player, h.getLevel(), dustStack, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5), Direction.UP, floor, false));
        h.assertTrue(h.getBlockState(new BlockPos(6, 1, 2)).is(Blocks.REDSTONE_WIRE), "vanilla's dust on the floor");
        h.assertValueEqual(dustStack.getCount(), 7, "spent by vanilla");
        h.succeed();
    }

    @GameTest(template = "wall", timeoutTicks = 20)
    public void theWallFormsPickAsTheVanillaItems(GameTestHelper h) {
        wall(h, 2, 2);
        wallDust(h, 2, 2);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 5));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), NORTH, pos, false);
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 5)).getCloneItemStack(hit, h.getLevel(), pos, player).is(Items.REDSTONE), "wall dust picks as redstone");
        h.assertTrue(repeater(0, Direction.DOWN, 1).getCloneItemStack(hit, h.getLevel(), pos, player).is(Items.REPEATER), "a wall repeater as a repeater");
        h.assertTrue(ModBlocks.WALL_COMPARATOR.get().defaultBlockState().getCloneItemStack(hit, h.getLevel(), pos, player).is(Items.COMPARATOR), "a wall comparator as a comparator");
        h.succeed();
    }
}
