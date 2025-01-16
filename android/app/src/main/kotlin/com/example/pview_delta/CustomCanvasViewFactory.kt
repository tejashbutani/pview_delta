package com.example.pview_delta

import android.content.Context
import android.graphics.Paint
import android.view.View
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import com.nomivision.sys.WhiteBoardSpeedup
import com.nomivision.sys.input.InputEventDispatchClient
import com.wriety.pview_delta.AcceleratedCanvasDelta

class CustomCanvasViewFactory(
    private val whiteBoardSpeedup: WhiteBoardSpeedup,
    private val inputEventDispatchClient: InputEventDispatchClient,
    private val paint: Paint,
    private val acceleratedCanvasDelta: AcceleratedCanvasDelta
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return CustomCanvasView(
            context, 
            viewId, 
            whiteBoardSpeedup,
            inputEventDispatchClient,
            paint,
            acceleratedCanvasDelta
        )
    }
}

class CustomCanvasView(
    private val context: Context,
    id: Int,
    private val whiteBoardSpeedup: WhiteBoardSpeedup,
    private val inputEventDispatchClient: InputEventDispatchClient,
    private val paint: Paint,
    private val acceleratedCanvasDelta: AcceleratedCanvasDelta
) : PlatformView {
    private val rendLibSurfaceView: RendLibSurfaceView

    init {
        rendLibSurfaceView = RendLibSurfaceView(context).apply {
            setMethodChannel(io.flutter.plugin.common.MethodChannel(
                io.flutter.embedding.engine.FlutterEngine(context).dartExecutor.binaryMessenger,
                "custom_canvas_view_$id"
            ))
            // Pass the dependencies to RendLibSurfaceView
            setDependencies(whiteBoardSpeedup, inputEventDispatchClient, paint, acceleratedCanvasDelta)
        }
    }

    override fun getView(): View {
        return rendLibSurfaceView
    }

    override fun dispose() {
        // Cleanup will be handled by MainActivity
    }
} 