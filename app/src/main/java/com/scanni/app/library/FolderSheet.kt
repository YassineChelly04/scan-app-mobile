package com.scanni.app.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FolderSheet(
    folders: List<LibraryFolderItem>,
    selectedFolderId: Long?,
    onFolderClick: (Long?) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Folders")
        Text(
            text = "All documents",
            modifier = Modifier.clickable { onFolderClick(null) }
        )
        folders.forEach { folder ->
            val folderLabel = if (folder.id == selectedFolderId) {
                "${folder.name} (Selected)"
            } else {
                folder.name
            }
            Text(
                text = folderLabel,
                modifier = Modifier.clickable { onFolderClick(folder.id) }
            )
        }
    }
}
