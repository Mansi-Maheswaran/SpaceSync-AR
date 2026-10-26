package com.truefitar.fitcheck;

import com.google.ar.core.Pose;

/**
 * TrueFit AR - the "does it fit?" maths.
 *
 * The gap is the segment between the two measured points A and B. Everything
 * is worked out in the horizontal (floor) plane using WORLD coordinates:
 *
 *   g = unit vector from A to B          (along the gap)
 *   n = g turned 90 degrees              (across the gap)
 *   u = the item's long axis after rotating by its yaw
 *   v = the item's short axis
 *
 *   extent  = how much room the item takes up ALONG the gap
 *           = length * |u.g| + width * |v.g|
 *   sizeFits = extent <= gap
 *   inside   = the item sits between A and B (not sticking out past an end)
 */
public final class FitEvaluator {

    private FitEvaluator() {}

    public static final class Result {
        public boolean sizeFits;
        /** False when the measured gap is (almost) vertical, e.g. measured up a wall. */
        public boolean insideKnown;
        public boolean inside;
        /** Straight-line gap A-B in metres (what the user saw while measuring). */
        public float gap;
        /** Room the item takes along the gap in its current orientation. */
        public float extent;
        /** Item centre, measured from A along the gap, metres. */
        public float along;
        /** Item centre, measured sideways from the gap line, metres. */
        public float lateral;
        /** Angle of the item's long axis relative to the gap, degrees. */
        public float relAngleDeg;
    }

    /** Yaw (degrees) that makes an item's long side lie along the line A -> B. */
    public static float alignedYawDegrees(Pose a, Pose b) {
        float dx = b.tx() - a.tx();
        float dz = b.tz() - a.tz();
        if (Math.hypot(dx, dz) < 0.05) return 0f;
        return (float) -Math.toDegrees(Math.atan2(dz, dx));
    }

    public static Result evaluate(Pose a, Pose b, FitCheckItem item) {
        Result r = new Result();

        float dx = b.tx() - a.tx();
        float dy = b.ty() - a.ty();
        float dz = b.tz() - a.tz();
        float gapH = (float) Math.hypot(dx, dz);
        r.gap = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float length = item.getLengthMeters();
        float width = item.getWidthMeters();

        if (gapH < 0.10f) {
            // Gap is vertical (measured up a wall): only the plain length comparison is meaningful.
            r.insideKnown = false;
            r.inside = true;
            r.extent = length;
            r.sizeFits = r.extent <= r.gap + 0.005f;
            return r;
        }

        float gx = dx / gapH;
        float gz = dz / gapH;
        float nx = -gz;
        float nz = gx;

        double th = Math.toRadians(item.getRotationDegrees());
        float ux = (float) Math.cos(th);
        float uz = (float) -Math.sin(th);
        float vx = (float) Math.sin(th);
        float vz = (float) Math.cos(th);

        r.extent = length * Math.abs(ux * gx + uz * gz) + width * Math.abs(vx * gx + vz * gz);
        r.sizeFits = r.extent <= r.gap + 0.005f;

        Pose c = item.getAnchor().getPose();
        float cx = c.tx() - a.tx();
        float cz = c.tz() - a.tz();
        r.along = cx * gx + cz * gz;
        r.lateral = cx * nx + cz * nz;

        r.relAngleDeg = (float) Math.toDegrees(Math.atan2(ux * nx + uz * nz, ux * gx + uz * gz));

        r.insideKnown = true;
        float tol = 0.03f;
        r.inside = (r.along - r.extent / 2f >= -tol)
                && (r.along + r.extent / 2f <= gapH + tol)
                && Math.abs(r.lateral) <= 1.0f;
        return r;
    }

    /** True if a world point (floor tap) lies on top of the item's footprint. */
    public static boolean contains(FitCheckItem item, Pose tap) {
        Pose c = item.getAnchor().getPose();
        float dx = tap.tx() - c.tx();
        float dz = tap.tz() - c.tz();
        double th = Math.toRadians(item.getRotationDegrees());
        float lx = (float) (dx * Math.cos(th) - dz * Math.sin(th));
        float lz = (float) (dx * Math.sin(th) + dz * Math.cos(th));
        return Math.abs(lx) <= item.getLengthMeters() / 2f + 0.05f
                && Math.abs(lz) <= item.getWidthMeters() / 2f + 0.05f;
    }
}
