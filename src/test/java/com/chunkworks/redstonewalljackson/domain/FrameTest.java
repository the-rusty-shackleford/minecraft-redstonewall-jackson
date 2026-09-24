/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Partitions: the floor; each of the four walls; the ceiling; the planar directions of every
 * frame are the four perpendicular to up, each once, and the inverse mapping agrees; support is
 * opposite up; a vertical wall normal is refused; Dir turns.
 */
final class FrameTest {

    @Test
    void theFloorIsVanillasMapNorthUpWestLeft() {
        assertEquals(Dir.NORTH, Frame.FLOOR.world(Planar.TOP));
        assertEquals(Dir.SOUTH, Frame.FLOOR.world(Planar.BOTTOM));
        assertEquals(Dir.WEST, Frame.FLOOR.world(Planar.LEFT));
        assertEquals(Dir.EAST, Frame.FLOOR.world(Planar.RIGHT));
        assertEquals(Dir.DOWN, Frame.FLOOR.support());
        assertFalse(Frame.FLOOR.isWall());
    }

    @Test
    void aNorthWallHasUpAtTheTopAndStandingBeforeItEastOnTheLeft() {
        Frame wall = Frame.wall(Dir.NORTH);   // the wire is north of the wall block, facing north
        assertEquals(Dir.UP, wall.world(Planar.TOP));
        assertEquals(Dir.DOWN, wall.world(Planar.BOTTOM));
        assertEquals(Dir.EAST, wall.world(Planar.LEFT));
        assertEquals(Dir.WEST, wall.world(Planar.RIGHT));
        assertEquals(Dir.SOUTH, wall.support());
        assertTrue(wall.isWall());
    }

    @Test
    void theCeilingIsTheNorthWallTippedBackOverYourHead() {
        Frame north = Frame.wall(Dir.NORTH);
        // Tipping the wall back turns its up (world up) to north and its normal (north) to down; east stays east.
        assertEquals(Dir.NORTH, Frame.CEILING.world(Planar.TOP));
        assertEquals(Dir.SOUTH, Frame.CEILING.world(Planar.BOTTOM));
        assertEquals(north.world(Planar.LEFT), Frame.CEILING.world(Planar.LEFT), "the left hand's way is unchanged");
        assertEquals(Dir.EAST, Frame.CEILING.world(Planar.LEFT));
        assertEquals(Dir.WEST, Frame.CEILING.world(Planar.RIGHT));
        assertEquals(Dir.UP, Frame.CEILING.support());
        assertEquals(Dir.DOWN, Frame.CEILING.up());
        assertFalse(Frame.CEILING.isWall());
        assertEquals(Frame.CEILING, Frame.facing(Dir.DOWN));
        assertEquals(Frame.FLOOR, Frame.facing(Dir.UP));
        assertEquals(north, Frame.facing(Dir.NORTH));
    }

    @Test
    void everyFramesPlanarDirectionsAreTheFourPerpendicularToItsNormalEachOnce() {
        for (Dir normal : Dir.values()) {
            Frame f = Frame.facing(normal);
            EnumSet<Dir> seen = EnumSet.noneOf(Dir.class);
            for (Planar p : Planar.values()) {
                Dir d = f.world(p);
                assertTrue(d != normal && d != normal.opposite(), normal + ": " + p + " leaves the plane");
                assertTrue(seen.add(d), normal + ": " + d + " twice");
                assertEquals(Optional.of(p), f.planar(d));
            }
            assertEquals(Optional.empty(), f.planar(normal));
            assertEquals(Optional.empty(), f.planar(normal.opposite()));
            assertEquals(f.world(Planar.LEFT).opposite(), f.world(Planar.RIGHT));
            assertEquals(f.world(Planar.TOP).opposite(), f.world(Planar.BOTTOM));
            assertEquals(normal.opposite(), f.support());
        }
    }

    @Test
    void aVerticalWallNormalIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Frame.wall(Dir.UP));
        assertThrows(IllegalArgumentException.class, () -> Frame.wall(Dir.DOWN));
    }

    @Test
    void directionsTurnAsTheGamesDo() {
        assertEquals(Dir.EAST, Dir.NORTH.clockwise());
        assertEquals(Dir.WEST, Dir.NORTH.counterClockwise());
        assertEquals(Dir.NORTH, Dir.WEST.clockwise());
        assertEquals(Dir.SOUTH, Dir.NORTH.opposite());
        assertThrows(IllegalArgumentException.class, Dir.UP::clockwise);
        for (Dir d : Dir.values()) {
            assertEquals(d, d.opposite().opposite());
            assertEquals(d.horizontal(), d.y == 0);
        }
    }
}
