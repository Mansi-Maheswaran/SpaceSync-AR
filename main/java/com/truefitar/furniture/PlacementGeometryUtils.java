package com.truefitar.furniture;

import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

/**
 * TrueFit AR — Module 2: 3D Furniture Placement & Manipulation
 *
 * Math helpers for the two-corner placement flow: given two points the
 * user tapped on a detected floor plane, works out (a) the midpoint
 * between them, so the furniture sits centered within the span the user
 * marked, and (b) the angle of the line between them, so the item can
 * be auto-rotated to align with that line — e.g. tapping the two ends
 * of a wall so the couch sits centered along it, facing the right way.
 *
 * Never changes size — only position and rotation.
 */
public class PlacementGeometryUtils {

    /**
     * Returns the angle (in degrees) of the line from pointA to pointB,
     * measured around the vertical (Y) axis — the same axis
     * FurnitureRenderer rotates models around. Pass this into
     * FurnitureItem.setRotationDegrees().
     */
    public static float angleBetweenDegrees(HitResult pointA, HitResult pointB) {
        Pose poseA = pointA.getHitPose();
        Pose poseB = pointB.getHitPose();

        float dx = poseB.tx() - poseA.tx();
        float dz = poseB.tz() - poseA.tz();

        return (float) Math.toDegrees(Math.atan2(dz, dx));
    }

    /**
     * Returns a Pose sitting exactly halfway between the two tapped
     * points, so furniture placed there is centered within the span the
     * user marked out rather than pinned to just the first tap.
     * Reuses pointA's orientation (its tilt relative to the detected
     * plane) since both points should be on the same flat surface.
     */
    public static Pose midpointPose(HitResult pointA, HitResult pointB) {
        Pose poseA = pointA.getHitPose();
        Pose poseB = pointB.getHitPose();

        float midX = (poseA.tx() + poseB.tx()) / 2f;
        float midY = (poseA.ty() + poseB.ty()) / 2f;
        float midZ = (poseA.tz() + poseB.tz()) / 2f;

        return new Pose(new float[] {midX, midY, midZ}, poseA.getRotationQuaternion());
    }
}