package com.scanni.app

import android.app.Application
import androidx.work.Configuration
import com.scanni.app.di.AppGraph
import com.scanni.app.vision.VisionRuntime
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class ScanniApplication : Application(), Configuration.Provider {

    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        VisionRuntime.init()
        PDFBoxResourceLoader.init(applicationContext)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(graph.workerFactory)
            .build()
}
