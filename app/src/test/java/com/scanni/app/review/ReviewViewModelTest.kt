package com.scanni.app.review

import app.cash.turbine.test
import com.scanni.app.camera.CapturedPageDraft
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun loadSession_initializesAllPagesAndProcessesFirstPage() = runTest {
        val requests = mutableListOf<Triple<String, EnhancementMode, List<Float>>>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String {
                requests += Triple(originalPath, mode, corners)
                return "$originalPath-${mode.name.lowercase()}.jpg"
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.uiState.test {
            assertEquals(ReviewUiState(), awaitItem())

            viewModel.loadSession(
                listOf(
                    CapturedPageDraft(
                        originalPath = "files/page-1.jpg",
                        previewPath = "files/page-1-preview.jpg",
                        detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                    ),
                    CapturedPageDraft(
                        originalPath = "files/page-2.jpg",
                        previewPath = "files/page-2-preview.jpg",
                        detectedCorners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)
                    )
                )
            )

            val loadingState = awaitItem()
            assertEquals(2, loadingState.pages.size)
            assertEquals(0, loadingState.activePageIndex)
            assertEquals(true, loadingState.activePage?.isProcessing)
            assertEquals("", loadingState.activePage?.processedPath)

            dispatcher.scheduler.advanceUntilIdle()

            val loadedState = awaitItem()
            assertEquals("files/page-1.jpg-document.jpg", loadedState.activePage?.processedPath)
            assertEquals(false, loadedState.activePage?.isProcessing)
            assertEquals(
                listOf(
                    Triple(
                        "files/page-1.jpg",
                        EnhancementMode.DOCUMENT,
                        listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                    )
                ),
                requests
            )
        }
    }

    @Test
    fun selectPage_preservesPerPageModeAndCorners() = runTest {
        val requests = mutableListOf<Triple<String, EnhancementMode, List<Float>>>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String {
                requests += Triple(originalPath, mode, corners)
                return "$originalPath-${mode.name.lowercase()}.jpg"
            }
        }
        val updatedCorners = listOf(0.05f, 0.05f, 0.95f, 0.05f, 0.95f, 0.95f, 0.05f, 0.95f)
        val viewModel = ReviewViewModel(processor)

        viewModel.loadSession(
            listOf(
                CapturedPageDraft(
                    originalPath = "files/page-1.jpg",
                    previewPath = "files/page-1-preview.jpg",
                    detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                ),
                CapturedPageDraft(
                    originalPath = "files/page-2.jpg",
                    previewPath = "files/page-2-preview.jpg",
                    detectedCorners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.changeMode(EnhancementMode.BOOK)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateActiveCorners(updatedCorners)
        viewModel.confirmActiveCrop()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectPage(1)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectPage(0)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.activePageIndex)
        assertEquals(EnhancementMode.BOOK, state.activePage?.mode)
        assertEquals(updatedCorners, state.activePage?.corners)
        assertEquals("files/page-1.jpg-book.jpg", state.activePage?.processedPath)
        assertEquals(
            listOf(
                Triple("files/page-1.jpg", EnhancementMode.DOCUMENT, listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)),
                Triple("files/page-1.jpg", EnhancementMode.BOOK, listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)),
                Triple("files/page-1.jpg", EnhancementMode.BOOK, updatedCorners),
                Triple("files/page-2.jpg", EnhancementMode.DOCUMENT, listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f))
            ),
            requests
        )
    }

    @Test
    fun confirmActiveCrop_reprocessesOnlyTheSelectedPage() = runTest {
        val requests = mutableListOf<Triple<String, EnhancementMode, List<Float>>>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String {
                requests += Triple(originalPath, mode, corners)
                return "$originalPath-${mode.name.lowercase()}.jpg"
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.loadSession(
            listOf(
                CapturedPageDraft(
                    originalPath = "files/page-1.jpg",
                    previewPath = "files/page-1-preview.jpg",
                    detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                ),
                CapturedPageDraft(
                    originalPath = "files/page-2.jpg",
                    previewPath = "files/page-2-preview.jpg",
                    detectedCorners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()
        requests.clear()

        viewModel.selectPage(1)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.updateActiveCorners(listOf(0.2f, 0.2f, 0.8f, 0.2f, 0.8f, 0.8f, 0.2f, 0.8f))
        viewModel.confirmActiveCrop()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Triple("files/page-2.jpg", EnhancementMode.DOCUMENT, listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)),
                Triple("files/page-2.jpg", EnhancementMode.DOCUMENT, listOf(0.2f, 0.2f, 0.8f, 0.2f, 0.8f, 0.8f, 0.2f, 0.8f))
            ),
            requests
        )
    }

    @Test
    fun processingFailure_staysOnTheFailingPageOnly() = runTest {
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String {
                if (originalPath.contains("page-2")) {
                    throw IllegalStateException("processing failed")
                }
                return "$originalPath-${mode.name.lowercase()}.jpg"
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.loadSession(
            listOf(
                CapturedPageDraft(
                    originalPath = "files/page-1.jpg",
                    previewPath = "files/page-1-preview.jpg",
                    detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
                ),
                CapturedPageDraft(
                    originalPath = "files/page-2.jpg",
                    previewPath = "files/page-2-preview.jpg",
                    detectedCorners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPage(1)
        dispatcher.scheduler.advanceUntilIdle()

        val failedState = viewModel.uiState.value
        assertEquals(1, failedState.activePageIndex)
        assertEquals("", failedState.activePage?.processedPath)
        assertEquals(false, failedState.activePage?.isProcessing)
        assertEquals("processing failed", failedState.activePage?.errorMessage)
        assertEquals("files/page-1.jpg-document.jpg", failedState.pages[0].processedPath)
        assertNull(failedState.pages[0].errorMessage)
    }

    @Test
    fun rotateActivePage_updatesRotationAndReprocessesOnlyThatPage() = runTest {
        val requests = mutableListOf<ProcessRequest>()
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String {
                requests += ProcessRequest(originalPath, mode, corners, rotationQuarterTurns)
                return "$originalPath-$rotationQuarterTurns.jpg"
            }
        }
        val viewModel = ReviewViewModel(processor)

        viewModel.loadSession(sampleDrafts())
        dispatcher.scheduler.advanceUntilIdle()
        requests.clear()

        viewModel.rotateActivePage(clockwise = true)
        dispatcher.scheduler.advanceUntilIdle()

        val activePage = viewModel.uiState.value.activePage!!
        assertEquals(1, activePage.rotationQuarterTurns)
        assertEquals("files/page-1.jpg-1.jpg", activePage.processedPath)
        assertEquals(
            listOf(
                ProcessRequest(
                    originalPath = "files/page-1.jpg",
                    mode = EnhancementMode.DOCUMENT,
                    corners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
                    rotationQuarterTurns = 1
                )
            ),
            requests
        )
    }

    @Test
    fun deleteActivePage_removesItAndRepairsActiveIndex() = runTest {
        val viewModel = ReviewViewModel(successfulProcessor())

        viewModel.loadSession(sampleDrafts())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectPage(1)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteActivePage()

        val state = viewModel.uiState.value
        assertEquals(1, state.pages.size)
        assertEquals("files/page-1.jpg", state.activePage?.originalPath)
        assertEquals(0, state.activePageIndex)
    }

    @Test
    fun deleteActivePage_ignoresSingleRemainingPage() = runTest {
        val viewModel = ReviewViewModel(successfulProcessor())

        viewModel.loadSession(listOf(sampleDrafts().first()))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteActivePage()

        assertEquals(1, viewModel.uiState.value.pages.size)
    }

    @Test
    fun movePage_reordersPagesAndPreservesPageState() = runTest {
        val viewModel = ReviewViewModel(successfulProcessor())

        viewModel.loadSession(sampleDrafts())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.rotateActivePage(clockwise = true)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.movePage(fromIndex = 0, toIndex = 1)

        val state = viewModel.uiState.value
        assertEquals("files/page-2.jpg", state.pages[0].originalPath)
        assertEquals("files/page-1.jpg", state.pages[1].originalPath)
        assertEquals(1, state.pages[1].rotationQuarterTurns)
        assertEquals(1, state.activePageIndex)
    }

    private fun sampleDrafts(): List<CapturedPageDraft> =
        listOf(
            CapturedPageDraft(
                originalPath = "files/page-1.jpg",
                previewPath = "files/page-1-preview.jpg",
                detectedCorners = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
            ),
            CapturedPageDraft(
                originalPath = "files/page-2.jpg",
                previewPath = "files/page-2-preview.jpg",
                detectedCorners = listOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f)
            )
        )

    private fun successfulProcessor(): PageProcessor =
        object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>,
                rotationQuarterTurns: Int
            ): String = "$originalPath-${mode.name.lowercase()}-$rotationQuarterTurns.jpg"
        }

    private data class ProcessRequest(
        val originalPath: String,
        val mode: EnhancementMode,
        val corners: List<Float>,
        val rotationQuarterTurns: Int
    )
}
