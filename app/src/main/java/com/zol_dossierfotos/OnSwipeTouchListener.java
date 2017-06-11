// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detects left and right swipes across a view.
 */
class OnSwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    OnSwipeTouchListener(Context context) {
        // Create a gesture listener instance.
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    // Method to override.
    void onSwipeLeft() {
    }

    // Method to override.
    void onSwipeRight() {
    }

    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        // Edit these for control over how far the user must swipe and how fast.
        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();

            if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) >
                    SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0)
                    onSwipeRight();
                else
                    onSwipeLeft();
                return true;
            }
            return false;
        }

    }

}
