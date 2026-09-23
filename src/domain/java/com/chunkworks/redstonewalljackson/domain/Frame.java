/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson.domain;

import java.util.Optional;

/**
 * The plane a run of redstone lies in, as an orientation of the floor. A floor circuit has an
 * "up" (world up), four planar directions (the horizontals) and a support direction (down). A
 * wall circuit is that plane stood up: up is the wall's outward normal, the planar directions
 * are world up, world down and the two horizontals along the wall, and support is into the
 * wall. Every rule written against a frame reproduces vanilla on the floor frame and is the
 * same rule, turned, on a wall.
 *
 * <p>AF: AF(up) = "the plane whose outward normal is {@code up}", with TOP, BOTTOM, LEFT and
 * RIGHT placed as a person facing the plane sees them: on the floor, north at the top of the
 * map and west on the left; on a wall, world up at the top and, standing in front of the wall,
 * the left hand's way on the left.<br>
 * RI: {@code up} is world up or horizontal (ceilings are not a frame yet); the four planar world
 * directions are exactly the four perpendicular to {@code up}, each once.
 *
 * @param up the plane's outward normal
 */
public record Frame(Dir up) {

    /** The floor: vanilla's one and only frame. */
    public static final Frame FLOOR = new Frame(Dir.UP);

    public Frame {
        if (up == Dir.DOWN) {
            throw new IllegalArgumentException("ceilings are not a frame yet");
        }
    }

    /**
     * effects: returns the frame of a wall whose face points {@code normal}: the wire sits in the
     * block in front of the wall, and {@code normal} runs from the wall toward it<br>
     * throws: IllegalArgumentException unless {@code normal} is horizontal
     */
    public static Frame wall(Dir normal) {
        if (!normal.horizontal()) {
            throw new IllegalArgumentException("a wall's normal is horizontal, was " + normal);
        }
        return new Frame(normal);
    }

    /** effects: returns whether this is a wall rather than the floor */
    public boolean isWall() {
        return up != Dir.UP;
    }

    /** effects: returns the direction into the block the plane rests on */
    public Dir support() {
        return up.opposite();
    }

    /** effects: returns the world direction a planar direction runs in, in this frame */
    public Dir world(Planar p) {
        if (!isWall()) {
            return switch (p) {
                case TOP -> Dir.NORTH;
                case BOTTOM -> Dir.SOUTH;
                case LEFT -> Dir.WEST;
                case RIGHT -> Dir.EAST;
            };
        }
        return switch (p) {
            case TOP -> Dir.UP;
            case BOTTOM -> Dir.DOWN;
            case LEFT -> support().counterClockwise();
            case RIGHT -> support().clockwise();
        };
    }

    /** effects: returns the planar direction {@code d} is in this frame, or empty if it leaves the plane */
    public Optional<Planar> planar(Dir d) {
        for (Planar p : Planar.values()) {
            if (world(p) == d) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
