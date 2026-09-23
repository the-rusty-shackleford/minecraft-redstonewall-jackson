/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

/**
 * The six directions of the world, free of the game. Named and ordered as the game names
 * them so the adapter maps by name.
 * AF: a unit step (x, y, z) along one axis. RI: exactly one component is nonzero and it is +-1.
 */
public enum Dir {
    DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);

    public final int x, y, z;

    Dir(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** effects: returns the direction pointing the other way */
    public Dir opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    /** effects: returns whether this lies in the horizontal plane */
    public boolean horizontal() {
        return y == 0;
    }

    /**
     * effects: returns the horizontal direction a quarter turn clockwise from this, seen from above
     * (north, east, south, west, north); throws: IllegalArgumentException for a vertical
     */
    public Dir clockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalArgumentException(this + " has no clockwise turn");
        };
    }

    /** effects: returns the horizontal direction a quarter turn counter-clockwise from this; throws for a vertical */
    public Dir counterClockwise() {
        return clockwise().opposite();
    }
}
