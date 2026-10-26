package com.truefitar.measurement;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import java.util.ArrayList;
import java.util.List;

/**
 * TrueFit AR — Module 1: AR Plane Detection & Measurement
 *
 * Handles tap-to-measure functionality. Tracks up to two anchor points
 * placed by the user via hit-testing, and calculates the real-world
 * straight-line distance between them using ARCore's tracked poses.
 *
 * Usage:
 *   1. On each screen tap, run a hit-test against the current Frame.
 *   2. Pass the resulting HitResult to addPoint().
 *   3. Once isComplete() is true, call getFormattedDistance() to show the result.
 *   4. Call reset() to clear and start a new measurement (addPoint() also
 *      auto-resets once two points already exist, so a third tap starts fresh).
 */
public class MeasurementManager {

    private static final String TAG = "MeasurementManager";

    public enum Unit { METERS, FEET }

    private final List<Anchor> points = new ArrayList<>();
    private Unit unit = Unit.METERS;

    /**
     * Adds a measurement point from a hit-test result.
     * If two points already exist, clears them first so the new tap
     * begins a fresh measurement.
     *
     * @param hitResult the ARCore hit-test result from the user's tap
     * @return the anchor created for this point (render a marker here)
     */
    public Anchor addPoint(HitResult hitResult) {
        if (points.size() >= 2) {
            reset();
        }

        Trackable trackable = hitResult.getTrackable();
        if (trackable.getTrackingState() != TrackingState.TRACKING) {
            Log.w(TAG, "Trackable not currently tracking; point may be inaccurate.");
        }

        Anchor anchor = hitResult.createAnchor();
        points.add(anchor);
        return anchor;
    }

    /** True once two points have been placed and a distance can be shown. */
    public boolean isComplete() {
        return points.size() == 2;
    }

    public List<Anchor> getPoints() {
        return points;
    }

    /**
     * Straight-line (Euclidean) distance between the two placed anchors, in meters.
     * Returns -1 if fewer than two points have been placed.
     */
    public float getDistanceMeters() {
        if (points.size() < 2) return -1f;

        Pose poseA = points.get(0).getPose();
        Pose poseB = points.get(1).getPose();

        float dx = poseA.tx() - poseB.tx();
        float dy = poseA.ty() - poseB.ty();
        float dz = poseA.tz() - poseB.tz();

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    /** Toggles between meters and feet, useful for a single UI button. */
    public void toggleUnit() {
        unit = (unit == Unit.METERS) ? Unit.FEET : Unit.METERS;
    }

    /** Returns a formatted, human-readable distance string in the selected unit. */
    public String getFormattedDistance() {
        float meters = getDistanceMeters();
        if (meters < 0) return "--";

        if (unit == Unit.METERS) {
            return String.format("%.2f m", meters);
        } else {
            float feet = meters * 3.28084f;
            return String.format("%.2f ft", feet);
        }
    }

    /** Detaches existing anchors and clears the current measurement. */
    public void reset() {
        for (Anchor anchor : points) {
            anchor.detach();
        }
        points.clear();
    }
}