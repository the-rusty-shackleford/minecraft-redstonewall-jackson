/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.redstonewalljackson;

import com.chunkworks.redstonewalljackson.domain.Dir;
import com.chunkworks.redstonewalljackson.domain.Frame;
import com.chunkworks.redstonewalljackson.domain.Planar;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The domain's directions and frames against the game's, and boxes drawn in a plane's own coordinates. */
final class Frames {
    private Frames() {}

    /** effects: returns the game's direction named as the domain's */
    static Direction dir(Dir d) {
        return Direction.valueOf(d.name());
    }

    /** effects: returns the domain's direction named as the game's */
    static Dir dir(Direction d) {
        return Dir.valueOf(d.name());
    }

    /** effects: returns the frame whose outward normal is {@code up}: a wall's face, the ceiling (down) or the floor (up) */
    static Frame of(Direction up) {
        return Frame.facing(dir(up));
    }

    /** effects: returns the world direction planar {@code p} runs in, in frame {@code f} */
    static Direction world(Frame f, Planar p) {
        return dir(f.world(p));
    }

    /**
     * effects: returns the world box of a box given in the plane's own coordinates, each 0..16:
     * {@code u} from the LEFT edge toward RIGHT, {@code v} from the BOTTOM edge toward TOP, and
     * {@code t} from the support face outward along up. On the floor frame that is x, 16 - z and y.
     */
    static VoxelShape box(Frame f, double u1, double v1, double t1, double u2, double v2, double t2) {
        double[] min = new double[3], max = new double[3];
        place(min, max, f.world(Planar.RIGHT), u1, u2);
        place(min, max, f.world(Planar.TOP), v1, v2);
        place(min, max, f.up(), t1, t2);
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    private static void place(double[] min, double[] max, Dir along, double c1, double c2) {
        int axis = along.x != 0 ? 0 : along.y != 0 ? 1 : 2;
        boolean positive = along.x + along.y + along.z > 0;
        double a = positive ? c1 : 16 - c1, b = positive ? c2 : 16 - c2;
        min[axis] = Math.min(a, b);
        max[axis] = Math.max(a, b);
    }
}
