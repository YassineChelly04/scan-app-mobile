package com.scanni.app.ocr

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MlKitOcrEngineTest {
    @Test
    fun extractText_usesRecognizerAndTrimsResult() = runTest {
        var recognizedPath: String? = null
        val engine = MlKitOcrEngine(
            appContext = ApplicationProvider.getApplicationContext(),
            recognizeText = { imagePath ->
                recognizedPath = imagePath
                "  Lecture Notes  "
            }
        )

        val text = engine.extractText("files/page-1.jpg")

        assertEquals("files/page-1.jpg", recognizedPath)
        assertEquals("Lecture Notes", text)
    }
}
