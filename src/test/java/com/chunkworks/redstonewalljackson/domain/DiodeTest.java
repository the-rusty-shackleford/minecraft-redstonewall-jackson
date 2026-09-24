/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Partitions. Sides: a floor diode facing north or east has vanilla's clockwise and
 * counter-clockwise neighbours; a wall diode facing up has the wall's left and right, facing
 * along the wall has up and down; a facing out of the plane is refused. Placement: a floor
 * look mostly north gives vanilla's south-facing input whatever the pitch; on a wall a look up
 * faces the input down, a look along the wall faces it the other way, the component out of the
 * wall is ignored; a zero look is refused; ties are settled in a fixed order.
 */
final class DiodeTest {

    @Test
    void sidesAreTheTwoPlanarDirectionsBesideTheFacing() {
        assertEquals(List.of(Dir.WEST, Dir.EAST), Diode.sides(Frame.FLOOR, Dir.NORTH));
        assertEquals(List.of(Dir.NORTH, Dir.SOUTH), Diode.sides(Frame.FLOOR, Dir.EAST));
        Frame north = Frame.wall(Dir.NORTH);
        assertEquals(List.of(Dir.EAST, Dir.WEST), Diode.sides(north, Dir.UP));
        assertEquals(List.of(Dir.UP, Dir.DOWN), Diode.sides(north, Dir.EAST));
        assertThrows(IllegalArgumentException.class, () -> Diode.sides(north, Dir.NORTH));
        assertThrows(IllegalArgumentException.class, () -> Diode.sides(Frame.FLOOR, Dir.UP));
    }

    @Test
    void theInputFacesThePlacerAlongThePlanarDirectionNearestTheLook() {
        assertEquals(Dir.SOUTH, Diode.facingFromLook(Frame.FLOOR, 0.1, -0.9, -0.4), "looking mostly north and down: the input is south, as vanilla places it");
        assertEquals(Dir.WEST, Diode.facingFromLook(Frame.FLOOR, 1, 0, 0.2));
        Frame north = Frame.wall(Dir.NORTH);
        assertEquals(Dir.DOWN, Diode.facingFromLook(north, 0, 1, -0.9), "looking up the wall: the output goes up, the input is below");
        assertEquals(Dir.UP, Diode.facingFromLook(north, 0.2, -1, -0.9));
        assertEquals(Dir.WEST, Diode.facingFromLook(north, 1, 0.1, -0.5), "looking east along the wall: the input is west");
        assertEquals(Dir.EAST, Diode.facingFromLook(north, -1, 0.1, 0));
        assertEquals(Dir.DOWN, Diode.facingFromLook(north, 0, 0.5, -10), "the component out of the wall does not count");
        assertThrows(IllegalArgumentException.class, () -> Diode.facingFromLook(north, 0, 0, 0));
        assertEquals(Dir.DOWN, Diode.facingFromLook(north, 1, 1, 0), "a tie goes to the top");
        // The ceiling: looking up and mostly north places the input south, as on the floor; the sides are east and west.
        assertEquals(Dir.SOUTH, Diode.facingFromLook(Frame.CEILING, 0.1, 0.9, -0.4));
        assertEquals(Dir.WEST, Diode.facingFromLook(Frame.CEILING, 1, 0.9, 0.2));
        assertEquals(List.of(Dir.EAST, Dir.WEST), Diode.sides(Frame.CEILING, Dir.NORTH));
    }
}
