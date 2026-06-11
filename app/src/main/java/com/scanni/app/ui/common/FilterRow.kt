package com.scanni.app.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scanni.app.R
import com.scanni.app.domain.model.ScanFilter

fun ScanFilter.labelRes(): Int = when (this) {
    ScanFilter.ORIGINAL -> R.string.filter_original
    ScanFilter.AUTO -> R.string.filter_auto
    ScanFilter.GRAYSCALE -> R.string.filter_grayscale
    ScanFilter.BLACK_WHITE -> R.string.filter_bw
    ScanFilter.WHITEBOARD -> R.string.filter_whiteboard
    ScanFilter.PHOTO -> R.string.filter_photo
}

/** Horizontal filter picker with live mini-previews. */
@Composable
fun FilterRow(
    selected: ScanFilter,
    chipPathFor: (ScanFilter) -> String?,
    onSelect: (ScanFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ScanFilter.entries.size, key = { ScanFilter.entries[it].name }) { index ->
            val filter = ScanFilter.entries[index]
            val isSelected = filter == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(filter) },
            ) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = MaterialTheme.shapes.small,
                        ),
                ) {
                    PageImage(
                        path = chipPathFor(filter),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(filter.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
