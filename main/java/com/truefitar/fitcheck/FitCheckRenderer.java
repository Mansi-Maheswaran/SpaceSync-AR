package com.truefitar.fitcheck;

import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrueFit AR - draws the fit-check furniture, the selection plate and the
 * measurement markers. Everything is built from ONE mesh (models/unit_cube.obj)
 * scaled and coloured differently, so no extra 3D models are needed.
 *
 * Assets needed in app/src/main/assets/models/:
 *   unit_cube.obj / unit_cube.mtl           (already in your project)
 *   tex_sofa.png, tex_sofa_accent.png, tex_chair*.png, tex_cupboard*.png,
 *   tex_other*.png, tex_marker.png          (new - supplied with this update)
 *
 * Only the anchor's POSITION is used (not its orientation). The item's own
 * rotationDegrees is a world-space yaw, so rotation is stable no matter where
 * the user was standing when they tapped.
 */
public class FitCheckRenderer {

    private static final String TAG = "FitCheckRenderer";

    private Mesh cubeMesh;
    private final Map<String, Texture> textures = new HashMap<>();
    private boolean meshLoaded = false;

    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private boolean ensureMesh(SampleRender render) {
        if (meshLoaded) return true;
        try {
            cubeMesh = Mesh.createFromAsset(render, "models/unit_cube.obj");
            meshLoaded = true;
        } catch (IOException e) {
            Log.e(TAG, "unit_cube.obj missing", e);
        }
        return meshLoaded;
    }

    private Texture texture(SampleRender render, String key) {
        Texture t = textures.get(key);
        if (t != null) return t;
        try {
            t = Texture.createFromAsset(
                    render,
                    "models/" + key + ".png",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB);
            textures.put(key, t);
        } catch (IOException e) {
            Log.e(TAG, "Missing texture " + key, e);
        }
        return t;
    }

    /** Draws every placed item. The selected one also gets an orange plate under it. */
    public void drawItems(
            SampleRender render,
            List<FitCheckItem> items,
            FitCheckItem selected,
            Shader shader,
            float[] viewMatrix,
            float[] projectionMatrix,
            Framebuffer fb) {

        if (items.isEmpty() || !ensureMesh(render)) return;

        for (FitCheckItem item : items) {
            Anchor anchor = item.getAnchor();
            if (anchor == null || anchor.getTrackingState() != TrackingState.TRACKING) continue;
            Pose p = anchor.getPose();

            float L = item.getLengthMeters();
            float H = item.getHeightMeters();
            float W = item.getWidthMeters();
            FurnitureType type = item.getType();

            Texture main = texture(render, "tex_" + type.getTextureKey());
            Texture accent = texture(render, "tex_" + type.getTextureKey() + "_accent");
            if (main == null) continue;
            if (accent == null) accent = main;

            for (FurnitureType.Part part : type.getParts()) {
                drawBox(render, shader, part.accent ? accent : main,
                        viewMatrix, projectionMatrix, fb,
                        p.tx(), p.ty(), p.tz(), item.getRotationDegrees(),
                        part.px * L, part.py * H, part.pz * W,
                        part.sx * L, part.sy * H, part.sz * W);
            }

            if (item == selected) {
                Texture marker = texture(render, "tex_marker");
                if (marker != null) {
                    drawBox(render, shader, marker, viewMatrix, projectionMatrix, fb,
                            p.tx(), p.ty() + 0.002f, p.tz(), item.getRotationDegrees(),
                            0f, 0f, 0f,
                            L + 0.12f, 0.004f, W + 0.12f);
                }
            }
        }
    }

    /** Draws the two measurement markers and the line between them. */
    public void drawMeasurement(
            SampleRender render,
            List<Anchor> points,
            Shader shader,
            float[] viewMatrix,
            float[] projectionMatrix,
            Framebuffer fb) {

        if (points.isEmpty() || !ensureMesh(render)) return;
        Texture marker = texture(render, "tex_marker");
        if (marker == null) return;

        for (Anchor a : points) {
            if (a.getTrackingState() != TrackingState.TRACKING) continue;
            Pose p = a.getPose();
            drawBox(render, shader, marker, viewMatrix, projectionMatrix, fb,
                    p.tx(), p.ty(), p.tz(), 0f,
                    0f, 0f, 0f,
                    0.05f, 0.12f, 0.05f);
        }

        if (points.size() == 2
                && points.get(0).getTrackingState() == TrackingState.TRACKING
                && points.get(1).getTrackingState() == TrackingState.TRACKING) {
            Pose a = points.get(0).getPose();
            Pose b = points.get(1).getPose();
            float dx = b.tx() - a.tx();
            float dz = b.tz() - a.tz();
            float horizontal = (float) Math.hypot(dx, dz);
            if (horizontal > 0.02f) {
                float yaw = (float) -Math.toDegrees(Math.atan2(dz, dx));
                drawBox(render, shader, marker, viewMatrix, projectionMatrix, fb,
                        (a.tx() + b.tx()) / 2f, Math.min(a.ty(), b.ty()) + 0.005f,
                        (a.tz() + b.tz()) / 2f, yaw,
                        0f, 0f, 0f,
                        horizontal, 0.01f, 0.02f);
            }
        }
    }

    private void drawBox(
            SampleRender render, Shader shader, Texture tex,
            float[] viewMatrix, float[] projectionMatrix, Framebuffer fb,
            float tx, float ty, float tz, float yawDegrees,
            float ox, float oy, float oz,
            float sx, float sy, float sz) {

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, tx, ty, tz);
        Matrix.rotateM(modelMatrix, 0, yawDegrees, 0f, 1f, 0f);
        Matrix.translateM(modelMatrix, 0, ox, oy, oz);
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz);

        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        shader.setMat4("u_ModelView", modelViewMatrix);
        shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
        shader.setTexture("u_AlbedoTexture", tex);
        render.draw(cubeMesh, shader, fb);
    }
}
