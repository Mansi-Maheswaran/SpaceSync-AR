package com.truefitar.furniture;

import android.opengl.Matrix;

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
 * TrueFit AR — Module 2: 3D Furniture Placement & Manipulation
 *
 * Loads and caches furniture meshes AND a matching color texture per
 * model, and draws every placed FurnitureItem at its anchor's real-world
 * position, rotated by the item's stored rotation.
 *
 * Each model needs, under app/src/main/assets/models/:
 *   <name>.obj
 *   <name>.mtl
 *   <name>.png   <- a small solid-color image (see integration guide for
 *                    how to make one in Paint). This replaces the earlier
 *                    version that reused the sample's placeholder texture.
 *
 * Reuses the SAME Shader instance your HelloAR sample already created
 * (virtualObjectShader) — only the Mesh and Texture swap per item.
 *
 * IMPORTANT: before trusting this file, search your HelloArActivity.java
 * for "Texture.createFromAsset" and confirm the argument order/types
 * below match exactly — this is the one call in this file not yet
 * directly confirmed against your project's actual code.
 */
public class FurnitureRenderer {

    private final Map<String, Mesh> meshCache = new HashMap<>();
    private final Map<String, Texture> textureCache = new HashMap<>();

    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    /**
     * Loads (and caches) the mesh + texture for a named model. Safe to
     * call every frame — only touches disk the first time per name.
     */
    private void ensureModelLoaded(SampleRender render, String modelName) throws IOException {
        if (!meshCache.containsKey(modelName)) {
            meshCache.put(modelName, Mesh.createFromAsset(render, "models/" + modelName + ".obj"));
        }
        if (!textureCache.containsKey(modelName)) {
            Texture texture =
                    Texture.createFromAsset(
                            render,
                            "models/" + modelName + ".png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);
            textureCache.put(modelName, texture);
        }
    }

    /**
     * Draws every placed furniture item. Call once per frame from
     * onDrawFrame, right after the sample draws its own default object.
     */
    public void drawItems(
            SampleRender render,
            List<FurnitureItem> items,
            Shader sharedShader,
            float[] viewMatrix,
            float[] projectionMatrix,
            Framebuffer targetFramebuffer) {

        for (FurnitureItem item : items) {
            try {
                ensureModelLoaded(render, item.getModelName());
            } catch (IOException e) {
                // Model files missing or unreadable — skip this item rather than crashing.
                continue;
            }

            item.getAnchor().getPose().toMatrix(modelMatrix, 0);
            // Rotate around the local up-axis only — position and size stay untouched.
            Matrix.rotateM(modelMatrix, 0, item.getRotationDegrees(), 0f, 1f, 0f);

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(
                    modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

            sharedShader.setMat4("u_ModelView", modelViewMatrix);
            sharedShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            sharedShader.setTexture("u_AlbedoTexture", textureCache.get(item.getModelName()));

            render.draw(meshCache.get(item.getModelName()), sharedShader, targetFramebuffer);
        }
    }
}