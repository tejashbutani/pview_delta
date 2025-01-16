package com.example.pview_delta

import android.content.Context
import android.graphics.Color
import android.view.View
import display.interactive.renderlib.RenderUtils
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class CustomPlatformView(
    context: Context,
    private val methodChannel: MethodChannel,
    creationParams: Map<String, Any>?
) : PlatformView, MethodChannel.MethodCallHandler {
    private val rendLibView: RendLibSurfaceView = RendLibSurfaceView(
        context,
        (creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK,
        (creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f
    )

    init {
        methodChannel.setMethodCallHandler(this)
        rendLibView.setMethodChannel(methodChannel)
        val colorValue = creationParams?.get("color")
        val widthValue = creationParams?.get("width")
        // android.util.Log.d("CustomPlatformView", "Color value: $colorValue (${colorValue?.javaClass})")
        // android.util.Log.d("CustomPlatformView", "Width value: $widthValue (${widthValue?.javaClass})")
    }

    override fun getView(): View {
        return rendLibView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updatePenSettings" -> {
                val color = (call.argument<Number>("color"))?.toInt()
                val width = call.argument<Double>("width")
                // android.util.Log.d("PenSettings", "Received method call - Color: $color, Width: $width")
                if (color != null && width != null) {
                    rendLibView.updatePenSettings(color, width.toFloat())
                    result.success(null)
                } else {
                    // android.util.Log.e("PenSettings", "Invalid arguments - Color: $color, Width: $width")
                    result.error("INVALID_ARGUMENTS", "Color or width is null", null)
                }
            }
            "clear" -> {
                RenderUtils.clearBitmapContent()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}
