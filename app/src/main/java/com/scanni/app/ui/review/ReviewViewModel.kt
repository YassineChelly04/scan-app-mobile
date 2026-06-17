package com.scanni.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.core.geometry.Quad
import com.scanni.app.data.files.PageFileStore
import com.scanni.app.domain.ScanSession
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.processing.PageProcessor
import com.scanni.app.domain.usecase.SaveScanUseCase
import com.scanni.app.core.image.ImageIo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A rendered page preview on disk; [key] identifies the edit state it reflects. */
data class PagePreview(
    val key: String,
    val path: String? = null,
    val processing: Boolean = true,
)

data class ReviewUiState(
    val pages: List<CapturedPage> = emptyList(),
    val previews: Map<String, PagePreview> = emptyMap(),
    /** "<pageId>:<filter>" -> rendered chip path. */
    val filterChips: Map<String, String> = emptyMap(),
    val croppingPageId: String? = null,
    val saving: Boolean = false,
)

sealed interface ReviewEvent {
    data class Saved(val documentId: String) : ReviewEvent
    data object SaveFailed : ReviewEvent
}

class ReviewViewModel(
    private val session: ScanSession,
    private val fileStore: PageFileStore,
    private val processor: PageProcessor,
    private val saveScan: SaveScanUseCase,
) : ViewModel() {

    private val previews = MutableStateFlow<Map<String, PagePreview>>(emptyMap())
    private val filterChips = MutableStateFlow<Map<String, String>>(emptyMap())
    private val croppingPageId = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)

    private val previewJobs = ConcurrentHashMap<String, Job>()
    private val chipJobs = ConcurrentHashMap<String, Job>()

    private val _events = MutableSharedFlow<ReviewEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ReviewEvent> = _events

    val uiState: StateFlow<ReviewUiState> = combine(
        session.pages,
        previews,
        filterChips,
        croppingPageId,
        saving,
    ) { pages, previews, chips, cropping, saving ->
        ReviewUiState(
            pages = pages,
            previews = previews,
            filterChips = chips,
            croppingPageId = cropping,
            saving = saving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        // Seed with the session's current pages so the screen never momentarily
        // looks empty on entry — otherwise the "no pages left -> leave" guard in
        // ReviewScreen fires before the combined state arrives and bounces back.
        ReviewUiState(pages = session.pages.value),
    )

    init {
        viewModelScope.launch {
            session.pages.collect { pages -> pages.forEach(::ensurePreview) }
        }
    }

    // --- Preview rendering ---

    private fun renderKey(page: CapturedPage): String =
        "${page.quad.encode()}|${page.rotationDeg}|${page.filter}".hashCode().toString()

    private fun ensurePreview(page: CapturedPage) {
        val key = renderKey(page)
        if (previews.value[page.id]?.key == key) return
        previewJobs[page.id]?.cancel()
        previews.update { current ->
            current + (page.id to PagePreview(key = key, path = current[page.id]?.path, processing = true))
        }
        previewJobs[page.id] = viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                val bitmap = processor.render(
                    originalPath = page.originalPath,
                    quad = page.quad,
                    rotationDeg = page.rotationDeg,
                    filter = page.filter,
                    maxDimension = PageProcessor.PREVIEW_SIZE,
                )
                val file = fileStore.previewFile("${page.id}_$key")
                ImageIo.saveJpeg(bitmap, file, quality = 88)
                bitmap.recycle()
                file.absolutePath
            }.getOrNull()
            previews.update { current ->
                current + (page.id to PagePreview(key = key, path = result, processing = false))
            }
        }
    }

    /** Renders the small per-filter previews shown in the filter carousel. */
    fun ensureFilterChips(pageId: String) {
        val page = session.pages.value.firstOrNull { it.id == pageId } ?: return
        val cropKey = "${page.quad.encode()}".hashCode().toString()
        val missing = ScanFilter.entries.filter { filter ->
            "${page.id}:${filter.name}" !in filterChips.value ||
                chipCropKeys[page.id] != cropKey
        }
        if (missing.isEmpty()) return
        chipCropKeys[page.id] = cropKey
        chipJobs[page.id]?.cancel()
        chipJobs[page.id] = viewModelScope.launch(Dispatchers.Default) {
            for (filter in ScanFilter.entries) {
                val result = runCatching {
                    val bitmap = processor.render(
                        originalPath = page.originalPath,
                        quad = page.quad,
                        rotationDeg = 0,
                        filter = filter,
                        maxDimension = PageProcessor.FILTER_CHIP_SIZE,
                    )
                    val file = fileStore.previewFile("chip_${page.id}_${filter.name}_$cropKey")
                    ImageIo.saveJpeg(bitmap, file, quality = 80)
                    bitmap.recycle()
                    file.absolutePath
                }.getOrNull() ?: continue
                filterChips.update { it + ("${page.id}:${filter.name}" to result) }
            }
        }
    }

    private val chipCropKeys = HashMap<String, String>()

    // --- Edits ---

    fun setFilter(pageId: String, filter: ScanFilter) {
        session.update(pageId) { it.copy(filter = filter) }
    }

    fun rotate(pageId: String) {
        session.update(pageId) { it.copy(rotationDeg = (it.rotationDeg + 90) % 360) }
    }

    fun openCrop(pageId: String) {
        croppingPageId.value = pageId
    }

    fun closeCrop() {
        croppingPageId.value = null
    }

    fun applyCrop(pageId: String, quad: Quad) {
        session.update(pageId) { it.copy(quad = quad) }
        croppingPageId.value = null
    }

    fun deletePage(pageId: String) {
        val removed = session.remove(pageId)
        removed?.let { fileStore.deleteSessionFile(it.originalPath) }
        previewJobs.remove(pageId)?.cancel()
        chipJobs.remove(pageId)?.cancel()
        previews.update { it - pageId }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        session.move(fromIndex, toIndex)
    }

    fun save(title: String) {
        if (saving.value) return
        val pages = session.pages.value
        if (pages.isEmpty()) return
        saving.value = true
        viewModelScope.launch {
            runCatching { saveScan(title, null, pages) }
                .onSuccess { documentId ->
                    session.clear()
                    fileStore.clearPreviews()
                    fileStore.clearSession()
                    _events.tryEmit(ReviewEvent.Saved(documentId))
                }
                .onFailure {
                    saving.value = false
                    _events.tryEmit(ReviewEvent.SaveFailed)
                }
        }
    }

    /** Discards the whole scan session including captured files. */
    fun discard() {
        session.clear()
        fileStore.clearSession()
        fileStore.clearPreviews()
    }
}
