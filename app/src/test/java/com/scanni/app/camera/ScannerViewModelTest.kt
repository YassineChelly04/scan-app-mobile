package com.scanni.app.camera

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerViewModelTest {

    @Test
    fun captureSuccess_appendsPageAndUpdatesCount() = runTest {
        val viewModel = ScannerViewModel()
        val firstPage = CapturedPageDraft(
            originalPath = "original.jpg",
            previewPath = "preview.jpg",
            detectedCorners = listOf(
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            )
        )
        val secondPage = CapturedPageDraft(
            originalPath = "original-2.jpg",
            previewPath = "preview-2.jpg",
            detectedCorners = listOf(
                10f, 10f,
                11f, 10f,
                11f, 11f,
                10f, 11f
            )
        )

        viewModel.uiState.test {
            assertEquals(ScannerUiState(), awaitItem())

            viewModel.onPageCaptured(firstPage)

            val firstUpdate = awaitItem()
            assertEquals(1, firstUpdate.pages.size)
            assertEquals(1, firstUpdate.captureCount)
            assertEquals(firstPage, firstUpdate.pages.single())

            viewModel.onPageCaptured(secondPage)

            val secondUpdate = awaitItem()
            assertEquals(2, secondUpdate.pages.size)
            assertEquals(2, secondUpdate.captureCount)
            assertEquals(listOf(firstPage, secondPage), secondUpdate.pages)
            assertEquals(firstPage, secondUpdate.pages[0])
            assertEquals(secondPage, secondUpdate.pages[1])
        }
    }
}
