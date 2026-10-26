package com.truefitar.furniture;

import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TrueFit AR — Module 2: 3D Furniture Placement & Manipulation
 *
 * Manages all furniture items currently placed in the AR scene.
 * No resize/scale method exists anywhere in this class — see
 * FurnitureItem's class comment for why.
 */
public class FurniturePlacementManager {

    private static final float GRAB_RADIUS_METERS = 0.35f;

    private final List<FurnitureItem> items = new ArrayList<>();

    /** Places a new item directly at a hit-test result (single-tap placement). */
    public FurnitureItem placeFurniture(HitResult hitResult, String modelName) {
        String id = UUID.randomUUID().toString();
        FurnitureItem item = new FurnitureItem(id, modelName, hitResult.createAnchor());
        items.add(item);
        return item;
    }

    /**
     * Places a new item at an arbitrary computed Pose (e.g. the midpoint
     * between two tapped corners) rather than directly at a hit-test
     * result. Needs the ARCore Session to create the anchor.
     */
    public FurnitureItem placeFurnitureAtPose(Pose pose, String modelName, Session session) {
        String id = UUID.randomUUID().toString();
        FurnitureItem item = new FurnitureItem(id, modelName, session.createAnchor(pose));
        items.add(item);
        return item;
    }

    /** Moves an existing item directly to a new hit-test result. */
    public void moveItem(FurnitureItem item, HitResult newHitResult) {
        item.setAnchor(newHitResult.createAnchor());
    }

    /** Moves an existing item to an arbitrary computed Pose (e.g. a midpoint). */
    public void moveItemToPose(FurnitureItem item, Pose pose, Session session) {
        item.setAnchor(session.createAnchor(pose));
    }

    public void rotateItem(FurnitureItem item, float deltaDegrees) {
        item.rotateBy(deltaDegrees);
    }

    public void removeItem(FurnitureItem item) {
        item.detach();
        items.remove(item);
    }

    public void clearAll() {
        for (FurnitureItem item : items) {
            item.detach();
        }
        items.clear();
    }

    public List<FurnitureItem> getItems() {
        return items;
    }

    /** Finds the placed item closest to a tap, if within GRAB_RADIUS_METERS. */
    public FurnitureItem findItemNear(HitResult hitResult) {
        Pose tapPose = hitResult.getHitPose();
        FurnitureItem closest = null;
        float closestDist = GRAB_RADIUS_METERS;

        for (FurnitureItem item : items) {
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