package com.scanni.app.vision

import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * Loads the bundled OpenCV native library once. Every OpenCV call site checks
 * [isAvailable] so the app keeps working (without edge detection/perspective
 * correction) on the rare devices where the native library fails to load.
 */
object VisionRuntime {

    @Volatile
    var isAvailable: Boolean = false
        private set

    fun init() {
        if (isAvailable) return
        isAvailable = runCatching { OpenCVLoader.initLocal() }
            .onFailure { Log.e(TAG, "OpenCV failed to load", it) }
            .getOrDefault(false)
        Log.i(TAG, "OpenCV available: $isAvailable")
    }

    private const val TAG = "VisionRuntime"
}
