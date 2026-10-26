package com.truefitar.fitcheck;

/**
 * TrueFit AR - the furniture categories offered in the Fit Check picker.
 *
 * Every category is drawn as a few boxes ("parts") built from the single
 * unit cube, so no extra 3D models are needed and the overall footprint is
 * always exactly the length x height x width the user typed in.
 *
 * Part numbers are FRACTIONS of the overall box:
 *   px, pz = centre of the part, as a fraction of length / width (-0.5 .. 0.5)
 *   py     = bottom of the part, as a fraction of height (0 .. 1)
 *   sx, sy, sz = size of the part, as a fraction of length / height / width
 *   accent = use the second (accent) colour
 * The back of the furniture is at -z, the front at +z.
 */
public enum FurnitureType {

    SOFA("Sofa", "🛋", "sofa", 200, 85, 90,
            new Part[] {
                new Part(0f, 0f, 0.075f, 0.86f, 0.42f, 0.85f, true),   // seat cushion
                new Part(0f, 0f, -0.40f, 1.00f, 1.00f, 0.20f, false),  // backrest
                new Part(-0.46f, 0f, 0.10f, 0.08f, 0.65f, 0.80f, false), // left arm
                new Part(0.46f, 0f, 0.10f, 0.08f, 0.65f, 0.80f, false)   // right arm
            }),

    CHAIR("Chair", "🪑", "chair", 50, 90, 50,
            new Part[] {
                new Part(0f, 0.45f, 0.05f, 1.00f, 0.08f, 0.90f, true),   // seat
                new Part(0f, 0.45f, -0.44f, 1.00f, 0.55f, 0.12f, false), // backrest
                new Part(-0.42f, 0f, -0.40f, 0.12f, 0.45f, 0.12f, false), // legs
                new Part(0.42f, 0f, -0.40f, 0.12f, 0.45f, 0.12f, false),
                new Part(-0.42f, 0f, 0.40f, 0.12f, 0.45f, 0.12f, false),
                new Part(0.42f, 0f, 0.40f, 0.12f, 0.45f, 0.12f, false)
            }),

    CUPBOARD("Cupboard", "🗄", "cupboard", 90, 180, 50,
            new Part[] {
                new Part(0f, 0f, 0f, 1.00f, 1.00f, 1.00f, false),        // body
                new Part(-0.25f, 0.04f, 0.5f, 0.44f, 0.92f, 0.03f, true), // left door
                new Part(0.25f, 0.04f, 0.5f, 0.44f, 0.92f, 0.03f, true)   // right door
            }),

    OTHER("Other (box)", "📦", "other", 100, 50, 50,
            new Part[] {
                new Part(0f, 0f, 0f, 1f, 1f, 1f, false)
            });

    public static final class Part {
        public final float px, py, pz, sx, sy, sz;
        public final boolean accent;

        Part(float px, float py, float pz, float sx, float sy, float sz, boolean accent) {
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.accent = accent;
        }
    }

    private final String label;
    private final String icon;
    private final String textureKey;
    private final int defaultLengthCm;
    private final int defaultHeightCm;
    private final int defaultWidthCm;
    private final Part[] parts;

    FurnitureType(String label, String icon, String textureKey,
                  int lengthCm, int heightCm, int widthCm, Part[] parts) {
        this.label = label;
        this.icon = icon;
        this.textureKey = textureKey;
        this.defaultLengthCm = lengthCm;
        this.defaultHeightCm = heightCm;
        this.defaultWidthCm = widthCm;
        this.parts = parts;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getTextureKey() { return textureKey; }
    public int getDefaultLengthCm() { return defaultLengthCm; }
    public int getDefaultHeightCm() { return defaultHeightCm; }
    public int getDefaultWidthCm() { return defaultWidthCm; }
    public Part[] getParts() { return parts; }

    /** Safe lookup used when reading saved rooms. */
    public static FurnitureType fromName(String name) {
        for (FurnitureType t : values()) {
            if (t.name().equals(name)) return t;
        }
        return OTHER;
    }
}
