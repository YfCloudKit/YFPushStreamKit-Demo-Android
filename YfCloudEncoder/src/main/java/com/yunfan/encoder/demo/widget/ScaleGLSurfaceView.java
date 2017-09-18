package com.yunfan.encoder.demo.widget;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.yunfan.encoder.demo.util.Log;

/**
 * Created by yunfan on 2016/9/8.
 */
public class ScaleGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "Yf_ScaleGLSurfaceView";

    public ScaleGLSurfaceView(Context context) {
        this(context, null);
    }

    public ScaleGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleListener);
    }

    public void initScaleGLSurfaceView(OnScareCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface OnScareCallback {
        int getCurrentZoom();

        int getMaxZoom();

        boolean onScale(int zoom);
    }

    OnScareCallback mCallback;
    ScaleGestureDetector mScaleGestureDetector;
    private double mZoom;
    private ScaleGestureDetector.SimpleOnScaleGestureListener mScaleListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {


        private int mZoomWhenScaleBegan;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            Log.d(TAG, "scale factor: " + scaleFactor);
            if (mCallback != null) {
                int zoom = (int) (mZoomWhenScaleBegan + (mCallback.getMaxZoom() * (scaleFactor - 1)));
                zoom = Math.min(zoom, mCallback.getMaxZoom());
                zoom = Math.max(0, zoom);
                if (zoom!= mZoom) {
                    mZoom =zoom;
                    Log.d(TAG, "onScale zoom: " + zoom);
                    mCallback.onScale(zoom);
                }
            }

            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin");
            if (mCallback != null)
                mZoomWhenScaleBegan = mCallback.getCurrentZoom();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd");
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        Log.d(TAG, "onTouchEvent: " + event.getX() + "  " + event.getY());
        if (mScaleGestureDetector != null && event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_UP)
            mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    public static Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int centerX = (int) (x * 2000 - 1000);
        int centerY = (int) (y * 2000 - 1000);
        Log.d(TAG, "focus centerX:" + centerX + "centerY:" + centerY);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        Log.d(TAG, "focus left=" + left + " right=" + right + " top=" + top + " bottom=" + bottom);

        return new Rect(left, top, right, bottom);
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
}
