/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.Objects;

/**
 * How a wire meets its four planar sides. Immutable value.
 * AF: the joint toward each of TOP, BOTTOM, LEFT and RIGHT.
 *
 * @param top    the joint toward TOP
 * @param bottom the joint toward BOTTOM
 * @param left   the joint toward LEFT
 * @param right  the joint toward RIGHT
 */
public record Shape(Joint top, Joint bottom, Joint left, Joint right) {

    /** Reaching nothing: the dot. */
    public static final Shape DOT = new Shape(Joint.NONE, Joint.NONE, Joint.NONE, Joint.NONE);
    /** Reaching every way along the plane: the cross a wire is placed as. */
    public static final Shape CROSS = new Shape(Joint.SIDE, Joint.SIDE, Joint.SIDE, Joint.SIDE);

    public Shape {
        Objects.requireNonNull(top, "top");
        Objects.requireNonNull(bottom, "bottom");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    /** effects: returns the joint toward {@code p} */
    public Joint get(Planar p) {
        return switch (p) {
            case TOP -> top;
            case BOTTOM -> bottom;
            case LEFT -> left;
            case RIGHT -> right;
        };
    }

    /** effects: returns this shape with {@code joint} toward {@code p} */
    public Shape with(Planar p, Joint joint) {
        return switch (p) {
            case TOP -> new Shape(joint, bottom, left, right);
            case BOTTOM -> new Shape(top, joint, left, right);
            case LEFT -> new Shape(top, bottom, joint, right);
            case RIGHT -> new Shape(top, bottom, left, joint);
        };
    }

    /** effects: returns whether the wire reaches toward {@code p} */
    public boolean connected(Planar p) {
        return get(p).connected();
    }

    /** effects: returns whether the wire reaches nowhere */
    public boolean isDot() {
        return !top.connected() && !bottom.connected() && !left.connected() && !right.connected();
    }

    /** effects: returns whether the wire reaches every way */
    public boolean isCross() {
        return top.connected() && bottom.connected() && left.connected() && right.connected();
    }
}
