package com.scanni.app.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val _uiState = MutableStateFlow(PageReviewState())
    val uiState: StateFlow<PageReviewState> = _uiState.asStateFlow()
    private var processingJob: Job? = null
    private var activeRequestId: Long = 0L

    fun loadDraft(originalPath: String, corners: List<Float>) {
        processingJob?.cancel()
        activeRequestId += 1
        _uiState.value = PageReviewState(
            originalPath = originalPath,
            processedPath = "",
            mode = EnhancementMode.DOCUMENT,
            corners = corners,
            isProcessing = false,
            errorMessage = null
        )
    }

    fun changeMode(mode: EnhancementMode) {
        val currentState = _uiState.value
        if (currentState.originalPath.isBlank()) {
            return
        }

        processingJob?.cancel()
        activeRequestId += 1
        val requestId = activeRequestId
        _uiState.update { state ->
            state.copy(
                processedPath = "",
                mode = mode,
                isProcessing = true,
                errorMessage = null
            )
        }
        processingJob = viewModelScope.launch {
            try {
                val processedPath = processor.process(
                    originalPath = currentState.originalPath,
                    mode = mode,
                    corners = currentState.corners
                )

                if (requestId != activeRequestId) {
                    return@launch
                }

                _uiState.update { state ->
                    state.copy(
                        processedPath = processedPath,
                        mode = mode,
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

                _uiState.update { state ->
                    state.copy(
                        processedPath = "",
                        mode = mode,
                        isProcessing = false,
                        errorMessage = error.message ?: error::class.simpleName ?: "Processing failed"
                    )
                }
            }
        }
    }
}
