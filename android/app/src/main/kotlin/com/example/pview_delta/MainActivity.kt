package com.example.pview_delta

import android.graphics.Paint
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import com.nomivision.sys.WhiteBoardSpeedup
import com.nomivision.sys.input.InputEventDispatchClient
import com.wriety.pview_delta.AcceleratedCanvasDelta

class MainActivity: FlutterActivity() {
    private lateinit var acceleratedCanvasDelta: AcceleratedCanvasDelta
    private lateinit var whiteBoardSpeedup: WhiteBoardSpeedup
    private lateinit var inputEventDispatchClient: InputEventDispatchClient
    private lateinit var paint: Paint

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Initialize components
        whiteBoardSpeedup = WhiteBoardSpeedup()
        inputEventDispatchClient = InputEventDispatchClient()
        acceleratedCanvasDelta = AcceleratedCanvasDelta()
        
        // Initialize paint settings
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        // Register the platform view factory with dependencies
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory(
                "custom_canvas_view", 
                CustomCanvasViewFactory(
                    whiteBoardSpeedup,
                    inputEventDispatchClient,
                    paint,
                    acceleratedCanvasDelta
                )
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            whiteBoardSpeedup.uninit()
            inputEventDispatchClient.uninit()
        } catch (ex: Exception) {
            // Handle cleanup errors
        }
    }
}