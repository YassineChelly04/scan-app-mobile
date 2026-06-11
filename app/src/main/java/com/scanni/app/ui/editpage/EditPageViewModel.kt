package com.scanni.app.ui.editpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.image.ImageIo
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.model.Page
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.processing.PageProcessor
import com.scanni.app.domain.repo.DocumentRepository
import com.scanni.app.domain.usecase.UpdatePageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditPageUiState(
    val page: Page? = null,
    val quad: Quad? = null,
    val detectedQuad: Quad? = null,
    val rotationDeg: Int = 0,
    val filter: ScanFilter = ScanFilter.AUTO,
    val previewPath: String? = null,
    val previewKey: String = "",
    val processing: Boolean = true,
    val filterChips: Map<String, String> = emptyMap(),
    val cropping: Boolean = false,
    val saving: Boolean = false,
)

sealed interface EditPageEvent {
    data object Saved : EditPageEvent
    data object NotFound : EditPageEvent
}

class EditPageViewModel(
    private val pageId: String,
    private val repository: DocumentRepository,
    private val fileStore: PageFileStore,
    private val processor: PageProcessor,
    private val updatePage: UpdatePageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPageUiState())
    val uiState: StateFlow<EditPageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditPageEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<EditPageEvent> = _events

    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val page = repository.getPage(pageId)
            if (page == null) {
                _events.tryEmit(EditPageEvent.NotFound)
                return@launch
            }
            _uiState.update {
                it.copy(
                    page = page,
                    quad = page.quad,
                    rotationDeg = page.rotationDeg,
                    filter = page.filter,
                )
            }
            renderPreview()
            renderFilterChips(page)
        }
    }

    fun setFilter(filter: ScanFilter) {
        _uiState.update { it.copy(filter = filter) }
        renderPreview()
    }

    fun rotate() {
        _uiState.update { it.copy(rotationDeg = (it.rotationDeg + 90) % 360) }
        renderPreview()
    }

    fun openCrop() = _uiState.update { it.copy(cropping = true) }

    fun closeCrop() = _uiState.update { it.copy(cropping = false) }

    fun applyCrop(quad: Quad) {
        _uiState.update { it.copy(quad = quad, cropping = false) }
        renderPreview()
        _uiState.value.page?.let { renderFilterChips(it) }
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.saving || state.page == null) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                updatePage(pageId, state.quad, state.rotationDeg, state.filter)
            }
            fileStore.clearPreviews()
            _events.tryEmit(EditPageEvent.Saved)
            onDone()
        }
    }

    private fun renderPreview() {
        val state = _uiState.value
        val page = state.page ?: return
        val key = "${state.quad?.encode()}|${state.rotationDeg}|${state.filter}".hashCode().toString()
        if (state.previewKey == key && state.previewPath != null) return
        previewJob?.cancel()
        _uiState.update { it.copy(processing = true, previewKey = key) }
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            val path = runCatching {
                val bitmap = processor.render(
                    originalPath = page.originalPath,
                    quad = _uiState.value.quad,
                    rotationDeg = _uiState.value.rotationDeg,
                    filter = _uiState.value.filter,
                    maxDimension = PageProcessor.PREVIEW_SIZE,
                )
                val file = fileStore.previewFile("edit_${pageId}_$key")
                ImageIo.saveJpeg(bitmap, file, quality = 88)
                bitmap.recycle()
                file.absolutePath
            }.getOrNull()
            _uiState.update { it.copy(previewPath = path, processing = false) }
        }
    }

    private fun renderFilterChips(page: Page) {
        viewModelScope.launch(Dispatchers.Default) {
            val quad = _uiState.value.quad
            val cropKey = quad?.encode().hashCode().toString()
            for (filter in ScanFilter.entries) {
                val path = runCatching {
                    val bitmap = processor.render(
                        originalPath = page.originalPath,
                        quad = quad,
                        rotationDeg = 0,
                        filter = filter,
                        maxDimension = PageProcessor.FILTER_CHIP_SIZE,
                    )
                    val file = fileStore.previewFile("editchip_${pageId}_${filter.name}_$cropKey")
                    ImageIo.saveJpeg(bitmap, file, quality = 80)
                    bitmap.recycle()
                    file.absolutePath
                }.getOrNull() ?: continue
                _uiState.update { it.copy(filterChips = it.filterChips + (filter.name to path)) }
            }
        }
    }
}
