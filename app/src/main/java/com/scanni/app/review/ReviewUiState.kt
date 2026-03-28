package com.scanni.app.review

data class ReviewUiState(
    val pages: List<ReviewPageState> = emptyList(),
    val activePageIndex: Int = 0,
    val isSaving: Boolean = false,
    val saveErrorMessage: String? = null
) {
    val activePage: ReviewPageState?
        get() = pages.getOrNull(activePageIndex)
}
