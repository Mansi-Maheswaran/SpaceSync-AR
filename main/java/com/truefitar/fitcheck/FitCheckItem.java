package com.truefitar.fitcheck;

import com.google.ar.core.Anchor;

/**
 * TrueFit AR - Module 3: Dimension-Verified Fit Check
 *
 * One "will this fit?" piece of furniture, drawn as a box (or a few boxes,
 * depending on its FurnitureType) at the exact real-world size the user typed.
 *
 * Size can only be set by typing dimensions - there is deliberately NO
 * pinch-to-resize gesture, so the fit result can never be faked.
 *
 * rotationDegrees is a WORLD yaw (rotation about the vertical axis). The
 * renderer only uses the anchor's position, not its orientation, because the
 * orientation of an ARCore hit pose depends on where the user was standing.
 */
public class FitCheckItem {

    private final String id;
    private Anchor anchor;
    private float rotationDegrees;
    private FurnitureType type = FurnitureType.OTHER;

    /** All in meters. */
    private final float lengthMeters;
    private final float heightMeters;
    private final float widthMeters;

    private final String referencePhotoUri;

    public FitCheckItem(
            String id,
            Anchor anchor,
            float lengthMeters,
            float heightMeters,
            float widthMeters,
            String referencePhotoUri) {
        this.id = id;
        this.anchor = anchor;
        this.rotationDegrees = 0f;
        this.lengthMeters = lengthMeters;
        this.heightMeters = heightMeters;
        this.widthMeters = widthMeters;
        this.referencePhotoUri = referencePhotoUri;
    }

    public String getId() { return id; }

    public Anchor getAnchor() { return anchor; }

    public void setAnchor(Anchor newAnchor) {
        if (this.anchor != null) {
            this.anchor.detach();
        }
        this.anchor = newAnchor;
    }

    public FurnitureType getType() { return type; }

    public void setType(FurnitureType type) {
        this.type = (type == null) ? FurnitureType.OTHER : type;
    }

    public float getRotationDegrees() { return rotationDegrees; }

    public void rotateBy(float deltaDegrees) {
        this.rotationDegrees = (this.rotationDegrees + deltaDegrees) % 360f;
    }

    public void setRotationDegrees(float degrees) {
        this.rotationDegrees = degrees % 360f;
    }

    public float getLengthMeters() { return lengthMeters; }

    public float getHeightMeters() { return heightMeters; }

    public float getWidthMeters() { return widthMeters; }

    public String getReferencePhotoUri() { return referencePhotoUri; }

    public void detach() {
        if (anchor != null) {
            anchor.detach();
        }
    }
}
