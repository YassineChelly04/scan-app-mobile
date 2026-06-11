package com.scanni.app.domain

import com.scanni.app.core.util.move
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory state of an in-progress scan, shared between the scanner and the
 * review screen. Cleared when the scan is saved or discarded.
 */
class ScanSession {

    private val _pages = MutableStateFlow<List<CapturedPage>>(emptyList())
    val pages: StateFlow<List<CapturedPage>> = _pages

    private val _mode = MutableStateFlow(ScanMode.DOCUMENT)
    val mode: StateFlow<ScanMode> = _mode

    fun setMode(mode: ScanMode) {
        _mode.value = mode
    }

    fun add(page: CapturedPage) {
        _pages.update { it + page }
    }

    fun remove(pageId: String): CapturedPage? {
        var removed: CapturedPage? = null
        _pages.update { pages ->
            removed = pages.firstOrNull { it.id == pageId }
            pages.filterNot { it.id == pageId }
        }
        return removed
    }

    fun update(pageId: String, transform: (CapturedPage) -> CapturedPage) {
        _pages.update { pages ->
            pages.map { if (it.id == pageId) transform(it) else it }
        }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        _pages.update { it.move(fromIndex, toIndex) }
    }

    fun clear() {
        _pages.value = emptyList()
    }
}
