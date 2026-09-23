/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.List;

/**
 * The four directions within a redstone plane, named as a person facing the plane sees them:
 * on a floor with north at the top of the map, on a wall standing in front of it. Vanilla's
 * north, south, west and east on the floor are TOP, BOTTOM, LEFT and RIGHT here, so one set of
 * rules serves every plane.
 */
public enum Planar {
    TOP, BOTTOM, LEFT, RIGHT;

    /** effects: returns the planar direction pointing the other way */
    public Planar opposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    /** effects: returns the two planar directions at right angles to this, in a fixed order */
    public List<Planar> beside() {
        return this == TOP || this == BOTTOM ? List.of(LEFT, RIGHT) : List.of(TOP, BOTTOM);
    }
}
