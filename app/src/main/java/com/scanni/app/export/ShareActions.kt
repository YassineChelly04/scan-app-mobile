package com.scanni.app.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Hands exported files and recognized text to other apps. */
object ShareActions {

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sharePdf(context: Context, file: File) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareImages(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map { contentUri(context, it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /** Copies [source] into a user-picked SAF target (ACTION_CREATE_DOCUMENT result). */
    suspend fun copyToUri(context: Context, source: File, target: Uri) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(target)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Cannot open $target")
        }
}
