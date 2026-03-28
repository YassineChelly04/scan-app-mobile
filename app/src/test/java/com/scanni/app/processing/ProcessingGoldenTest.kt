package com.scanni.app.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
            corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
            rotationQuarterTurns = 0
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
                corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                rotationQuarterTurns = 0
            )
        )
        val innerCropOutput = BitmapFactory.decodeFile(
            processor.process(
                originalPath = source.absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0.2f, 0.1f, 0.8f, 0.1f, 0.75f, 0.9f, 0.25f, 0.9f),
                rotationQuarterTurns = 0
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
                corners = listOf(0f, 0f, 1f, 0f),
                rotationQuarterTurns = 0
            )
            fail("Expected invalid corner input to throw")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun process_rotationChangesOutputOrientation() = runBlocking {
        val source = createSampleImage()
        val processor = OpenCvPageProcessor()

        val unrotated = BitmapFactory.decodeFile(
            processor.process(
                originalPath = source.absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                rotationQuarterTurns = 0
            )
        )
        val rotated = BitmapFactory.decodeFile(
            processor.process(
                originalPath = source.absolutePath,
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                rotationQuarterTurns = 1
            )
        )

        assertTrue(unrotated.width > unrotated.height)
        assertEquals(unrotated.width, rotated.height)
        assertEquals(unrotated.height, rotated.width)
    }

    private fun createSampleImage(): File {
        val file = File("build/test-output/processing/source.jpg").apply {
            parentFile?.mkdirs()
        }
        Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(215, 190, 150))
            for (x in 15 until 105) {
                for (y in 10 until 70) {
                    setPixel(x, y, Color.rgb(30, 30, 30))
                }
            }
            for (x in 30 until 90) {
                setPixel(x, 40, Color.rgb(250, 250, 250))
            }
            file.outputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            recycle()
        }
        return file
    }
}
