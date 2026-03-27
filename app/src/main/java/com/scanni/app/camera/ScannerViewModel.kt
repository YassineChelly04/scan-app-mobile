package com.scanni.app.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScannerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onPageCaptured(page: CapturedPageDraft) {
        _uiState.update { state ->
            val updatedPages = state.pages + page
            state.copy(
                pages = updatedPages,
                captureCount = updatedPages.size
            )
        }
    }

    fun clearSession() {
        _uiState.value = ScannerUiState()
    }
}
