//package com.wriety.pview_delta
//
//import android.graphics.Bitmap
//import android.graphics.Paint
//import android.graphics.Path
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.MotionEvent
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.viewinterop.AndroidView
//import com.nomivision.sys.WhiteBoardSpeedup
//import com.nomivision.sys.input.InputEventDispatchClient
//import com.wriety.delta_hw.ui.theme.Delta_hwTheme
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class MainActivity : ComponentActivity() {
//    private lateinit var whiteBoardSpeedup: WhiteBoardSpeedup
//    private lateinit var inputEventDispatchClient: InputEventDispatchClient
//    private lateinit var paint: Paint
//    private lateinit var whiteboardWidget: WhiteboardWidget
//
//    companion object {
//        private const val TAG = "MainActivity"
//        private const val USE_RGBA_4444 = true
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        // Initialize components
//        whiteBoardSpeedup = WhiteBoardSpeedup()
//        inputEventDispatchClient = InputEventDispatchClient()
//        whiteboardWidget = WhiteboardWidget()
//        setupPaint()
//
//        setContent {
//            Delta_hwTheme {
//                Surface(modifier = Modifier.fillMaxSize()) {
//                    WhiteboardScreen(
//                        whiteBoardSpeedup = whiteBoardSpeedup,
//                        inputEventDispatchClient = inputEventDispatchClient,
//                        paint = paint,
//                        whiteboardWidget = whiteboardWidget
//                    )
//                }
//            }
//        }
//    }
//
//    private fun setupPaint() {
//        paint = Paint().apply {
//            isAntiAlias = true
//            style = Paint.Style.STROKE
//            strokeWidth = 5f
//            strokeJoin = Paint.Join.ROUND
//            strokeCap = Paint.Cap.ROUND
//            isDither = true
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        initializeWhiteboard()
//        initializeTouchInput()
//    }
//
//    private fun initializeWhiteboard() {
//        try {
//            if (USE_RGBA_4444) {
//                whiteBoardSpeedup.init(Bitmap.Config.ARGB_4444)
//            } else {
//                whiteBoardSpeedup.init(Bitmap.Config.ARGB_8888)
//            }
//        } catch (ex: Exception) {
//            Log.e(TAG, "Failed to initialize WhiteBoardSpeedup: ${ex.message}")
//        }
//    }
//
//    private fun initializeTouchInput() {
//        try {
//            inputEventDispatchClient.init()
//            inputEventDispatchClient.setEventCallback(object : InputEventDispatchClient.EventCallback() {
//                override fun onMotionEvent() {
//                    whiteboardWidget.processMotionEvents(inputEventDispatchClient, paint, whiteBoardSpeedup)
//                }
//            })
//        } catch (ex: Exception) {
//            Log.e(TAG, "Failed to initialize InputEventDispatchClient: ${ex.message}")
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        try {
//            whiteBoardSpeedup.uninit()
//            inputEventDispatchClient.uninit()
//        } catch (ex: Exception) {
//            Log.e(TAG, "Failed to uninitialize: ${ex.message}")
//        }
//    }
//}