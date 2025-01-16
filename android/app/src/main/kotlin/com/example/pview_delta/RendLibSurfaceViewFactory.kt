package com.example.pview_delta

import android.content.Context
import android.graphics.Color
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class RendLibSurfaceViewFactory(private val messenger: BinaryMessenger) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val creationParams = args as? Map<String?, Any?>
        val channel = MethodChannel(messenger, "custom_canvas_view_$viewId")
        return RendLibSurfaceViewWrapper(context, creationParams, channel)
    }
}