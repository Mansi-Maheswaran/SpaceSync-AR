package com.truefitar.furniture;

import com.google.ar.core.Anchor;

/**
 * TrueFit AR — Module 2: 3D Furniture Placement & Manipulation
 *
 * Represents one furniture model placed in the AR scene.
 *
 * Deliberately has NO scale/size field. Per the project's design decision,
 * furniture size always reflects the model's true real-world dimensions —
 * users can reposition and reorient an item, but cannot resize it.
 */
public class FurnitureItem {

    private final String id;
    private final String modelName;
    private Anchor anchor;
    private float rotationDegrees;

    public FurnitureItem(String id, String modelName, Anchor anchor) {
        this.id = id;
        this.modelName = modelName;
        this.anchor = anchor;
        this.rotationDegrees = 0f;
    }

    public String getId() {
        return id;
    }

    public String getModelName() {
        return modelName;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    /** Used when relocating: the old anchor is detached and replaced with a new one. */
    public void setAnchor(Anchor newAnchor) {
        if (this.anchor != null) {
            this.anchor.detach();
        }
        this.anchor = newAnchor;
    }

    public float getRotationDegrees() {
        return rotationDegrees;
    }

    /** Adds to the current rotation — used for the live two-finger twist gesture. */
    public void rotateBy(float deltaDegrees) {
        this.rotationDegrees = (this.rotationDegrees + deltaDegrees) % 360f;
    }

    /**
     * Sets rotation to an absolute value — used when auto-aligning the item
     * to a two-corner placement (see PlacementGeometryUtils), rather than
     * accumulating from a gesture.
     */
    public void setRotationDegrees(float degrees) {
        this.rotationDegrees = degrees % 360f;
    }

    public void detach() {
        if (anchor != null) {
            anchor.detach();
        }
    }
}