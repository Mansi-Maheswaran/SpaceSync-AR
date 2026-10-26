/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.truefitar.fitcheck.FitCheckItem;
import com.truefitar.fitcheck.FitCheckManager;
import com.truefitar.fitcheck.FitCheckRenderer;
import com.truefitar.fitcheck.FitEvaluator;
import com.truefitar.fitcheck.FurnitureType;
import com.truefitar.storage.RoomStore;
import com.truefitar.storage.SavedRoom;
import com.truefitar.ui.Dialogs;
import com.truefitar.ui.UiKit;
import android.graphics.drawable.GradientDrawable;
import java.util.Locale;
import com.truefitar.furniture.FurnitureItem;
import com.truefitar.furniture.FurniturePlacementManager;
import com.truefitar.furniture.FurnitureRenderer;
import com.truefitar.furniture.PlacementGeometryUtils;
import com.truefitar.furniture.RotationGestureDetector;
import com.truefitar.measurement.MeasurementManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private final MeasurementManager measurementManager = new MeasurementManager();

  private final FurniturePlacementManager furnitureManager = new FurniturePlacementManager();
  private FurnitureItem draggedItem = null;
  private RotationGestureDetector rotationDetector;

  private final FurnitureRenderer furnitureRenderer = new FurnitureRenderer();

  private HitResult pendingCornerA = null;
  private final FitCheckManager fitCheckManager = new FitCheckManager();
  private final FitCheckRenderer fitCheckRenderer = new FitCheckRenderer();
  private boolean placingFitCheckItem = false;

  private boolean placingFurniture = false; // default = measurement mode
  private Toast currentToast;
  private String fitVerdictText = null; // last fit / no-fit verdict, shown on screen


  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

  // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
  // constants.

  private float pendingLengthMeters = 0f;
  private float pendingHeightMeters = 0f;
  private float pendingWidthMeters = 0f;
  private static final float[] sphericalHarmonicFactors = {
    0.282095f,
    -0.325735f,
    0.325735f,
    -0.325735f,
    0.273137f,
    -0.273137f,
    0.078848f,
    -0.273137f,
    0.136569f,
  };

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // Assumed distance from the device camera to the surface on which user will try to place objects.
  // This value affects the apparent scale of objects while the tracking method of the
  // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
  // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
  // values for AR experiences where users are expected to place objects on surfaces close to the
  // camera. Use larger values for experiences where the user will likely be standing and trying to
  // place an object on the ground or floor in front of them.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual object (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;

  private final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    rotationDetector = new RotationGestureDetector(deltaDegrees -> {
      if (mode == Mode.ADJUST && selectedItem != null) {
        selectedItem.rotateBy(-deltaDegrees); // two-finger twist rotates the selected furniture
      } else if (draggedItem != null) {
        furnitureManager.rotateItem(draggedItem, deltaDegrees);
      }
    });
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/* context= */ this);

    // Set up touch listener.
    tapHelper = new TapHelper(/* context= */ this);
    surfaceView.setOnTouchListener((v, event) -> {
      rotationDetector.onTouchEvent(event);
      if (mode == Mode.ADJUST) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (event.getPointerCount() > 1) multiTouch = true;
        if (!multiTouch && event.getPointerCount() == 1 && action == MotionEvent.ACTION_MOVE) {
          dragX = event.getX();
          dragY = event.getY();
          dragging = true; // one-finger drag moves the selected furniture
        } else if (multiTouch
            || action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL) {
          dragging = false;
        }
      }
      return tapHelper.onTouch(v, event);
    });

    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());

    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
            popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
            popup.inflate(R.menu.settings_menu);
            popup.show();
          }
        });
    setupTrueFitUi();
  }

  /** Menu button to launch feature specific settings. */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.instant_placement_settings) {
      launchInstantPlacementSettingsMenuDialog();
      return true;
    }
    return false;
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        // Always check the latest availability.
        Availability availability = ArCoreApk.getInstance().checkAvailability(this);

        // In all other cases, try to install ARCore and handle installation failures.
        if (availability != Availability.SUPPORTED_INSTALLED) {
          switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            case INSTALL_REQUESTED:
              installRequested = true;
              return;
            case INSTALLED:
              break;
          }
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      configureSession();
      // To record a live camera session for later playback, call
      // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
      // session instead of using the live camera feed, call
      // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
      // learn more about recording and playback, see:
      // https://developers.google.com/ar/develop/java/recording-and-playback
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

      cubemapFilter =
          new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      dfgTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /* useMipmaps= */ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
          ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /* level= */ 0,
          GLES30.GL_RG16F,
          /* width= */ dfgResolution,
          /* height= */ dfgResolution,
          /* border= */ 0,
          GLES30.GL_RG,
          GLES30.GL_HALF_FLOAT,
          buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      // Point cloud
      pointCloudShader =
          Shader.createFromAssets(
                  render,
                  "shaders/point_cloud.vert",
                  "shaders/point_cloud.frag",
                  /* defines= */ null)
              .setVec4(
                  "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
              .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
          new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
          new Mesh(
              render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);

      // Virtual object to render (ARCore pawn)
      virtualObjectAlbedoTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectAlbedoInstantPlacementTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo_instant_placement.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      Texture virtualObjectPbrTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_roughness_metallic_ao.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.LINEAR);

      virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj");
      virtualObjectShader =
          Shader.createFromAssets(
                  render,
                  "shaders/environmental_hdr.vert",
                  "shaders/environmental_hdr.frag",
                  /* defines= */ new HashMap<String, String>() {
                    {
                      put(
                          "NUMBER_OF_MIPMAP_LEVELS",
                          Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                    }
                  })
              .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
              .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
              .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
              .setTexture("u_DfgTexture", dfgTexture);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
          new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    // Obtain the current frame from the AR Session. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }
    Camera camera = frame.getCamera();

    // Update BackgroundRenderer state to match the depth settings.
    try {
      backgroundRenderer.setUseDepthVisualization(
          render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame);

    if (camera.getTrackingState() == TrackingState.TRACKING
        && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage16Bits()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
    }

    // Handle one tap per frame.
    handleTap(frame, camera);
    handleDrag(frame, camera);
    processRequests();
    updateAdjustStatus();

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // Show a message based on whether tracking has failed, if planes are detected, and if the user
    // has placed any objects.
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    } else if (hasTrackingPlane()) {
      message = null;
    } else {
      message = SEARCHING_PLANE_MESSAGE;
    }
    showScanHint(message);

    // -- Draw background

    if (frame.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

    // -- Draw non-occluded virtual objects (planes, point cloud)

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0);

    // Visualize tracked points.
    // Use try-with-resources to automatically release the point cloud.
    try (PointCloud pointCloud = frame.acquirePointCloud()) {
      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.getPoints());
        lastPointCloudTimestamp = pointCloud.getTimestamp();
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      render.draw(pointCloudMesh, pointCloudShader);
    }

    // Visualize planes.
    planeRenderer.drawPlanes(
        render,
        session.getAllTrackables(Plane.class),
        camera.getDisplayOrientedPose(),
        projectionMatrix);

    // -- Draw occluded virtual objects

    // Update lighting parameters in the shader
    updateLightEstimation(frame.getLightEstimate(), viewMatrix);

    // Visualize anchors created by touch.
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
    // TrueFit AR: measurement markers + line, then every placed piece of furniture.
    fitCheckRenderer.drawMeasurement(
        render,
        measurementManager.getPoints(),
        virtualObjectShader,
        viewMatrix,
        projectionMatrix,
        virtualSceneFramebuffer);
    fitCheckRenderer.drawItems(
        render,
        fitCheckManager.getItems(),
        selectedItem,
        virtualObjectShader,
        viewMatrix,
        projectionMatrix,
        virtualSceneFramebuffer);

    // Compose the virtual scene with the background.
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
  }
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    rotationDetector.onTouchEvent(event);
    return super.onTouchEvent(event);
  }

  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      List<HitResult> hitResultList;
      if (instantPlacementSettings.isInstantPlacementEnabled()) {
        hitResultList =
            frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
      } else {
        hitResultList = frame.hitTest(tap);
      }
      for (HitResult hit : hitResultList) {
        // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
        Trackable trackable = hit.getTrackable();
        // If a plane was hit, check that it was hit inside the plane polygon.
        // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
            || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                    == OrientationMode.ESTIMATED_SURFACE_NORMAL)
            || (trackable instanceof InstantPlacementPoint)
            || (trackable instanceof DepthPoint)) {
          // Cap the number of objects created. This avoids overloading both the
          // rendering system and ARCore.
          if (wrappedAnchors.size() >= 20) {
            wrappedAnchors.get(0).getAnchor().detach();
            wrappedAnchors.remove(0);
          }

          // Adding an Anchor tells ARCore that it should track this position in
          // space. This anchor is created on the Plane to place the 3D model
          // in the correct position relative both to the world and to the plane.
          onFloorTap(hit);

          // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
          // Instant Placement Point.
          break;
        }
      }
    }
  }

  /**
   * Shows a pop-up dialog on the first call, determining whether the user wants to enable
   * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // Don't need to show dialog.
    }

    // Asks the user whether they want to use depth-based occlusion.
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_with_depth)
        .setMessage(R.string.depth_use_explanation)
        .setPositiveButton(
            R.string.button_text_enable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(true);
            })
        .setNegativeButton(
            R.string.button_text_disable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(false);
            })
        .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_instant_placement)
        .setMultiChoiceItems(
            resources.getStringArray(R.array.instant_placement_options_array),
            instantPlacementSettingsMenuDialogCheckboxes,
            (DialogInterface dialog, int which, boolean isChecked) ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
        .setPositiveButton(
            R.string.done,
            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
        .setNegativeButton(
            android.R.string.cancel,
            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
        .show();
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private void launchDepthSettingsMenuDialog() {
    // Retrieves the current settings to show in the checkboxes.
    resetSettingsMenuDialogCheckboxes();

    // Shows the dialog to the user.
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // With depth support, the user can select visualization options.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_with_depth)
          .setMultiChoiceItems(
              resources.getStringArray(R.array.depth_options_array),
              depthSettingsMenuDialogCheckboxes,
              (DialogInterface dialog, int which, boolean isChecked) ->
                  depthSettingsMenuDialogCheckboxes[which] = isChecked)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .setNegativeButton(
              android.R.string.cancel,
              (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
          .show();
    } else {
      // Without depth support, no settings are available.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_without_depth)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
        instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
        instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** Update state based on the current frame's light estimation. */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
        lightEstimate.getEnvironmentalHdrMainLightDirection(),
        lightEstimate.getEnvironmentalHdrMainLightIntensity(),
        viewMatrix);
    updateSphericalHarmonicsCoefficients(
        lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
    // constants in sphericalHarmonicFactors were derived from three terms:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
    // of all incoming light over a hemisphere for a given surface normal, which is what the shader
    // (environmental_hdr.frag) expects.
    //
    // You can read more details about the math here:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
          "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // Apply each factor to every component of each coefficient
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
        "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Configures the session with feature settings. */
  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }
    if (instantPlacementSettings.isInstantPlacementEnabled()) {
      config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
    } else {
      config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
    }
    session.configure(config);
  }
  private void showFitToast(String message) {
    runOnUiThread(() -> {
      if (currentToast != null) {
        currentToast.cancel();
      }
      currentToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
      currentToast.show();
    });
  }

  // =====================================================================
  // TrueFit AR - screen flow: choose mode -> measure / fit check -> adjust -> save
  // =====================================================================

  private enum Mode { CHOOSE, MEASURE, PLACE, ADJUST }

  private volatile Mode mode = Mode.CHOOSE;
  private volatile FitCheckItem selectedItem = null;
  private FurnitureType pendingType = FurnitureType.OTHER;
  private volatile boolean dragging = false;
  private volatile float dragX;
  private volatile float dragY;
  private boolean multiTouch = false;
  private volatile boolean resetRequested = false;
  private volatile String saveRequestName = null;
  private String lastAdjustKey = "";
  private String lastBadge = "";
  private String lastDetail = "";
  private String lastHint = null;

  private void setupTrueFitUi() {
    findViewById(R.id.chooseMeasure).setOnClickListener(v -> {
      findViewById(R.id.modeChooser).setVisibility(View.GONE);
      findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
      enterMeasureMode();
    });
    findViewById(R.id.chooseFitCheck).setOnClickListener(v -> {
      findViewById(R.id.modeChooser).setVisibility(View.GONE);
      findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
      startFitCheckFlow();
    });
    findViewById(R.id.btnMeasure).setOnClickListener(v -> enterMeasureMode());
    findViewById(R.id.btnFitCheck).setOnClickListener(v -> startFitCheckFlow());
    findViewById(R.id.btnSave).setOnClickListener(v -> startSaveFlow());
    findViewById(R.id.btnReset).setOnClickListener(v -> resetRequested = true);
    highlightButtons();
    showStatus(null, 0, "Move your phone slowly to scan the floor, then pick an option");
  }

  /** Step 4: measure the distance between two points. */
  private void enterMeasureMode() {
    mode = Mode.MEASURE;
    dragging = false;
    highlightButtons();
    updateMeasureStatus();
  }

  /** Steps 5-8: furniture list -> size dialog -> place on the floor. */
  private void startFitCheckFlow() {
    if (!measurementManager.isComplete()) {
      showFitToast("Measure the gap first: tap 2 points, then open Fit Check");
      enterMeasureMode();
      return;
    }
    Dialogs.pickFurniture(this, type -> {
      pendingType = type;
      Dialogs.enterDimensions(this, type, (lengthM, heightM, widthM) -> {
        pendingType = type;
        pendingLengthMeters = lengthM;
        pendingHeightMeters = heightM;
        pendingWidthMeters = widthM;
        mode = Mode.PLACE;
        dragging = false;
        highlightButtons();
        showStatus(null, 0, "Tap the floor (white dots) where the "
            + type.getLabel().toLowerCase(Locale.US) + " should go");
      });
    });
  }

  /** Step 10: save the room layout. */
  private void startSaveFlow() {
    if (!measurementManager.isComplete()) {
      showFitToast("Measure a gap first, then save the room");
      return;
    }
    String defaultName = "Room " + (RoomStore.loadAll(this).size() + 1);
    Dialogs.askText(this, "Save this room", "Give the layout a name", defaultName, "Save",
        text -> saveRequestName = text);
  }

  /** Runs on the GL thread so ARCore poses are read safely. */
  private void doSave(String name) {
    if (!measurementManager.isComplete()) return;
    Pose a = measurementManager.getPoints().get(0).getPose();
    Pose b = measurementManager.getPoints().get(1).getPose();

    SavedRoom room = new SavedRoom();
    room.name = name;
    room.timestamp = System.currentTimeMillis();
    room.gapMeters = measurementManager.getDistanceMeters();
    for (FitCheckItem item : fitCheckManager.getItems()) {
      FitEvaluator.Result r = FitEvaluator.evaluate(a, b, item);
      SavedRoom.Item si = new SavedRoom.Item();
      si.type = item.getType().name();
      si.lengthM = item.getLengthMeters();
      si.heightM = item.getHeightMeters();
      si.widthM = item.getWidthMeters();
      si.along = r.along;
      si.lateral = r.lateral;
      si.relAngleDeg = r.relAngleDeg;
      si.fits = r.sizeFits && (!r.insideKnown || r.inside);
      room.items.add(si);
    }
    RoomStore.save(this, room);
    showFitToast("Saved \"" + name + "\" - find it under Saved Rooms");
  }

  /** Called from handleTap() (GL thread) for every valid floor tap. */
  private void onFloorTap(HitResult hit) {
    switch (mode) {
      case MEASURE:
        measurementManager.addPoint(hit);
        updateMeasureStatus();
        break;
      case PLACE:
        placeItem(hit);
        break;
      case ADJUST:
        adjustTap(hit);
        break;
      default:
        break; // CHOOSE: the mode picker is still on screen
    }
  }

  private void placeItem(HitResult hit) {
    if (!measurementManager.isComplete()) {
      mode = Mode.MEASURE;
      updateMeasureStatus();
      runOnUiThread(this::highlightButtons);
      return;
    }
    Pose a = measurementManager.getPoints().get(0).getPose();
    Pose b = measurementManager.getPoints().get(1).getPose();

    FitCheckItem item = fitCheckManager.placeFitCheckItem(
        hit, pendingLengthMeters, pendingHeightMeters, pendingWidthMeters, null);
    item.setType(pendingType);
    item.setRotationDegrees(FitEvaluator.alignedYawDegrees(a, b)); // long side along the gap
    selectedItem = item;
    mode = Mode.ADJUST;
    lastAdjustKey = "";
    runOnUiThread(this::highlightButtons);

    updateAdjustStatus();
    showFitToast(lastBadge + " - " + lastDetail);
  }

  /** Tap another piece to select it, or tap the floor to move the selected piece there. */
  private void adjustTap(HitResult hit) {
    Pose tap = hit.getHitPose();
    FitCheckItem current = selectedItem;
    if (current != null && FitEvaluator.contains(current, tap)) return;
    for (FitCheckItem it : fitCheckManager.getItems()) {
      if (it != current && FitEvaluator.contains(it, tap)) {
        selectedItem = it;
        lastAdjustKey = "";
        return;
      }
    }
    if (current != null) {
      fitCheckManager.moveItem(current, hit);
    }
  }

  /** Step 9: one-finger drag moves the selected furniture along the floor. */
  private void handleDrag(Frame frame, Camera camera) {
    FitCheckItem item = selectedItem;
    if (!dragging || mode != Mode.ADJUST || item == null) return;
    if (camera.getTrackingState() != TrackingState.TRACKING) return;
    for (HitResult hit : frame.hitTest(dragX, dragY)) {
      Trackable trackable = hit.getTrackable();
      if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
        fitCheckManager.moveItem(item, hit);
        break;
      }
    }
  }

  /** Reset / save requests coming from the UI thread are handled here, on the GL thread. */
  private void processRequests() {
    if (resetRequested) {
      resetRequested = false;
      measurementManager.reset();
      fitCheckManager.clearAll();
      selectedItem = null;
      dragging = false;
      lastAdjustKey = "";
      mode = Mode.MEASURE;
      runOnUiThread(this::highlightButtons);
      updateMeasureStatus();
    }
    String name = saveRequestName;
    if (name != null) {
      saveRequestName = null;
      doSave(name);
    }
  }

  private void updateMeasureStatus() {
    int n = measurementManager.getPoints().size();
    if (n == 0) {
      showStatus(null, 0, "Tap the FIRST point (one end of the gap)");
    } else if (n == 1) {
      showStatus(null, 0, "Now tap the SECOND point");
    } else {
      showStatus(null, 0, "Distance: " + measurementManager.getFormattedDistance()
          + "\nTap Fit Check to test furniture in this gap");
    }
  }

  /** Recomputed every frame while adjusting, so the verdict follows the furniture as it moves. */
  private void updateAdjustStatus() {
    FitCheckItem item = selectedItem;
    if (mode != Mode.ADJUST || item == null || !measurementManager.isComplete()) return;

    Pose a = measurementManager.getPoints().get(0).getPose();
    Pose b = measurementManager.getPoints().get(1).getPose();
    FitEvaluator.Result r = FitEvaluator.evaluate(a, b, item);
    String name = item.getType().getLabel().toLowerCase(Locale.US);

    String badge;
    int color;
    String detail;
    if (!r.sizeFits) {
      badge = "CANNOT BUY";
      color = UiKit.RED;
      detail = String.format(Locale.US,
          "Too big: the %s needs %.2f m along the gap but only %.2f m is free. "
              + "Try twisting it with two fingers.", name, r.extent, r.gap);
    } else if (r.insideKnown && !r.inside) {
      badge = "SIZE OK";
      color = UiKit.ORANGE;
      detail = String.format(Locale.US,
          "The %s (%.2f m) fits the %.2f m gap. Drag it between the two orange markers.",
          name, r.extent, r.gap);
    } else {
      badge = "CAN BUY";
      color = UiKit.GREEN;
      detail = String.format(Locale.US,
          "It fits! %.2f m in a %.2f m gap (%.0f cm spare). Drag to move, twist to rotate.",
          r.extent, r.gap, (r.gap - r.extent) * 100f);
    }

    String key = badge + detail;
    if (!key.equals(lastAdjustKey)) {
      lastAdjustKey = key;
      lastBadge = badge;
      lastDetail = detail;
      showStatus(badge, color, detail);
    }
  }

  private void showStatus(final String badge, final int color, final String detail) {
    runOnUiThread(() -> {
      TextView badgeView = findViewById(R.id.verdictBadge);
      if (badge == null) {
        badgeView.setVisibility(View.GONE);
      } else {
        badgeView.setText(badge);
        GradientDrawable bg = UiKit.rounded(this, color, 20);
        badgeView.setBackground(bg);
        badgeView.setVisibility(View.VISIBLE);
      }
      ((TextView) findViewById(R.id.distanceText)).setText(detail);
    });
  }

  private void showScanHint(final String message) {
    if (message == null ? lastHint == null : message.equals(lastHint)) return;
    lastHint = message;
    runOnUiThread(() -> {
      TextView hint = findViewById(R.id.scanHint);
      if (message == null) {
        hint.setVisibility(View.GONE);
      } else {
        hint.setText("🔍 " + message);
        hint.setVisibility(View.VISIBLE);
      }
    });
  }

  private void highlightButtons() {
    View measure = findViewById(R.id.btnMeasure);
    View fit = findViewById(R.id.btnFitCheck);
    int activeFill = 0xE60E5F5A;
    int idleFill = 0xE616294F;
    int idleStroke = 0xFF2B4A85;
    measure.setBackground(UiKit.roundedStroke(this,
        mode == Mode.MEASURE ? activeFill : idleFill,
        mode == Mode.MEASURE ? UiKit.TEAL : idleStroke, 16));
    boolean fitActive = mode == Mode.PLACE || mode == Mode.ADJUST;
    fit.setBackground(UiKit.roundedStroke(this,
        fitActive ? activeFill : idleFill,
        fitActive ? UiKit.TEAL : idleStroke, 16));
  }

}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
  private Anchor anchor;
  private Trackable trackable;

  public WrappedAnchor(Anchor anchor, Trackable trackable) {
    this.anchor = anchor;
    this.trackable = trackable;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public Trackable getTrackable() {
    return trackable;
  }
}
