package com.truefitar.fitcheck;

import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TrueFit AR — Module 3: Dimension-Verified Fit Check
 *
 * Manages all fit-check placeholder boxes currently in the AR scene.
 * Completely independent of Module 2's FurniturePlacementManager — no
 * shared state, so nothing here can affect Module 2's behavior.
 */
public class FitCheckManager {

    private static final float GRAB_RADIUS_METERS = 0.35f;

    private final List<FitCheckItem> items = new ArrayList<>();

    /**
     * Places a new fit-check box at a hit-test location, sized to the
     * given real-world dimensions (in meters).
     */
    public FitCheckItem placeFitCheckItem(
            HitResult hitResult,
            float lengthMeters,
            float heightMeters,
            float widthMeters,
            String referencePhotoUri) {
        String id = UUID.randomUUID().toString();
        FitCheckItem item =
                new FitCheckItem(
                        id,
                        hitResult.createAnchor(),
                        lengthMeters,
                        heightMeters,
                        widthMeters,
                        referencePhotoUri);
        items.add(item);
        return item;
    }

    public void moveItem(FitCheckItem item, HitResult newHitResult) {
        item.setAnchor(newHitResult.createAnchor());
    }

    public void rotateItem(FitCheckItem item, float deltaDegrees) {
        item.rotateBy(deltaDegrees);
    }

    public void removeItem(FitCheckItem item) {
        item.detach();
        items.remove(item);
    }

    public void clearAll() {
        for (FitCheckItem item : items) {
            item.detach();
        }
        items.clear();
    }

    public List<FitCheckItem> getItems() {
        return items;
    }

    public FitCheckItem findItemNear(HitResult hitResult) {
        Pose tapPose = hitResult.getHitPose();
        FitCheckItem closest = null;
        float closestDist = GRAB_RADIUS_METERS;

        for (FitCheckItem item : items) {
            Pose itemPose = item.getAnchor().getPose();
            float dx = tapPose.tx() - itemPose.tx();
            float dy = tapPose.ty() - itemPose.ty();
            float dz = tapPose.tz() - itemPose.tz();
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < closestDist) {
                closestDist = dist;
                closest = item;
            }
        }
        return closest;
    }
}