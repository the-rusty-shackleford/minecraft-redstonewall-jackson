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
 * Partitions: the floor; each of the four walls; the planar directions of a frame are the
 * four perpendicular to up, each once, and the inverse mapping agrees; support is opposite
 * up; a ceiling and a vertical wall normal are refused; Dir turns.
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
    void everyWallsPlanarDirectionsAreTheFourPerpendicularToItsNormalEachOnce() {
        for (Dir normal : List.of(Dir.NORTH, Dir.SOUTH, Dir.WEST, Dir.EAST)) {
            Frame wall = Frame.wall(normal);
            EnumSet<Dir> seen = EnumSet.noneOf(Dir.class);
            for (Planar p : Planar.values()) {
                Dir d = wall.world(p);
                assertTrue(d != normal && d != normal.opposite(), normal + ": " + p + " leaves the plane");
                assertTrue(seen.add(d), normal + ": " + d + " twice");
                assertEquals(Optional.of(p), wall.planar(d));
            }
            assertEquals(Optional.empty(), wall.planar(normal));
            assertEquals(Optional.empty(), wall.planar(normal.opposite()));
            assertEquals(wall.world(Planar.LEFT).opposite(), wall.world(Planar.RIGHT));
            assertEquals(wall.world(Planar.TOP).opposite(), wall.world(Planar.BOTTOM));
        }
    }

    @Test
    void aCeilingAndAVerticalWallNormalAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(Dir.DOWN));
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
