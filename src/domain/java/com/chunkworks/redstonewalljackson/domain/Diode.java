/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.List;

/**
 * The frame-dependent part of a repeater or comparator: which way it faces when placed, and
 * which two sides its lock and side inputs come from. Everything else about a diode is a
 * scheduled tick and a level read, which the adapter ports as vanilla wrote it.
 */
public final class Diode {
    private Diode() {}

    /**
     * effects: returns the two planar directions beside {@code facing} in frame {@code f}, where
     * a repeater is locked from and a comparator takes its side inputs<br>
     * throws: IllegalArgumentException if {@code facing} leaves the plane
     */
    public static List<Dir> sides(Frame f, Dir facing) {
        Planar p = f.planar(facing).orElseThrow(() -> new IllegalArgumentException(facing + " is not in " + f));
        return p.beside().stream().map(f::world).toList();
    }

    /**
     * effects: returns the input direction of a diode placed by someone looking along
     * {@code (lx, ly, lz)} in frame {@code f}: the planar direction nearest the look, turned
     * round, so the input faces the placer and the output points where they look, as a floor
     * repeater's does; ties go to TOP, BOTTOM, LEFT, RIGHT in that order<br>
     * throws: IllegalArgumentException for a zero look
     */
    public static Dir facingFromLook(Frame f, double lx, double ly, double lz) {
        if (lx == 0 && ly == 0 && lz == 0) {
            throw new IllegalArgumentException("a look has a direction");
        }
        Planar best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (Planar p : Planar.values()) {
            Dir d = f.world(p);
            double dot = lx * d.x + ly * d.y + lz * d.z;
            if (dot > bestDot) {
                bestDot = dot;
                best = p;
            }
        }
        return f.world(best).opposite();
    }
}
