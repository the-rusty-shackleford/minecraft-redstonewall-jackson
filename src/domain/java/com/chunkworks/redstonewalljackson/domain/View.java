/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Everything around one wire that its rules read, already turned into the wire's frame: the
 * block in front of it (toward up), and for each planar direction the block one step that way,
 * the one a further step up (where a wire climbs to) and the one a further step toward the
 * support (where a wire steps down to). The adapter builds it from the level; the rules never
 * see the level. Immutable.
 *
 * <p>RI: every planar direction has its three cells; {@code bestOtherSignal} is 0..15.
 *
 * @param above           the block in front of the wire, toward up
 * @param side            the block one step in each planar direction
 * @param sideUp          the block one planar step and one step up
 * @param sideDown        the block one planar step and one step toward the support
 * @param bestOtherSignal the strongest signal any of the six neighbours that is not a wire gives the wire
 */
public record View(Cell above, Map<Planar, Cell> side, Map<Planar, Cell> sideUp, Map<Planar, Cell> sideDown, int bestOtherSignal) {

    public View {
        Objects.requireNonNull(above, "above");
        side = complete(side, "side");
        sideUp = complete(sideUp, "sideUp");
        sideDown = complete(sideDown, "sideDown");
        if (bestOtherSignal < 0 || bestOtherSignal > 15) {
            throw new IllegalArgumentException("a signal is 0..15, was " + bestOtherSignal);
        }
    }

    private static Map<Planar, Cell> complete(Map<Planar, Cell> cells, String name) {
        EnumMap<Planar, Cell> copy = new EnumMap<>(Planar.class);
        for (Planar p : Planar.values()) {
            Cell cell = cells.get(p);
            if (cell == null) {
                throw new IllegalArgumentException(name + " lacks " + p);
            }
            copy.put(p, cell);
        }
        return Map.copyOf(copy);
    }

    /** effects: returns a view with every cell air and no signal: a wire alone in the open */
    public static View empty() {
        return of(Cell.AIR, 0, p -> Cell.AIR, p -> Cell.AIR, p -> Cell.AIR);
    }

    /** effects: returns a view built from three functions of the planar direction */
    public static View of(Cell above, int bestOtherSignal, Function<Planar, Cell> side, Function<Planar, Cell> sideUp,
                          Function<Planar, Cell> sideDown) {
        EnumMap<Planar, Cell> s = new EnumMap<>(Planar.class), u = new EnumMap<>(Planar.class), d = new EnumMap<>(Planar.class);
        for (Planar p : Planar.values()) {
            s.put(p, side.apply(p));
            u.put(p, sideUp.apply(p));
            d.put(p, sideDown.apply(p));
        }
        return new View(above, s, u, d, bestOtherSignal);
    }

    public Cell side(Planar p) {
        return side.get(p);
    }

    public Cell sideUp(Planar p) {
        return sideUp.get(p);
    }

    public Cell sideDown(Planar p) {
        return sideDown.get(p);
    }

    /** effects: returns this view with {@code cell} one step {@code p} */
    public View withSide(Planar p, Cell cell) {
        EnumMap<Planar, Cell> s = new EnumMap<>(side);
        s.put(p, cell);
        return new View(above, s, sideUp, sideDown, bestOtherSignal);
    }

    /** effects: returns this view with {@code cell} one step {@code p} and one up */
    public View withSideUp(Planar p, Cell cell) {
        EnumMap<Planar, Cell> u = new EnumMap<>(sideUp);
        u.put(p, cell);
        return new View(above, side, u, sideDown, bestOtherSignal);
    }

    /** effects: returns this view with {@code cell} one step {@code p} and one toward the support */
    public View withSideDown(Planar p, Cell cell) {
        EnumMap<Planar, Cell> d = new EnumMap<>(sideDown);
        d.put(p, cell);
        return new View(above, side, sideUp, d, bestOtherSignal);
    }

    /** effects: returns this view with {@code cell} in front of the wire */
    public View withAbove(Cell cell) {
        return new View(cell, side, sideUp, sideDown, bestOtherSignal);
    }

    /** effects: returns this view with {@code signal} as the strongest non-wire neighbour signal */
    public View withSignal(int signal) {
        return new View(above, side, sideUp, sideDown, signal);
    }
}
