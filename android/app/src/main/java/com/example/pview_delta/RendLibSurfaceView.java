package com.example.pview_delta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import com.nomivision.sys.WhiteBoardSpeedup;
import com.nomivision.sys.input.InputEventDispatchClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

public class RendLibSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "RendLibSurfaceView";
    private static final boolean ACCEL_FB_USE_LE_RGBA4444 = true;

    private SurfaceHolder mHolder;
    private WhiteBoardSpeedup mWhiteBoardSpeedup;
    private InputEventDispatchClient mInEvtDispatchClient;
    private Paint mPaint;
    private Path mPath;
    private float mLastX = 0.0f;
    private float mLastY = 0.0f;
    private List<PointF> currentStrokePoints;
    private MethodChannel methodChannel;

    public RendLibSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public RendLibSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Initialize WhiteBoardSpeedup
        mWhiteBoardSpeedup = new WhiteBoardSpeedup();
        initializeAccelFb();
        
        // Initialize paint settings
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(4.0f);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        
        mPath = new Path();
        currentStrokePoints = new ArrayList<>();
        getHolder().addCallback(this);
    }

    private void initializeAccelFb() {
        try {
            if (ACCEL_FB_USE_LE_RGBA4444) {
                mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_4444);
            } else {
                mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_8888);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to initialize WhiteBoardSpeedup: " + ex.toString());
        }
    }

    public void setMethodChannel(MethodChannel channel) {
        this.methodChannel = channel;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mHolder = holder;
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        clearCanvas();
    }

    void clearCanvas() {
        try {
            WhiteBoardSpeedup.AccelFbCanvas canvas = mWhiteBoardSpeedup.getAccelFbCurFrameCanvas();
            Paint clearPaint = new Paint();
            clearPaint.setColor(Color.TRANSPARENT);
            clearPaint.setStyle(Paint.Style.FILL);
            canvas.drawColor(Color.TRANSPARENT);
//            mWhiteBoardSpeedup.postCurFrameToDisp(true);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to clear canvas: " + ex.toString());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                handleTouchUp(x, y);
                break;
        }
        return true;
    }

    private void handleTouchDown(float x, float y) {
        mLastX = x;
        mLastY = y;
        currentStrokePoints.clear();
        currentStrokePoints.add(new PointF(x, y));
        mPath.moveTo(x, y);
    }

    private void handleTouchMove(float x, float y) {
        currentStrokePoints.add(new PointF(x, y));
        mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
        
        try {
            WhiteBoardSpeedup.AccelFbCanvas canvas = mWhiteBoardSpeedup.getAccelFbCurFrameCanvas();
            canvas.drawLine(mLastX, mLastY, x, y, mPaint);
//            mWhiteBoardSpeedup.postCurFrameToDisp(true);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to draw line: " + ex.toString());
        }
        
        mLastX = x;
        mLastY = y;
    }

    private void handleTouchUp(float x, float y) {
        currentStrokePoints.add(new PointF(x, y));
        
        if (methodChannel != null) {
            Map<String, Object> strokeData = new HashMap<>();
            List<Map<String, Double>> points = new ArrayList<>();
            
            for (PointF point : currentStrokePoints) {
                Map<String, Double> pointMap = new HashMap<>();
                pointMap.put("x", (double) point.x);
                pointMap.put("y", (double) point.y);
                points.add(pointMap);
            }
            
            strokeData.put("points", points);
            strokeData.put("color", Color.RED);
            strokeData.put("width", 4.0);
            
            methodChannel.invokeMethod("onStrokeComplete", strokeData);
        }
        
        mPath.reset();
    }

    public void updatePenSettings(int color, float width) {
        mPaint.setColor(color);
        mPaint.setStrokeWidth(width);
        Log.d(TAG, "Updated pen settings - Color: " + color + ", Width: " + width);
    }

    public void clear() {
        clearCanvas();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (mWhiteBoardSpeedup != null) {
//            mWhiteBoardSpeedup.uninit();
        }
    }

    public void setDependencies(
        WhiteBoardSpeedup whiteBoardSpeedup,
        InputEventDispatchClient inputEventDispatchClient,
        Paint paint
    ) {
        this.mWhiteBoardSpeedup = whiteBoardSpeedup;
        this.mInEvtDispatchClient = inputEventDispatchClient;
        this.mPaint = paint;
    }
}