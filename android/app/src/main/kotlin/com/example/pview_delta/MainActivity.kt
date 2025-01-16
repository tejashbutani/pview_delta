package com.example.pview_delta

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import androidx.annotation.NonNull

class MainActivity: FlutterActivity() {

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // Register the platform view factory
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("custom_canvas_view", RendLibSurfaceViewFactory(flutterEngine.dartExecutor.binaryMessenger))
    }

}