package com.example.pview_delta;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import display.interactive.renderlib.RenderUtils;
import io.flutter.plugin.common.MethodChannel;

/**
 * @ClassName: display.interactive.rendlibtools.view
 * @Description: 作用表述
 * @Author: maoxingwen
 * @Date: 2024/11/23
 */
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
        
        RenderUtils.initRendLib();

//        int[] resolution = RenderUtils.getDeviceNativeResolution(context);
//        mScreenWidth = resolution[0];
//        mScreenHeight = resolution[1];
        
        // Create bitmap with optimal config for drawing
       mBitmap = RenderUtils.getAccelerateBitmap(3840, 2160);
        // mBitmap = Bitmap.createBitmap(3840, 2160, Bitmap.Config.ARGB_8888);
        
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
        
        RenderUtils.initRendLib();
        
        mBitmap = RenderUtils.getAccelerateBitmap(3840, 2160);
        
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
       RenderUtils.clearBitmapContent();
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
