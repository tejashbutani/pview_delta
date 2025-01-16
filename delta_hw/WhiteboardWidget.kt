package com.wriety.delta_hw

import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.nomivision.sys.WhiteBoardSpeedup
import com.nomivision.sys.input.InputEventDispatchClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WhiteboardWidget {
    private val handler = Handler(Looper.getMainLooper())
    private val currentPaths = mutableMapOf<Int, Path>()
    private val pathPointsMap = mutableMapOf<Int, MutableList<Pair<Float, Float>>>()
    private val activePointers = mutableSetOf<Int>()
    private val drawingScope = CoroutineScope(Dispatchers.Default + Job())
    
    companion object {
        private const val TAG = "WhiteboardWidget"
        private const val BATCH_SIZE = 5  // Number of points to process at once
    }

    fun processMotionEvents(inputEventDispatchClient: InputEventDispatchClient, paint: Paint, whiteBoardSpeedup: WhiteBoardSpeedup) {
        val events = inputEventDispatchClient.motionEventList ?: return
        
        for (event in events) {
            val pointerId = event.getPointerId(event.actionIndex)
            
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 
                    handleTouchDown(pointerId, event.getX(event.actionIndex), event.getY(event.actionIndex))
                    
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        if (id in activePointers) {
                            handleTouchMove(id, event.getX(i), event.getY(i), paint, whiteBoardSpeedup)
                        }
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 
                    handleTouchUp(pointerId, paint, whiteBoardSpeedup)
            }
        }
    }

    private fun handleTouchDown(pointerId: Int, x: Float, y: Float) {
        activePointers.add(pointerId)
        currentPaths[pointerId] = Path().apply {
            moveTo(x, y)
        }
        pathPointsMap[pointerId] = mutableListOf<Pair<Float, Float>>().apply {
            add(x to y)
        }
    }

    private fun handleTouchMove(pointerId: Int, x: Float, y: Float, paint: Paint, whiteBoardSpeedup: WhiteBoardSpeedup) {
        if (pointerId !in activePointers) return
        
        pathPointsMap[pointerId]?.add(x to y)
        
        if (pathPointsMap[pointerId]?.size ?: 0 >= BATCH_SIZE) {
            updatePath(pointerId)
            drawCurrentPath(pointerId, paint, whiteBoardSpeedup)
            
            pathPointsMap[pointerId]?.let { points ->
                val lastPoint = points.last()
                points.clear()
                points.add(lastPoint)
            }
        }
    }

    private fun handleTouchUp(pointerId: Int, paint: Paint, whiteBoardSpeedup: WhiteBoardSpeedup) {
        updatePath(pointerId)
        drawCurrentPath(pointerId, paint, whiteBoardSpeedup)
        
        activePointers.remove(pointerId)
        pathPointsMap[pointerId]?.clear()
        currentPaths.remove(pointerId)
        pathPointsMap.remove(pointerId)
    }

    private fun updatePath(pointerId: Int) {
        val points = pathPointsMap[pointerId] ?: return
        val path = currentPaths[pointerId] ?: return
        
        if (points.isEmpty()) return
        
        path.reset()
        path.moveTo(points[0].first, points[0].second)
        
        for (i in 1 until points.size) {
            path.lineTo(points[i].first, points[i].second)
        }
    }

    private fun drawCurrentPath(pointerId: Int, paint: Paint, whiteBoardSpeedup: WhiteBoardSpeedup) {
        try {
            val path = currentPaths[pointerId] ?: return
            drawingScope.launch(Dispatchers.Main) {
                whiteBoardSpeedup.accelFbCurFrameCanvas.drawPath(path, paint)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to draw path: ${ex.message}")
        }
    }
}

@Composable
fun WhiteboardScreen(
    whiteBoardSpeedup: WhiteBoardSpeedup,
    inputEventDispatchClient: InputEventDispatchClient,
    paint: Paint,
    whiteboardWidget: WhiteboardWidget
) {
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                whiteBoardSpeedup.uninit()
                inputEventDispatchClient.uninit()
            } catch (ex: Exception) {
                Log.e("WhiteboardScreen", "Failed to cleanup: ${ex.message}")
            }
        }
    }

    // TODO: Add your view implementation here
} 