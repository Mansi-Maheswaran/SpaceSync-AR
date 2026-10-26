package com.truefitar.furniture;

import android.view.MotionEvent;

/**
 * TrueFit AR — Module 2: 3D Furniture Placement & Manipulation
 *
 * Detects a two-finger twist (rotation) gesture. Android provides a
 * built-in ScaleGestureDetector for pinch-to-zoom, but no equivalent for
 * rotation, so this is a small custom implementation.
 *
 * Deliberately reports ONLY rotation — there is no paired scale factor,
 * to keep it clear this gesture must never be wired to resize furniture.
 *
 * Usage: forward your Activity/View's onTouchEvent() MotionEvents into
 * onTouchEvent(), and implement OnRotationListener to receive the delta
 * angle (in degrees) each time the fingers move.
 */
public class RotationGestureDetector {

    public interface OnRotationListener {
        /** Called with the change in angle (degrees) since the last event. */
        void onRotate(float deltaDegrees);
    }

    private final OnRotationListener listener;
    private float previousAngle;
    private boolean tracking = false;

    public RotationGestureDetector(OnRotationListener listener) {
        this.listener = listener;
    }

    public void onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    previousAngle = angleBetweenFingers(event);
                    tracking = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (tracking && event.getPointerCount() == 2) {
                    float currentAngle = angleBetweenFingers(event);
                    float delta = currentAngle - previousAngle;

                    // Normalize to avoid a jump when crossing the -180/180 boundary.
                    if (delta > 180f) delta -= 360f;
                    if (delta < -180f) delta += 360f;

                    listener.onRotate(delta);
                    previousAngle = currentAngle;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                break;
        }
    }

    private float angleBetweenFingers(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
}