/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

/**
 * A block near a wire, as the wire's rules see it. Immutable.
 *
 * <p>RI: {@code wirePower} is -1 (no wire there) or 0..15.
 *
 * @param wirePower        the power of the wire there, of any kind, or -1 when it is not a wire
 * @param conductor        whether it is a solid block that carries power (vanilla's redstone conductor)
 * @param standable        whether a wire could stand on it toward the frame's up: its face toward up is
 *                         sturdy, or it is a hopper; a trapdoor counts too, as vanilla has it
 * @param sturdyTowardWire whether its face toward the wire is sturdy, so a wire can climb it
 * @param connects         whether it says a wire connects to it: vanilla's canRedstoneConnectTo, asked
 *                         with the direction from the wire for a block beside it and with no direction
 *                         for a block above or below a side
 */
public record Cell(int wirePower, boolean conductor, boolean standable, boolean sturdyTowardWire, boolean connects) {

    /** Nothing there. */
    public static final Cell AIR = new Cell(-1, false, false, false, false);

    public Cell {
        if (wirePower < -1 || wirePower > 15) {
            throw new IllegalArgumentException("a wire's power is 0..15, or -1 for no wire, was " + wirePower);
        }
    }

    /** effects: returns a wire of {@code power}: it connects, and neither conducts nor bears weight */
    public static Cell wire(int power) {
        return new Cell(power, false, false, false, true);
    }

    /** effects: returns a plain solid block: conducts, can be stood on and climbed, does not connect */
    public static Cell solid() {
        return new Cell(-1, true, true, true, false);
    }

    /** effects: returns a block a wire connects to that is not a wire: a repeater's end, a lever, a torch */
    public static Cell source() {
        return new Cell(-1, false, false, false, true);
    }

    /** effects: returns whether a wire of any kind is there */
    public boolean wire() {
        return wirePower >= 0;
    }
}
