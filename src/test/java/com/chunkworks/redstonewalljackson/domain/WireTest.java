/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import static com.chunkworks.redstonewalljackson.domain.Joint.NONE;
import static com.chunkworks.redstonewalljackson.domain.Joint.SIDE;
import static com.chunkworks.redstonewalljackson.domain.Joint.UP;
import static com.chunkworks.redstonewalljackson.domain.Planar.BOTTOM;
import static com.chunkworks.redstonewalljackson.domain.Planar.LEFT;
import static com.chunkworks.redstonewalljackson.domain.Planar.RIGHT;
import static com.chunkworks.redstonewalljackson.domain.Planar.TOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Partitions, each vanilla's rule for dust on a floor, which a view makes frame-free.
 * Joints: a wire beside; a source beside; a conductor beside with nothing on it; a climb onto
 * a sturdy block with a wire on top, a SIDE when that face is not sturdy, no climb with a
 * conductor in front of the wire; a step down beyond a non-conductor; nothing. Shapes from
 * scratch: alone is a cross, alone kept as a dot, one neighbour is a line through it, two
 * opposite a line, two adjacent a corner, three a tee, four a cross. Update of one side: a
 * joint refreshed in place, a cross redrawn, a change redrawn. Toggle: cross to dot, dot to
 * cross, a line untouched. Power: a non-wire signal; a wire beside less one; the climb only
 * with a conductor beside and nothing in front; the step only beyond a non-conductor; the
 * wire in front; fifteen from a source ends the search; the strongest wins; nothing is zero.
 * Signals: to a wire nothing; in front nothing; into the support all; along a connected side
 * all; along an unconnected side nothing; at zero nothing; the same in every frame.
 */
final class WireTest {

    private static View alone() {
        return View.empty();
    }

    @Test
    void jointsFollowWhatStandsBeside() {
        assertEquals(SIDE, Wire.connectingSide(alone().withSide(TOP, Cell.wire(3)), TOP));
        assertEquals(SIDE, Wire.connectingSide(alone().withSide(LEFT, Cell.source()), LEFT));
        assertEquals(NONE, Wire.connectingSide(alone().withSide(RIGHT, Cell.solid()), RIGHT));
        assertEquals(NONE, Wire.connectingSide(alone(), BOTTOM));
        // The climb: a solid block beside with a wire on top of it, nothing solid in front of this wire.
        View climb = alone().withSide(TOP, Cell.solid()).withSideUp(TOP, Cell.wire(0));
        assertEquals(UP, Wire.connectingSide(climb, TOP));
        assertEquals(SIDE, Wire.connectingSide(climb.withSide(TOP, new Cell(-1, true, true, false, false)), TOP), "a face that is not sturdy is reached along the plane instead");
        assertEquals(NONE, Wire.connectingSide(climb.withAbove(Cell.solid()), TOP), "a conductor in front of the wire stops the climb");
        // The step down: nothing beside, a wire one step further toward the support.
        assertEquals(SIDE, Wire.connectingSide(alone().withSideDown(RIGHT, Cell.wire(9)), RIGHT));
        assertEquals(NONE, Wire.connectingSide(alone().withSide(RIGHT, Cell.solid()).withSideDown(RIGHT, Cell.wire(9)), RIGHT), "no step down through a conductor");
    }

    @Test
    void aWireAloneIsACrossUnlessItWasADot() {
        assertEquals(Shape.CROSS, Wire.connectionState(alone(), false));
        assertEquals(Shape.DOT, Wire.connectionState(alone(), true));
    }

    @Test
    void neighboursDrawLinesCornersTeesAndCrosses() {
        assertEquals(new Shape(SIDE, SIDE, NONE, NONE), Wire.connectionState(alone().withSide(TOP, Cell.wire(1)), false), "one neighbour: a line through it");
        assertEquals(new Shape(NONE, NONE, SIDE, SIDE), Wire.connectionState(alone().withSide(RIGHT, Cell.wire(1)), true), "even when it was a dot");
        assertEquals(new Shape(SIDE, SIDE, NONE, NONE), Wire.connectionState(alone().withSide(TOP, Cell.wire(1)).withSide(BOTTOM, Cell.wire(1)), false));
        assertEquals(new Shape(SIDE, NONE, NONE, SIDE), Wire.connectionState(alone().withSide(TOP, Cell.wire(1)).withSide(RIGHT, Cell.wire(1)), false), "a corner");
        assertEquals(new Shape(SIDE, SIDE, NONE, SIDE), Wire.connectionState(alone().withSide(TOP, Cell.wire(1)).withSide(BOTTOM, Cell.wire(1)).withSide(RIGHT, Cell.wire(1)), false), "a tee");
        View all = alone().withSide(TOP, Cell.wire(1)).withSide(BOTTOM, Cell.wire(1)).withSide(LEFT, Cell.wire(1)).withSide(RIGHT, Cell.wire(1));
        assertEquals(Shape.CROSS, Wire.connectionState(all, false));
        View climbing = alone().withSide(LEFT, Cell.solid()).withSideUp(LEFT, Cell.wire(1));
        assertEquals(new Shape(NONE, NONE, UP, SIDE), Wire.connectionState(climbing, false), "a climb is a joint like any other, and the line runs through");
    }

    @Test
    void oneSideChangingRefreshesThatJointUnlessTheShapeMustBeRedrawn() {
        Shape line = new Shape(SIDE, SIDE, NONE, NONE);
        View v = alone().withSide(TOP, Cell.wire(1)).withSide(BOTTOM, Cell.wire(1));
        assertEquals(line, Wire.updateShape(v, line, TOP), "still connected: the joint is refreshed in place");
        View climb = v.withSide(TOP, Cell.solid()).withSideUp(TOP, Cell.wire(1));
        assertEquals(new Shape(UP, SIDE, NONE, NONE), Wire.updateShape(climb, line, TOP), "a joint that changes kind but stays connected is refreshed in place");
        View corner = v.withSide(BOTTOM, Cell.AIR).withSide(RIGHT, Cell.wire(1));
        assertEquals(new Shape(SIDE, NONE, NONE, SIDE), Wire.updateShape(corner, line, RIGHT), "a side that starts connecting redraws the shape");
        assertEquals(new Shape(SIDE, SIDE, NONE, NONE), Wire.updateShape(v, Shape.CROSS, TOP), "a cross is always redrawn");
    }

    @Test
    void aClickTogglesDotAndCrossAndLeavesLinesAlone() {
        assertEquals(Optional.of(Shape.DOT), Wire.toggled(alone(), Shape.CROSS));
        assertEquals(Optional.of(Shape.CROSS), Wire.toggled(alone(), Shape.DOT));
        assertEquals(Optional.empty(), Wire.toggled(alone(), new Shape(SIDE, SIDE, NONE, NONE)));
        View beside = alone().withSide(TOP, Cell.wire(1));
        assertEquals(Optional.of(new Shape(SIDE, SIDE, NONE, NONE)), Wire.toggled(beside, Shape.CROSS), "a cross with a neighbour becomes the line its neighbour draws");
    }

    @Test
    void powerIsTheStrongestSignalOrOneLessThanTheStrongestWireTouched() {
        assertEquals(0, Wire.targetPower(alone()));
        assertEquals(7, Wire.targetPower(alone().withSignal(7)));
        assertEquals(11, Wire.targetPower(alone().withSide(LEFT, Cell.wire(12))));
        assertEquals(0, Wire.targetPower(alone().withSide(LEFT, Cell.wire(0))), "a dead wire beside gives nothing, not minus one");
        assertEquals(12, Wire.targetPower(alone().withSignal(12).withSide(LEFT, Cell.wire(12))), "the signal wins over the wire's power less one");
        assertEquals(15, Wire.targetPower(alone().withSignal(15).withSide(LEFT, Cell.wire(15))));
        // The climb counts only over a conductor beside and with nothing solid in front.
        assertEquals(9, Wire.targetPower(alone().withSide(TOP, Cell.solid()).withSideUp(TOP, Cell.wire(10))));
        assertEquals(0, Wire.targetPower(alone().withSide(TOP, Cell.solid()).withSideUp(TOP, Cell.wire(10)).withAbove(Cell.solid())));
        assertEquals(0, Wire.targetPower(alone().withSideUp(TOP, Cell.wire(10))), "no conductor beside: the wire above it is out of reach");
        // The step down counts only beyond a non-conductor.
        assertEquals(4, Wire.targetPower(alone().withSideDown(RIGHT, Cell.wire(5))));
        assertEquals(0, Wire.targetPower(alone().withSide(RIGHT, Cell.solid()).withSideDown(RIGHT, Cell.wire(5))));
        // The seam: a floor wire in front of a wall wire.
        assertEquals(13, Wire.targetPower(alone().withAbove(Cell.wire(14))));
        // The strongest of everything.
        View busy = alone().withSignal(3).withSide(TOP, Cell.wire(5)).withSideDown(LEFT, Cell.wire(8)).withSide(RIGHT, Cell.solid()).withSideUp(RIGHT, Cell.wire(6));
        assertEquals(7, Wire.targetPower(busy));
    }

    @Test
    void aWireGivesItsPowerIntoItsSupportAndAlongConnectedSidesAndNothingToWiresOrForward() {
        Shape line = new Shape(SIDE, SIDE, NONE, NONE);
        for (Frame f : List.of(Frame.FLOOR, Frame.wall(Dir.NORTH), Frame.wall(Dir.EAST), Frame.wall(Dir.SOUTH), Frame.wall(Dir.WEST))) {
            assertEquals(9, Wire.signal(9, line, f, f.support(), false), f + ": into the support");
            assertEquals(0, Wire.signal(9, line, f, f.up(), false), f + ": nothing forward");
            assertEquals(9, Wire.signal(9, line, f, f.world(TOP), false), f + ": along a connected side");
            assertEquals(9, Wire.signal(9, line, f, f.world(BOTTOM), false));
            assertEquals(0, Wire.signal(9, line, f, f.world(LEFT), false), f + ": nothing along an unconnected side");
            assertEquals(0, Wire.signal(9, line, f, f.world(TOP), true), f + ": nothing to a wire");
            assertEquals(0, Wire.signal(0, line, f, f.support(), false), f + ": nothing at zero");
        }
    }

    @Test
    void cellsAndViewsHoldTheirInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new Cell(16, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Cell(-2, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> alone().withSignal(16));
        assertThrows(IllegalArgumentException.class, () -> new View(Cell.AIR, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), 0));
        assertEquals(true, Cell.wire(0).wire());
        assertEquals(false, Cell.solid().wire());
    }
}
