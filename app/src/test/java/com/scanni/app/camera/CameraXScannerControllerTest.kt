package com.scanni.app.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CameraXScannerControllerTest {
    @Test
    fun capturePage_usesCameraCaptureAndReturnsWrittenFiles() = runTest {
        val outputDir = File("build/test-output/camera").apply {
            mkdirs()
        }
        val lifecycleOwner = TestLifecycleOwner()
        val controller = CameraXScannerController(
            appContext = ApplicationProvider.getApplicationContext(),
            outputDir = outputDir,
            lifecycleOwnerProvider = { lifecycleOwner },
            captureWithCamera = { originalFile, previewFile, _ ->
                originalFile.writeText("original")
                previewFile.writeText("preview")
            }
        )

        val captured = controller.capturePage()

        assertTrue(File(captured.originalPath).exists())
        assertTrue(File(captured.previewPath).exists())
        assertEquals("original", File(captured.originalPath).readText())
        assertEquals("preview", File(captured.previewPath).readText())
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle
            get() = registry
    }
}
