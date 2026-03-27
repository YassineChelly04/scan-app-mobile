package com.scanni.app.processing

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProcessingGoldenTest {
    @Test
    fun documentMode_writesProcessedOutputFile() = runBlocking {
        val source = createSampleImage()
        val processor = OpenCvPageProcessor()

        val outputPath = processor.process(
            originalPath = source.absolutePath,
            mode = EnhancementMode.DOCUMENT,
            corners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
        )

        assertNotEquals(source.absolutePath, outputPath)
        assertTrue(File(outputPath).exists())
    }

    private fun createSampleImage(): File {
        val file = File("build/test-output/processing/source.jpg").apply {
            parentFile?.mkdirs()
        }
        Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(180, 120, 60))
            setPixel(30, 40, Color.rgb(20, 20, 20))
            file.outputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            recycle()
        }
        return file
    }
}
