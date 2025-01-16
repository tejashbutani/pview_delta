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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.MethodChannel;

public class RendLibSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "RendLibSurfaceView";
    private static final boolean ACCEL_FB_USE_LE_RGBA4444 = true;
    private static final int BATCH_SIZE = 5;  // Number of points to process at once
    
    private SurfaceHolder mHolder;
    private WhiteBoardSpeedup mWhiteBoardSpeedup;
    private InputEventDispatchClient mInEvtDispatchClient;
    private Paint mPaint;
    private Map<Integer, Path> mCurrentPaths;
    private Map<Integer, List<PointF>> mPathPointsMap;
    private Set<Integer> mActivePointers;
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
        
        mWhiteBoardSpeedup = new WhiteBoardSpeedup();
        initializeAccelFb();
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(4.0f);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setDither(true);
        
        mCurrentPaths = new HashMap<>();
        mPathPointsMap = new HashMap<>();
        mActivePointers = new HashSet<>();
        
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
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTouchDown(pointerId, event.getX(pointerIndex), event.getY(pointerIndex));
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    if (mActivePointers.contains(id)) {
                        handleTouchMove(id, event.getX(i), event.getY(i));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleTouchUp(pointerId);
                break;
        }
        return true;
    }

    private void handleTouchDown(int pointerId, float x, float y) {
        mActivePointers.add(pointerId);
        Path path = new Path();
        path.moveTo(x, y);
        mCurrentPaths.put(pointerId, path);
        
        List<PointF> points = new ArrayList<>();
        points.add(new PointF(x, y));
        mPathPointsMap.put(pointerId, points);
    }

    private void handleTouchMove(int pointerId, float x, float y) {
        List<PointF> points = mPathPointsMap.get(pointerId);
        if (points == null) return;

        points.add(new PointF(x, y));
        
        if (points.size() >= BATCH_SIZE) {
            updatePath(pointerId);
            drawCurrentPath(pointerId);
            
            // Keep only the last point for continuity
            PointF lastPoint = points.get(points.size() - 1);
            points.clear();
            points.add(lastPoint);
        }
    }

    private void handleTouchUp(int pointerId) {
        updatePath(pointerId);
        drawCurrentPath(pointerId);
        
        if (methodChannel != null) {
            sendStrokeData(pointerId);
        }
        
        mActivePointers.remove(pointerId);
        mCurrentPaths.remove(pointerId);
        mPathPointsMap.remove(pointerId);
    }

    private void updatePath(int pointerId) {
        List<PointF> points = mPathPointsMap.get(pointerId);
        Path path = mCurrentPaths.get(pointerId);
        if (points == null || path == null || points.isEmpty()) return;

        path.reset();
        PointF firstPoint = points.get(0);
        path.moveTo(firstPoint.x, firstPoint.y);
        
        for (int i = 1; i < points.size(); i++) {
            PointF point = points.get(i);
            path.lineTo(point.x, point.y);
        }
    }

    private void drawCurrentPath(int pointerId) {
        try {
            Path path = mCurrentPaths.get(pointerId);
            if (path == null) return;
            
            WhiteBoardSpeedup.AccelFbCanvas canvas = mWhiteBoardSpeedup.getAccelFbCurFrameCanvas();
            canvas.drawPath(path, mPaint);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to draw path: " + ex.toString());
        }
    }

    private void sendStrokeData(int pointerId) {
        List<PointF> points = mPathPointsMap.get(pointerId);
        if (points == null) return;

        Map<String, Object> strokeData = new HashMap<>();
        List<Map<String, Double>> pointsList = new ArrayList<>();
        
        for (PointF point : points) {
            Map<String, Double> pointMap = new HashMap<>();
            pointMap.put("x", (double) point.x);
            pointMap.put("y", (double) point.y);
            pointsList.add(pointMap);
        }
        
        strokeData.put("points", pointsList);
        strokeData.put("color", mPaint.getColor());
        strokeData.put("width", mPaint.getStrokeWidth());
        
        methodChannel.invokeMethod("onStrokeComplete", strokeData);
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