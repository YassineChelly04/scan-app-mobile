package com.scanni.app.review

import app.cash.turbine.test
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun changeMode_reprocessesPageWithoutLosingOriginal() = runTest {
        val calls = mutableListOf<Pair<String, EnhancementMode>>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>
            ): String {
                calls += originalPath to mode
                return "$originalPath-${mode.name.lowercase()}.jpg"
            }
        }
        val corners = listOf(
            0f, 0f,
            100f, 0f,
            100f, 100f,
            0f, 100f
        )
        val viewModel = ReviewViewModel(processor)

        viewModel.uiState.test {
            assertEquals(PageReviewState(), awaitItem())

            viewModel.loadDraft(
                originalPath = "files/original-1.jpg",
                corners = corners
            )
            val loadedState = awaitItem()
            assertEquals("files/original-1.jpg", loadedState.originalPath)
            assertEquals("", loadedState.processedPath)
            assertEquals(EnhancementMode.DOCUMENT, loadedState.mode)
            assertEquals(corners, loadedState.corners)
            assertEquals(false, loadedState.isProcessing)
            assertEquals(null, loadedState.errorMessage)

            viewModel.changeMode(EnhancementMode.BOOK)
            val loadingBookState = awaitItem()
            assertEquals("", loadingBookState.processedPath)
            assertEquals(EnhancementMode.BOOK, loadingBookState.mode)
            assertEquals(true, loadingBookState.isProcessing)
            assertEquals(null, loadingBookState.errorMessage)

            dispatcher.scheduler.advanceUntilIdle()
            val bookState = awaitItem()
            assertEquals("files/original-1.jpg", bookState.originalPath)
            assertEquals("files/original-1.jpg-book.jpg", bookState.processedPath)
            assertEquals(EnhancementMode.BOOK, bookState.mode)
            assertEquals(corners, bookState.corners)
            assertEquals(false, bookState.isProcessing)
            assertEquals(null, bookState.errorMessage)

            viewModel.changeMode(EnhancementMode.WHITEBOARD)
            val loadingWhiteboardState = awaitItem()
            assertEquals("", loadingWhiteboardState.processedPath)
            assertEquals(EnhancementMode.WHITEBOARD, loadingWhiteboardState.mode)
            assertEquals(true, loadingWhiteboardState.isProcessing)
            assertEquals(null, loadingWhiteboardState.errorMessage)

            dispatcher.scheduler.advanceUntilIdle()
            val whiteboardState = awaitItem()
            assertEquals("files/original-1.jpg", whiteboardState.originalPath)
            assertEquals("files/original-1.jpg-whiteboard.jpg", whiteboardState.processedPath)
            assertEquals(EnhancementMode.WHITEBOARD, whiteboardState.mode)
            assertEquals(corners, whiteboardState.corners)
            assertEquals(false, whiteboardState.isProcessing)
            assertEquals(null, whiteboardState.errorMessage)

            assertEquals(
                listOf(
                    "files/original-1.jpg" to EnhancementMode.BOOK,
                    "files/original-1.jpg" to EnhancementMode.WHITEBOARD
                ),
                calls
            )
        }
    }

    @Test
    fun changeMode_ignoresOlderResultWhenNewerRequestFinishesLater() = runTest {
        val bookResult = CompletableDeferred<String>()
        val whiteboardResult = CompletableDeferred<String>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>
            ): String = when (mode) {
                EnhancementMode.BOOK -> bookResult.await()
                EnhancementMode.WHITEBOARD -> whiteboardResult.await()
                EnhancementMode.DOCUMENT -> "$originalPath-document.jpg"
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.uiState.test {
            assertEquals(PageReviewState(), awaitItem())

            viewModel.loadDraft(
                originalPath = "files/original-1.jpg",
                corners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
            )
            val loadedState = awaitItem()
            assertEquals("", loadedState.processedPath)
            assertEquals(false, loadedState.isProcessing)

            viewModel.changeMode(EnhancementMode.BOOK)
            val loadingBookState = awaitItem()
            assertEquals(EnhancementMode.BOOK, loadingBookState.mode)
            assertEquals(true, loadingBookState.isProcessing)
            assertEquals("", loadingBookState.processedPath)

            viewModel.changeMode(EnhancementMode.WHITEBOARD)
            val loadingWhiteboardState = awaitItem()
            assertEquals(EnhancementMode.WHITEBOARD, loadingWhiteboardState.mode)
            assertEquals(true, loadingWhiteboardState.isProcessing)
            assertEquals("", loadingWhiteboardState.processedPath)

            whiteboardResult.complete("files/original-1.jpg-whiteboard.jpg")
            dispatcher.scheduler.advanceUntilIdle()

            val whiteboardState = awaitItem()
            assertEquals("files/original-1.jpg-whiteboard.jpg", whiteboardState.processedPath)
            assertEquals(EnhancementMode.WHITEBOARD, whiteboardState.mode)
            assertEquals(false, whiteboardState.isProcessing)
            assertEquals(null, whiteboardState.errorMessage)

            bookResult.complete("files/original-1.jpg-book.jpg")
            dispatcher.scheduler.advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun changeMode_exposesProcessorFailureInState() = runTest {
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>
            ): String {
                throw IllegalStateException("processing failed")
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.uiState.test {
            assertEquals(PageReviewState(), awaitItem())

            viewModel.loadDraft(
                originalPath = "files/original-1.jpg",
                corners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
            )
            awaitItem()

            viewModel.changeMode(EnhancementMode.BOOK)
            val loadingState = awaitItem()
            assertEquals(EnhancementMode.BOOK, loadingState.mode)
            assertEquals(true, loadingState.isProcessing)
            assertEquals(null, loadingState.errorMessage)

            dispatcher.scheduler.advanceUntilIdle()

            val failedState = awaitItem()
            assertEquals("files/original-1.jpg", failedState.originalPath)
            assertEquals("", failedState.processedPath)
            assertEquals(EnhancementMode.BOOK, failedState.mode)
            assertEquals(false, failedState.isProcessing)
            assertEquals("processing failed", failedState.errorMessage)
        }
    }
}
