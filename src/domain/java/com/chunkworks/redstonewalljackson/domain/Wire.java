/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.Optional;

/**
 * Vanilla's redstone dust rules, written once over a {@link View} so they hold in every
 * {@link Frame}: how a wire reaches its sides, what power it settles at, and what it gives a
 * neighbour. The floor frame reproduces vanilla; a wall or ceiling frame is the same rule turned.
 *
 * <p>Two things are not vanilla's, both for the inside corner where a run turns from one plane
 * onto the plane at right angles to it (a floor run up a wall, a wall run onto the ceiling, one
 * wall onto the next): the wire in front of this one counts as a neighbour when it rests on the
 * block beside this wire, drawn as vanilla's climb up that block's face (on a floor nothing is
 * ever a wire in front); and a wire gives another wire nothing through a signal, since wires
 * read each other's power directly and vanilla loses a point of power across every wire-to-wire
 * step.
 */
public final class Wire {
    private Wire() {}

    /**
     * effects: returns how the wire meets its side {@code p}: climbing (UP) the sturdy face of a
     * block there that a wire sits on top of, provided nothing solid stands in front of this wire;
     * likewise climbing that face when the wire in front rests on the block there (the inside
     * corner), SIDE if the face is not sturdy; SIDE for a block that connects, or for a step down
     * to a connecting block below a non-conductor; NONE for a conductor with no wire on it and
     * for empty space
     */
    public static Joint connectingSide(View v, Planar p) {
        Cell side = v.side(p);
        if (!v.above().conductor() && side.standable() && v.sideUp(p).connects()) {
            return side.sturdyTowardWire() ? Joint.UP : Joint.SIDE;
        }
        if (v.frontRestsOn().equals(Optional.of(p))) {
            return side.sturdyTowardWire() ? Joint.UP : Joint.SIDE;
        }
        if (side.connects()) {
            return Joint.SIDE;
        }
        if (side.conductor()) {
            return Joint.NONE;
        }
        return v.sideDown(p).connects() ? Joint.SIDE : Joint.NONE;
    }

    /**
     * effects: returns the shape a wire takes given its surroundings, from scratch: a joint toward
     * every side that connects; then, unless {@code keepDot} and nothing connects (a dot stays a
     * dot), a wire with no joints along one axis is drawn straight across the other, so a lone
     * wire is a cross and a wire with one neighbour is a line through it
     */
    public static Shape connectionState(View v, boolean keepDot) {
        Shape s = Shape.DOT;
        for (Planar p : Planar.values()) {
            s = s.with(p, connectingSide(v, p));
        }
        if (keepDot && s.isDot()) {
            return s;
        }
        boolean vertical = s.connected(Planar.TOP) || s.connected(Planar.BOTTOM);
        boolean horizontal = s.connected(Planar.LEFT) || s.connected(Planar.RIGHT);
        if (!vertical) {
            if (!s.connected(Planar.LEFT)) s = s.with(Planar.LEFT, Joint.SIDE);
            if (!s.connected(Planar.RIGHT)) s = s.with(Planar.RIGHT, Joint.SIDE);
        }
        if (!horizontal) {
            if (!s.connected(Planar.TOP)) s = s.with(Planar.TOP, Joint.SIDE);
            if (!s.connected(Planar.BOTTOM)) s = s.with(Planar.BOTTOM, Joint.SIDE);
        }
        return s;
    }

    /**
     * effects: returns the shape after the block toward {@code p} changed: if whether that side
     * connects is unchanged and the wire is not a cross, only that joint is refreshed; otherwise
     * the shape is drawn from scratch, a cross allowed to become a line
     */
    public static Shape updateShape(View v, Shape current, Planar p) {
        Joint joint = connectingSide(v, p);
        if (joint.connected() == current.connected(p) && !current.isCross()) {
            return current.with(p, joint);
        }
        return connectionState(v, false);
    }

    /**
     * effects: returns the shape a click turns the wire into, if a click changes it: a cross
     * becomes a dot and a dot a cross, each then redrawn against the surroundings; anything else
     * is left alone
     */
    public static Optional<Shape> toggled(View v, Shape current) {
        if (current.isCross()) {
            return Optional.of(connectionState(v, true));
        }
        if (current.isDot()) {
            return Optional.of(connectionState(v, false));
        }
        return Optional.empty();
    }

    /**
     * effects: returns the power the wire settles at: the strongest non-wire signal it receives,
     * or one less than the strongest wire it touches, whichever is more. A wire is touched one
     * planar step away; one step up beyond a conductor when nothing solid stands in front of this
     * wire (the climb); one step down beyond a non-conductor (the step); and in front of the wire
     * when the wire there rests on a block beside this one (the inside corner)
     */
    public static int targetPower(View v) {
        int i = v.bestOtherSignal();
        int j = 0;
        if (i < 15) {
            for (Planar p : Planar.values()) {
                Cell side = v.side(p);
                j = Math.max(j, side.wirePower());
                if (side.conductor() && !v.above().conductor()) {
                    j = Math.max(j, v.sideUp(p).wirePower());
                } else if (!side.conductor()) {
                    j = Math.max(j, v.sideDown(p).wirePower());
                }
            }
            if (v.frontRestsOn().isPresent()) {
                j = Math.max(j, v.above().wirePower());
            }
        }
        return Math.max(i, j - 1);
    }

    /**
     * effects: returns the signal a wire of {@code power} and {@code shape} in frame {@code f}
     * gives the neighbour in direction {@code askerDir} from the wire: nothing to another wire
     * (wires read each other's power directly), nothing in front of the wire, all of it into
     * the block it rests on, and all of it along a planar direction it reaches toward
     */
    public static int signal(int power, Shape shape, Frame f, Dir askerDir, boolean askerIsWire) {
        if (askerIsWire || power == 0 || askerDir == f.up()) {
            return 0;
        }
        if (askerDir == f.support()) {
            return power;
        }
        return f.planar(askerDir).map(p -> shape.connected(p) ? power : 0).orElse(0);
    }
}
