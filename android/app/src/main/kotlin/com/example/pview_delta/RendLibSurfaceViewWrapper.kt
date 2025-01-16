package com.example.pview_delta

import android.content.Context
import android.view.View
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class RendLibSurfaceViewWrapper(
    context: Context,
    creationParams: Map<String?, Any?>?,
    private val methodChannel: MethodChannel,
) : PlatformView, MethodChannel.MethodCallHandler {
    
    private val rendLibSurfaceView: RendLibSurfaceView

    init {
        rendLibSurfaceView = RendLibSurfaceView(context)
        methodChannel.setMethodCallHandler(this)
        rendLibSurfaceView.setMethodChannel(methodChannel)

        // Apply initial settings from creation params
        creationParams?.let { params ->
            params["color"]?.let { color ->
                params["width"]?.let { width ->
                    rendLibSurfaceView.updatePenSettings(
                        (color as Number).toInt(),
                        (width as Number).toFloat()
                    )
                }
            }
        }
    }

    override fun getView(): View {
        return rendLibSurfaceView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updatePenSettings" -> {
                val color = call.argument<Number>("color")?.toInt()
                val width = call.argument<Number>("width")?.toFloat()
                if (color != null && width != null) {
                    rendLibSurfaceView.updatePenSettings(color, width)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENTS", "Color and width must be provided", null)
                }
            }
            "clear" -> {
                rendLibSurfaceView.clear()
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
} 