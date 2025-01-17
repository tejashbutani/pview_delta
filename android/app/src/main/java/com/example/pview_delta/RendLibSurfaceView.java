package com.example.pview_delta;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

public class RendLibSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    
    private SurfaceHolder mHolder;

    private Bitmap mBitmap;


    private int mScreenWidth;

    private int mScreenHeight;

    /**
     * Drawing Camvas
     */
    private Canvas mPaintCanvas;


    private Paint mPaint;
    

    private float Prex = 0.0f;
    private float Prey = 0.0f;
    private Path mPath = new Path();

    private List<PointF> currentStrokePoints = new ArrayList<>();
    private MethodChannel methodChannel;
    private WhiteBoardSpeedup mWhiteBoardSpeedup;

    private Map<Integer, Path> mPathMap = new HashMap<>();
    private Map<Integer, List<PointF>> mStrokePointsMap = new HashMap<>();
    private Map<Integer, Float> mLastXMap = new HashMap<>();
    private Map<Integer, Float> mLastYMap = new HashMap<>();

    public void setMethodChannel(MethodChannel channel) {
    this.methodChannel = channel;
   } 


    public RendLibSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public RendLibSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RendLibSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public RendLibSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public RendLibSurfaceView(Context context, int initialColor, float initialWidth) {
        super(context);
        init(context, initialColor, initialWidth);
    }

    public RendLibSurfaceView(Context context, AttributeSet attrs, int initialColor, float initialWidth) {
        super(context, attrs);
        init(context, initialColor, initialWidth);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mWhiteBoardSpeedup = new WhiteBoardSpeedup();
        try {
            mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_4444);

        } catch (Exception ex) {
            Log.e(TAG, "Failed to initialize WhiteBoardSpeedup: " + ex.toString());
            ex.printStackTrace();
        }

        mBitmap = mWhiteBoardSpeedup.getAccelFbCurFrameBitmap();
        
        getHolder().addCallback(this);
        
        // Initialize paint with optimal flags
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        float density = getResources().getDisplayMetrics().density;
        mPaint.setStrokeWidth(5.0f * density);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setDither(true);
        
        mPaintCanvas = new Canvas();
        mPaintCanvas.setBitmap(mBitmap);
    }

    private void init(Context context, int initialColor, float initialWidth) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mWhiteBoardSpeedup = new WhiteBoardSpeedup();
        try {
                mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_4444);

        } catch (Exception ex) {
            Log.e(TAG, "Failed to initialize WhiteBoardSpeedup: " + ex.toString());
            ex.printStackTrace();
        }
        
        mBitmap = mWhiteBoardSpeedup.getAccelFbCurFrameBitmap();
        
        getHolder().addCallback(this);
        
        // Initialize paint with optimal flags and initial parameters
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(initialColor);
        float density = getResources().getDisplayMetrics().density;
        mPaint.setStrokeWidth(initialWidth * density);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setDither(true);
        
        mPaintCanvas = new Canvas();
        mPaintCanvas.setBitmap(mBitmap);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        if(surfaceHolder != null) {
            mHolder = surfaceHolder;
            Canvas canvas = mHolder.lockCanvas();
            // Set the background of the acceleration bitmap to transparent
            canvas.drawColor(Color.WHITE);
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            mHolder.unlockCanvasAndPost(canvas);
        } else {
            Log.w("TestMXW", "surfaceHolder is nulll !!!");
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
      // RenderUtils.clearBitmapContent();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        int maskedAction = event.getActionMasked();

        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float startX = event.getX(actionIndex);
                float startY = event.getY(actionIndex);
                
                Path path = new Path();
                path.moveTo(startX, startY);
                mPathMap.put(pointerId, path);
                
                List<PointF> points = new ArrayList<>();
                points.add(new PointF(startX, startY));
                mStrokePointsMap.put(pointerId, points);
                
                mLastXMap.put(pointerId, startX);
                mLastYMap.put(pointerId, startY);

                mPaintCanvas.drawPoint(startX, startY, mPaint);
                // Log.w("onTouchEvent", "ACTION_POINTER_DOWN " + mPaint.getColor()  + mPaint.getStrokeWidth());
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < pointerCount; i++) {
                    int id = event.getPointerId(i);
                    float x = event.getX(i);
                    float y = event.getY(i);
                    
                    Path currentPath = mPathMap.get(id);
                    List<PointF> currentPoints = mStrokePointsMap.get(id);
                    Float lastX = mLastXMap.get(id);
                    Float lastY = mLastYMap.get(id);
                    
                    if (currentPath != null && currentPoints != null && lastX != null && lastY != null) {
                        currentPath.lineTo(x, y);
                        currentPoints.add(new PointF(x, y));
                        mPaintCanvas.drawLine(lastX, lastY, x, y, mPaint);
                        
                        mLastXMap.put(id, x);
                        mLastYMap.put(id, y);
                    }
                }
                // Log.w("onTouchEvent", "ACTION_POINTER_MOVE " + mPaint.getColor()  + mPaint.getStrokeWidth());
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                List<PointF> finalPoints = mStrokePointsMap.get(pointerId);
                if (finalPoints != null && methodChannel != null) {
                    Map<String, Object> strokeData = new HashMap<>();
                    List<Map<String, Double>> pointsList = new ArrayList<>();
                    
                    float density = getResources().getDisplayMetrics().density;
                    
                    for (PointF point : finalPoints) {
                        Map<String, Double> pointMap = new HashMap<>();
                        pointMap.put("x", (double) (point.x / density));
                        pointMap.put("y", (double) (point.y / density));
                        pointsList.add(pointMap);
                    }
                    
                    strokeData.put("points", pointsList);
                    strokeData.put("color", mPaint.getColor());
                    strokeData.put("width", (double) (mPaint.getStrokeWidth() / density));
                    
                    methodChannel.invokeMethod("onStrokeComplete", strokeData);
                }
                
                mPathMap.remove(pointerId);
                mStrokePointsMap.remove(pointerId);
                mLastXMap.remove(pointerId);
                mLastYMap.remove(pointerId);
                // Log.w("onTouchEvent", "ACTION_POINTER_UP " + mPaint.getColor()  + mPaint.getStrokeWidth());
                break;

            case MotionEvent.ACTION_CANCEL:
                mPathMap.clear();
                mStrokePointsMap.clear();
                mLastXMap.clear();
                mLastYMap.clear();
                break;
        }
        
        return true;
    }

    public void updatePenSettings(int color, float width) {
        // width parameter is in logical pixels (dp)
        // need to convert to physical pixels by multiplying with density
        float density = getResources().getDisplayMetrics().density;
        float physicalWidth = width * density;
        
        // Log.d("PenSettings", "Before update - Current color: " + mPaint.getColor() + 
        //                    ", Current width: " + mPaint.getStrokeWidth() + " px");
        
        mPaint.setColor(color);
        mPaint.setStrokeWidth(physicalWidth);
        
        // Log.d("PenSettings", "After update - New color: " + color + 
        //                    ", New width: " + width + " dp" +
        //                    ", Screen density: " + density +
        //                    ", Physical width: " + physicalWidth + " px");
    }
}


//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.PixelFormat;
//import android.graphics.PointF;
//import android.util.AttributeSet;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//
//import androidx.annotation.NonNull;
//
//import com.nomivision.sys.WhiteBoardSpeedup;
//import com.nomivision.sys.input.InputEventDispatchClient;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import io.flutter.plugin.common.MethodChannel;
//
//public class RendLibSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
//    private static final String TAG = "RendLibSurfaceView";
//    private static final boolean ACCEL_FB_USE_LE_RGBA4444 = true;
//    private static final int BATCH_SIZE = 5;  // Number of points to process at once
//
//    private SurfaceHolder mHolder;
//    private WhiteBoardSpeedup mWhiteBoardSpeedup;
//    private InputEventDispatchClient mInEvtDispatchClient;
//    private Paint mPaint;
//    private Map<Integer, Path> mCurrentPaths;
//    private Map<Integer, List<PointF>> mPathPointsMap;
//    private Set<Integer> mActivePointers;
//    private MethodChannel methodChannel;
//
//    public RendLibSurfaceView(Context context) {
//        super(context);
//        init(context);
//    }
//
//    public RendLibSurfaceView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init(context);
//    }
//
//    private void init(Context context) {
//        try {
//            setLayerType(View.LAYER_TYPE_HARDWARE, null);
//
//            mWhiteBoardSpeedup = new WhiteBoardSpeedup();
//            initializeAccelFb();
//
//            mPaint = new Paint();
//            mPaint.setAntiAlias(true);
//            mPaint.setColor(Color.RED);
//            mPaint.setStrokeWidth(4.0f);
//            mPaint.setStyle(Paint.Style.STROKE);
//            mPaint.setStrokeCap(Paint.Cap.ROUND);
//            mPaint.setStrokeJoin(Paint.Join.ROUND);
//            mPaint.setDither(true);
//
//            mCurrentPaths = new HashMap<>();
//            mPathPointsMap = new HashMap<>();
//            mActivePointers = new HashSet<>();
//
//            getHolder().addCallback(this);
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to initialize RendLibSurfaceView: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void initializeAccelFb() {
//        try {
//            if (ACCEL_FB_USE_LE_RGBA4444) {
//                mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_4444);
//            } else {
//                mWhiteBoardSpeedup.init(Bitmap.Config.ARGB_8888);
//            }
//        } catch (Exception ex) {
//            Log.e(TAG, "Failed to initialize WhiteBoardSpeedup: " + ex.toString());
//            ex.printStackTrace();
//        }
//    }
//
//    public void setMethodChannel(MethodChannel channel) {
//        this.methodChannel = channel;
//    }
//
//    @Override
//    public void surfaceCreated(@NonNull SurfaceHolder holder) {
//        mHolder = holder;
//        mHolder.setFormat(PixelFormat.TRANSPARENT);
//        clearCanvas();
//    }
//
//    void clearCanvas() {
//        try {
//            WhiteBoardSpeedup.AccelFbCanvas canvas = mWhiteBoardSpeedup.getAccelFbCurFrameCanvas();
//            Paint clearPaint = new Paint();
//            clearPaint.setColor(Color.TRANSPARENT);
//            clearPaint.setStyle(Paint.Style.FILL);
//            canvas.drawColor(Color.TRANSPARENT);
////            mWhiteBoardSpeedup.postCurFrameToDisp(true);
//        } catch (Exception ex) {
//            Log.e(TAG, "Failed to clear canvas: " + ex.toString());
//        }
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        int pointerIndex = event.getActionIndex();
//        int pointerId = event.getPointerId(pointerIndex);
//
//        switch (event.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_POINTER_DOWN:
//                handleTouchDown(pointerId, event.getX(pointerIndex), event.getY(pointerIndex));
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                for (int i = 0; i < event.getPointerCount(); i++) {
//                    int id = event.getPointerId(i);
//                    if (mActivePointers.contains(id)) {
//                        handleTouchMove(id, event.getX(i), event.getY(i));
//                    }
//                }
//                break;
//
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_POINTER_UP:
//                handleTouchUp(pointerId);
//                break;
//        }
//        return true;
//    }
//
//    private void handleTouchDown(int pointerId, float x, float y) {
//        mActivePointers.add(pointerId);
//        Path path = new Path();
//        path.moveTo(x, y);
//        mCurrentPaths.put(pointerId, path);
//
//        List<PointF> points = new ArrayList<>();
//        points.add(new PointF(x, y));
//        mPathPointsMap.put(pointerId, points);
//    }
//
//    private void handleTouchMove(int pointerId, float x, float y) {
//        List<PointF> points = mPathPointsMap.get(pointerId);
//        if (points == null) return;
//
//        points.add(new PointF(x, y));
//
//        if (points.size() >= BATCH_SIZE) {
//            updatePath(pointerId);
//            drawCurrentPath(pointerId);
//
//            // Keep only the last point for continuity
//            PointF lastPoint = points.get(points.size() - 1);
//            points.clear();
//            points.add(lastPoint);
//        }
//    }
//
//    private void handleTouchUp(int pointerId) {
//        updatePath(pointerId);
//        drawCurrentPath(pointerId);
//
//        if (methodChannel != null) {
//            sendStrokeData(pointerId);
//        }
//
//        mActivePointers.remove(pointerId);
//        mCurrentPaths.remove(pointerId);
//        mPathPointsMap.remove(pointerId);
//    }
//
//    private void updatePath(int pointerId) {
//        List<PointF> points = mPathPointsMap.get(pointerId);
//        Path path = mCurrentPaths.get(pointerId);
//        if (points == null || path == null || points.isEmpty()) return;
//
//        path.reset();
//        PointF firstPoint = points.get(0);
//        path.moveTo(firstPoint.x, firstPoint.y);
//
//        for (int i = 1; i < points.size(); i++) {
//            PointF point = points.get(i);
//            path.lineTo(point.x, point.y);
//        }
//    }
//
//    private void drawCurrentPath(int pointerId) {
//        try {
//            Path path = mCurrentPaths.get(pointerId);
//            if (path == null) return;
//
//            WhiteBoardSpeedup.AccelFbCanvas canvas = mWhiteBoardSpeedup.getAccelFbCurFrameCanvas();
//            canvas.drawp
//            canvas.drawPath(path, mPaint);
//        } catch (Exception ex) {
//            Log.e(TAG, "Failed to draw path: " + ex.toString());
//        }
//    }
//
//    private void sendStrokeData(int pointerId) {
//        List<PointF> points = mPathPointsMap.get(pointerId);
//        if (points == null) return;
//
//        Map<String, Object> strokeData = new HashMap<>();
//        List<Map<String, Double>> pointsList = new ArrayList<>();
//
//        for (PointF point : points) {
//            Map<String, Double> pointMap = new HashMap<>();
//            pointMap.put("x", (double) point.x);
//            pointMap.put("y", (double) point.y);
//            pointsList.add(pointMap);
//        }
//
//        strokeData.put("points", pointsList);
//        strokeData.put("color", mPaint.getColor());
//        strokeData.put("width", mPaint.getStrokeWidth());
//
//        methodChannel.invokeMethod("onStrokeComplete", strokeData);
//    }
//
//    public void updatePenSettings(int color, float width) {
//        mPaint.setColor(color);
//        mPaint.setStrokeWidth(width);
//        Log.d(TAG, "Updated pen settings - Color: " + color + ", Width: " + width);
//    }
//
//    public void clear() {
//        clearCanvas();
//    }
//
//    @Override
//    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//    }
//
//    @Override
//    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//        if (mWhiteBoardSpeedup != null) {
////            mWhiteBoardSpeedup.uninit();
//        }
//    }
//}