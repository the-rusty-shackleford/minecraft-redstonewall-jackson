/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

/** How a wire meets one planar side: not at all, along the plane, or climbing the face of a block there. Vanilla's RedstoneSide. */
public enum Joint {
    NONE, SIDE, UP;

    /** effects: returns whether the wire reaches that side at all */
    public boolean connected() {
        return this != NONE;
    }
}
