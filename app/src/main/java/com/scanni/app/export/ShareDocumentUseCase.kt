package com.scanni.app.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import java.io.File

class ShareDocumentUseCase {
    fun createIntent(context: Context, file: File): Intent {
        val uri = LocalPdfShareProvider.getUriForFile(context, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            clipData = ClipData.newRawUri(file.name, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
