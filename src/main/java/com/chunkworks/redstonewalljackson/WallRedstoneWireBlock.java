/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.chunkworks.redstonewalljackson.domain.Frame;
import com.chunkworks.redstonewalljackson.domain.Joint;
import com.chunkworks.redstonewalljackson.domain.Planar;
import com.chunkworks.redstonewalljackson.domain.Shape;
import com.chunkworks.redstonewalljackson.domain.View;
import com.chunkworks.redstonewalljackson.domain.Wire;
import com.chunkworks.redstonewalljackson.mixin.RedStoneWireBlockAccessor;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Redstone dust on a wall or a ceiling: vanilla's dust with its plane stood up or turned over.
 * {@code FACING} is the plane's outward normal (the dust faces that way: a horizontal for a
 * wall, down for a ceiling); TOP, BOTTOM, LEFT and RIGHT are its joints in the plane, as a
 * person facing the plane sees them; POWER is the power. Every rule is {@link Wire}'s over a
 * {@link View} that {@link Views} reads in the block's {@link Frame}, so a wall run behaves as
 * the floor run it is a turned copy of. It places from vanilla's redstone item
 * ({@link Placement}) and drops it.
 */
public final class WallRedstoneWireBlock extends Block {

    public static final MapCodec<WallRedstoneWireBlock> CODEC = simpleCodec(WallRedstoneWireBlock::new);
    /** The plane's outward normal: never up, which is vanilla's own floor dust. */
    public static final DirectionProperty FACING = DirectionProperty.create("facing", d -> d != Direction.UP);
    public static final EnumProperty<RedstoneSide> TOP = EnumProperty.create("top", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> BOTTOM = EnumProperty.create("bottom", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> LEFT = EnumProperty.create("left", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> RIGHT = EnumProperty.create("right", RedstoneSide.class);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Planar, EnumProperty<RedstoneSide>> PROPERTY_BY_PLANAR = Map.of(
            Planar.TOP, TOP, Planar.BOTTOM, BOTTOM, Planar.LEFT, LEFT, Planar.RIGHT, RIGHT);

    private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>();
    /** Vanilla's dust colours by power, for the particles; the tint uses vanilla's own function. */
    private static final Vec3[] COLORS = new Vec3[16];
    /**
     * Vanilla's rule, shared with vanilla: while any wire computes its power, no wire gives a
     * signal, so none reads itself or another back through a strongly powered block. This is
     * this block's flag; vanilla's is read and set through the accessor.
     */
    private boolean shouldSignal = true;

    static {
        for (int i = 0; i <= 15; i++) {
            float f = (float) i / 15.0F;
            float r = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float g = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float b = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            COLORS[i] = new Vec3(r, g, b);
        }
    }

    public WallRedstoneWireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(TOP, RedstoneSide.NONE).setValue(BOTTOM, RedstoneSide.NONE)
                .setValue(LEFT, RedstoneSide.NONE).setValue(RIGHT, RedstoneSide.NONE).setValue(POWER, 0));
        for (BlockState state : getStateDefinition().getPossibleStates()) {
            if (state.getValue(POWER) == 0) {
                SHAPES.put(state, calculateShape(state));
            }
        }
    }

    @Override
    protected MapCodec<WallRedstoneWireBlock> codec() {
        return CODEC;
    }

    // --- the frame and the shape as the domain sees them ---------------------

    static Frame frame(BlockState state) {
        return Frames.of(state.getValue(FACING));
    }

    /** effects: returns the direction from a wire of either kind (vanilla's dust or this) into the block it rests on, or empty for anything else */
    public static Optional<Direction> restsToward(BlockState state) {
        return Views.restsToward(state);
    }

    static Shape shapeOf(BlockState state) {
        return new Shape(joint(state.getValue(TOP)), joint(state.getValue(BOTTOM)), joint(state.getValue(LEFT)), joint(state.getValue(RIGHT)));
    }

    static BlockState withShape(BlockState state, Shape shape) {
        for (Planar p : Planar.values()) {
            state = state.setValue(PROPERTY_BY_PLANAR.get(p), side(shape.get(p)));
        }
        return state;
    }

    private static Joint joint(RedstoneSide side) {
        return switch (side) {
            case NONE -> Joint.NONE;
            case SIDE -> Joint.SIDE;
            case UP -> Joint.UP;
        };
    }

    private static RedstoneSide side(Joint joint) {
        return switch (joint) {
            case NONE -> RedstoneSide.NONE;
            case SIDE -> RedstoneSide.SIDE;
            case UP -> RedstoneSide.UP;
        };
    }

    /**
     * effects: returns the state of dust placed at {@code pos} on the block whose face points
     * {@code normal} (a wall for a horizontal, the ceiling for down): a cross drawn against its neighbours
     */
    public static BlockState placementState(BlockGetter level, BlockPos pos, Direction normal) {
        BlockState state = ModBlocks.WALL_REDSTONE_WIRE.get().defaultBlockState().setValue(FACING, normal);
        return withShape(state, Wire.connectionState(Views.of(level, pos, frame(state)), false));
    }

    private static VoxelShape calculateShape(BlockState state) {
        Frame f = frame(state);
        VoxelShape shape = Frames.box(f, 3, 3, 0, 13, 13, 1);
        for (Planar p : Planar.values()) {
            RedstoneSide side = state.getValue(PROPERTY_BY_PLANAR.get(p));
            if (side == RedstoneSide.NONE) {
                continue;
            }
            shape = Shapes.or(shape, switch (p) {
                case TOP -> Frames.box(f, 3, 3, 0, 13, 16, 1);
                case BOTTOM -> Frames.box(f, 3, 0, 0, 13, 13, 1);
                case LEFT -> Frames.box(f, 0, 3, 0, 13, 13, 1);
                case RIGHT -> Frames.box(f, 3, 3, 0, 16, 13, 1);
            });
            if (side == RedstoneSide.UP) {
                shape = Shapes.or(shape, switch (p) {
                    case TOP -> Frames.box(f, 3, 15, 0, 13, 16, 16);
                    case BOTTOM -> Frames.box(f, 3, 0, 0, 13, 1, 16);
                    case LEFT -> Frames.box(f, 0, 3, 0, 1, 13, 16);
                    case RIGHT -> Frames.box(f, 15, 3, 0, 16, 13, 16);
                });
            }
        }
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.setValue(POWER, 0));
    }

    // --- shape upkeep: vanilla's updateShape, in the frame ---------------------

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Frame f = frame(state);
        Direction up = Frames.dir(f.up());
        if (dir == up.getOpposite()) {
            return Views.standable(level, neighborPos, neighbor, up) ? state : Blocks.AIR.defaultBlockState();
        }
        View view = Views.of(level, pos, f);
        if (dir == up) {
            return withShape(state, Wire.connectionState(view, shapeOf(state).isDot()));
        }
        Planar p = f.planar(Frames.dir(dir)).orElseThrow();
        return withShape(state, Wire.updateShape(view, shapeOf(state), p));
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
        Frame f = frame(state);
        Direction up = Frames.dir(f.up());
        for (Planar p : Planar.values()) {
            if (state.getValue(PROPERTY_BY_PLANAR.get(p)) == RedstoneSide.NONE) {
                continue;
            }
            Direction d = Frames.world(f, p);
            BlockPos sp = pos.relative(d);
            if (Views.isWire(level.getBlockState(sp))) {
                continue;
            }
            for (Direction step : new Direction[] {up.getOpposite(), up}) {
                BlockPos diagonal = sp.relative(step);
                if (Views.isWire(level.getBlockState(diagonal))) {
                    BlockPos from = diagonal.relative(d.getOpposite());
                    level.neighborShapeChanged(d.getOpposite(), level.getBlockState(from), diagonal, from, flags, recursionLeft);
                }
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction up = state.getValue(FACING);
        BlockPos support = pos.relative(up.getOpposite());
        return Views.standable(level, support, level.getBlockState(support), up);
    }

    // --- power: vanilla's, in the frame -----------------------------------------

    /** effects: returns whether wires are giving signals now: neither this block nor vanilla's is computing a wire's power */
    private boolean signalling() {
        return shouldSignal && ((RedStoneWireBlockAccessor) Blocks.REDSTONE_WIRE).redstonewalljackson$shouldSignal();
    }

    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) {
        RedStoneWireBlockAccessor vanilla = (RedStoneWireBlockAccessor) Blocks.REDSTONE_WIRE;
        boolean vanillaWas = vanilla.redstonewalljackson$shouldSignal();
        int best;
        shouldSignal = false;
        vanilla.redstonewalljackson$setShouldSignal(false);
        try {
            best = Views.bestOtherSignal(level, pos);
        } finally {
            shouldSignal = true;
            vanilla.redstonewalljackson$setShouldSignal(vanillaWas);
        }
        int target = Wire.targetPower(Views.of(level, pos, frame(state), best));
        if (state.getValue(POWER) != target) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, state.setValue(POWER, target), 2);
            }
            Set<BlockPos> touched = new HashSet<>();
            touched.add(pos);
            for (Direction d : Direction.values()) {
                touched.add(pos.relative(d));
            }
            for (BlockPos p : touched) {
                level.updateNeighborsAt(p, this);
            }
        }
    }

    /** effects: tells the neighbours of {@code pos} about a change there, if a wire of any kind is there */
    private void checkCornerChangeAt(Level level, BlockPos pos) {
        if (Views.isWire(level.getBlockState(pos))) {
            level.updateNeighborsAt(pos, this);
            for (Direction d : Direction.values()) {
                level.updateNeighborsAt(pos.relative(d), this);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos, Frame f) {
        Direction up = Frames.dir(f.up());
        for (Planar p : Planar.values()) {
            checkCornerChangeAt(level, pos.relative(Frames.world(f, p)));
        }
        for (Planar p : Planar.values()) {
            BlockPos sp = pos.relative(Frames.world(f, p));
            if (level.getBlockState(sp).isRedstoneConductor(level, sp)) {
                checkCornerChangeAt(level, sp.relative(up));
            } else {
                checkCornerChangeAt(level, sp.relative(up.getOpposite()));
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this) && !level.isClientSide) {
            Frame f = frame(state);
            updatePowerStrength(level, pos, state);
            for (Direction d : new Direction[] {Frames.dir(f.up()), Frames.dir(f.support())}) {
                level.updateNeighborsAt(pos.relative(d), this);
            }
            updateNeighborsOfNeighboringWires(level, pos, f);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            if (!level.isClientSide) {
                for (Direction d : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(d), this);
                }
                updatePowerStrength(level, pos, state);
                updateNeighborsOfNeighboringWires(level, pos, frame(state));
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (state.canSurvive(level, pos)) {
                updatePowerStrength(level, pos, state);
            } else {
                dropResources(state, level, pos);
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return signalling() ? getSignal(state, level, pos, side) : 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        int power = state.getValue(POWER);
        if (power == 0 || !signalling()) {
            return 0;
        }
        Frame f = frame(state);
        Direction askerDir = side.getOpposite();
        boolean askerIsWire = Views.isWire(level.getBlockState(pos.relative(askerDir)));
        // As vanilla does, the joints are read fresh from the neighbours, not from the stored state.
        Shape shape = Wire.connectionState(Views.of(level, pos, f), shapeOf(state).isDot());
        return Wire.signal(power, shape, f, Frames.dir(askerDir), askerIsWire);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * effects: returns whether a neighbour in the direction opposite {@code direction} (the
     * direction is from the asker to this block, as vanilla asks it) can connect: anything in
     * the plane or in front of the wire, never the block behind it; anything at all when no
     * direction is asked
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) {
            return true;
        }
        return direction.getOpposite() != state.getValue(FACING).getOpposite();
    }

    // --- the click: dot and cross -----------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        Frame f = frame(state);
        Optional<Shape> toggled = Wire.toggled(Views.of(level, pos, f), shapeOf(state));
        if (toggled.isEmpty()) {
            return InteractionResult.PASS;
        }
        BlockState next = withShape(state, toggled.get());
        if (next == state) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, next, 3);
        for (Planar p : Planar.values()) {
            Direction d = Frames.world(f, p);
            BlockPos sp = pos.relative(d);
            if (state.getValue(PROPERTY_BY_PLANAR.get(p)).isConnected() != next.getValue(PROPERTY_BY_PLANAR.get(p)).isConnected()
                    && level.getBlockState(sp).isRedstoneConductor(level, sp)) {
                level.updateNeighborsAtExceptFromFacing(sp, this, d.getOpposite());
            }
        }
        return InteractionResult.SUCCESS;
    }

    // --- looks -------------------------------------------------------------------

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(Items.REDSTONE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) {
            return;
        }
        Frame f = frame(state);
        Direction up = Frames.dir(f.up()), support = up.getOpposite();
        for (Planar p : Planar.values()) {
            Direction d = Frames.world(f, p);
            switch (state.getValue(PROPERTY_BY_PLANAR.get(p))) {
                case UP -> {
                    spawnParticlesAlongLine(level, random, pos, COLORS[power], d, up, -0.5F, 0.5F);
                    spawnParticlesAlongLine(level, random, pos, COLORS[power], support, d, 0.0F, 0.5F);
                }
                case SIDE -> spawnParticlesAlongLine(level, random, pos, COLORS[power], support, d, 0.0F, 0.5F);
                case NONE -> spawnParticlesAlongLine(level, random, pos, COLORS[power], support, d, 0.0F, 0.3F);
            }
        }
    }

    /** Vanilla's, unchanged: a dust particle somewhere along a line of the wire. */
    private static void spawnParticlesAlongLine(Level level, RandomSource random, BlockPos pos, Vec3 color, Direction xDirection, Direction zDirection, float min, float max) {
        float span = max - min;
        if (random.nextFloat() >= 0.2F * span) {
            return;
        }
        float at = min + span * random.nextFloat();
        double x = 0.5 + 0.4375F * xDirection.getStepX() + at * zDirection.getStepX();
        double y = 0.5 + 0.4375F * xDirection.getStepY() + at * zDirection.getStepY();
        double z = 0.5 + 0.4375F * xDirection.getStepZ() + at * zDirection.getStepZ();
        level.addParticle(new DustParticleOptions(color.toVector3f(), 1.0F), pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return turned(state, rotation::rotate);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return turned(state, mirror::mirror);
    }

    /**
     * effects: returns {@code state} with its plane and every joint carried through {@code turn},
     * a rotation or a mirror of world directions: each joint goes to the planar direction of the
     * new frame that its world direction turns into (on a wall a mirror swaps a viewer's hands)
     */
    private static BlockState turned(BlockState state, UnaryOperator<Direction> turn) {
        Frame from = frame(state);
        BlockState result = state.setValue(FACING, turn.apply(state.getValue(FACING)));
        Frame to = frame(result);
        for (Planar p : Planar.values()) {
            Planar q = to.planar(Frames.dir(turn.apply(Frames.world(from, p)))).orElseThrow();
            result = result.setValue(PROPERTY_BY_PLANAR.get(q), state.getValue(PROPERTY_BY_PLANAR.get(p)));
        }
        return result;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TOP, BOTTOM, LEFT, RIGHT, POWER);
    }
}
