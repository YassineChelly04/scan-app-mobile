package com.scanni.app

import android.app.Application
import androidx.work.Configuration
import com.scanni.app.di.AppGraph
import com.scanni.app.vision.VisionRuntime

class ScanniApplication : Application(), Configuration.Provider {

    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        // OpenCV is needed for live camera detection from the first frame, so load it
        // at startup. PdfBox is only used during export and initializes itself lazily
        // in SearchablePdfWriter, keeping it off the cold-start path.
        VisionRuntime.init()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(graph.workerFactory)
            .build()
}
