package com.scanni.app.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
            corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        )

        assertNotEquals(source.absolutePath, outputPath)
        assertTrue(File(outputPath).exists())
    }

    @Test
    fun process_warpedOutputChangesWhenCornersChange() = runBlocking {
        val source = createSampleImage()
        val processor = OpenCvPageProcessor()

        val fullPageOutput = BitmapFactory.decodeFile(
            processor.process(
                originalPath = source.absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
            )
        )
        val innerCropOutput = BitmapFactory.decodeFile(
            processor.process(
                originalPath = source.absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0.2f, 0.1f, 0.8f, 0.1f, 0.75f, 0.9f, 0.25f, 0.9f)
            )
        )

        assertTrue(innerCropOutput.width < fullPageOutput.width)
        assertTrue(innerCropOutput.height <= fullPageOutput.height)
    }

    @Test
    fun process_rejectsInvalidCornerShape() = runBlocking {
        try {
            OpenCvPageProcessor().process(
                originalPath = createSampleImage().absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0f, 0f, 1f, 0f)
            )
            fail("Expected invalid corner input to throw")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun createSampleImage(): File {
        val file = File("build/test-output/processing/source.jpg").apply {
            parentFile?.mkdirs()
        }
        Bitmap.createBitmap(80, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(215, 190, 150))
            for (x in 10 until 70) {
                for (y in 15 until 85) {
                    setPixel(x, y, Color.rgb(30, 30, 30))
                }
            }
            for (x in 20 until 60) {
                setPixel(x, 50, Color.rgb(250, 250, 250))
            }
            file.outputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            recycle()
        }
        return file
    }
}
