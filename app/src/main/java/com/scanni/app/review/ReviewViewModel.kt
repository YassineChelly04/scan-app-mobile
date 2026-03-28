package com.scanni.app.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scanni.app.camera.CapturedPageDraft
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val processor: PageProcessor
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
    private var processingJob: Job? = null
    private var activeRequestId: Long = 0L

    fun loadSession(drafts: List<CapturedPageDraft>) {
        processingJob?.cancel()
        activeRequestId += 1
        val pages = drafts.mapIndexed { index, draft ->
            ReviewPageState(
                originalPath = draft.originalPath,
                corners = draft.detectedCorners,
                isProcessing = index == 0 && drafts.isNotEmpty()
            )
        }
        _uiState.value = ReviewUiState(
            pages = pages
        )
        if (drafts.isNotEmpty()) {
            processPage(index = 0, skipLoadingUpdate = true)
        }
    }

    fun selectPage(index: Int) {
        if (index !in _uiState.value.pages.indices) {
            return
        }

        _uiState.update { state ->
            state.copy(activePageIndex = index)
        }
        val selectedPage = _uiState.value.pages[index]
        if (selectedPage.processedPath.isBlank() && !selectedPage.isProcessing) {
            processPage(index)
        }
    }

    fun updateActiveCorners(corners: List<Float>) {
        val activePage = _uiState.value.activePage ?: return
        updatePage(_uiState.value.activePageIndex) {
            activePage.copy(
                corners = corners,
                processedPath = "",
                errorMessage = null
            )
        }
    }

    fun confirmActiveCrop() {
        val activeIndex = _uiState.value.activePageIndex
        if (activeIndex !in _uiState.value.pages.indices) {
            return
        }
        processPage(activeIndex)
    }

    fun changeMode(mode: EnhancementMode) {
        val activePage = _uiState.value.activePage ?: return

        updatePage(_uiState.value.activePageIndex) {
            activePage.copy(
                mode = mode,
                processedPath = "",
                errorMessage = null
            )
        }
        processPage(
            index = _uiState.value.activePageIndex,
            mode = mode
        )
    }

    private fun processPage(
        index: Int,
        mode: EnhancementMode? = null,
        corners: List<Float>? = null,
        skipLoadingUpdate: Boolean = false
    ) {
        val currentPage = _uiState.value.pages.getOrNull(index) ?: return
        if (currentPage.originalPath.isBlank()) {
            return
        }
        val nextMode = mode ?: currentPage.mode
        val nextCorners = corners ?: currentPage.corners

        processingJob?.cancel()
        activeRequestId += 1
        val requestId = activeRequestId
        if (!skipLoadingUpdate) {
            updatePage(index) { page ->
                page.copy(
                    processedPath = "",
                    mode = nextMode,
                    isProcessing = true,
                    errorMessage = null
                )
            }
        }
        processingJob = viewModelScope.launch {
            try {
                val processedPath = processor.process(
                    originalPath = currentPage.originalPath,
                    mode = nextMode,
                    corners = nextCorners
                )

                if (requestId != activeRequestId) {
                    return@launch
                }

                updatePage(index) { page ->
                    page.copy(
                        processedPath = processedPath,
                        mode = nextMode,
                        isProcessing = false,
                        errorMessage = null
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (requestId != activeRequestId) {
                    return@launch
                }

                updatePage(index) { page ->
                    page.copy(
                        processedPath = "",
                        mode = nextMode,
                        isProcessing = false,
                        errorMessage = error.message ?: error::class.simpleName ?: "Processing failed"
                    )
                }
            }
        }
    }

    private fun updatePage(index: Int, transform: (ReviewPageState) -> ReviewPageState) {
        _uiState.update { state ->
            if (index !in state.pages.indices) {
                state
            } else {
                state.copy(
                    pages = state.pages.toMutableList().also { pages ->
                        pages[index] = transform(pages[index])
                    }
                )
            }
        }
    }

    companion object {
        fun factory(processor: PageProcessor): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ReviewViewModel(processor) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
