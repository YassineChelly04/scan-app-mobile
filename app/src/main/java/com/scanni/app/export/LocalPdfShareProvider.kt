package com.scanni.app.export

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

class LocalPdfShareProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val file = requireResolvedFile(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(columns)
        val values = columns.map { column ->
            when (column) {
                OpenableColumns.DISPLAY_NAME -> file.name
                OpenableColumns.SIZE -> file.length()
                else -> null
            }
        }.toTypedArray()
        cursor.addRow(values)
        return cursor
    }

    override fun getType(uri: Uri): String = "application/pdf"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = requireResolvedFile(uri)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun requireResolvedFile(uri: Uri): File {
        val appContext = context?.applicationContext
            ?: throw FileNotFoundException("Provider context is unavailable.")
        val resolved = resolveFile(appContext, uri)
            ?: throw FileNotFoundException("Unsupported uri: $uri")

        if (!resolved.exists()) {
            throw FileNotFoundException("File does not exist: ${resolved.path}")
        }

        return resolved
    }

    companion object {
        private const val CACHE_ROOT = "cache"
        private const val FILES_ROOT = "files"
        private const val AUTHORITY_SUFFIX = ".pdfprovider"

        fun getUriForFile(context: Context, file: File): Uri {
            val target = file.canonicalFile
            val roots = listOf(
                CACHE_ROOT to context.cacheDir.canonicalFile,
                FILES_ROOT to context.filesDir.canonicalFile
            )

            val (rootName, rootDir) = roots.firstOrNull { (_, root) ->
                target.path == root.path || target.path.startsWith(root.path + File.separator)
            } ?: throw IllegalArgumentException(
                "File must be located under the app cacheDir or filesDir."
            )

            val relativePath = target.path.removePrefix(rootDir.path).trimStart(File.separatorChar)
            val builder = Uri.Builder()
                .scheme("content")
                .authority(context.packageName + AUTHORITY_SUFFIX)
                .appendPath(rootName)

            relativePath.split(File.separatorChar)
                .filter { it.isNotEmpty() }
                .forEach(builder::appendPath)

            return builder.build()
        }

        private fun resolveFile(context: Context, uri: Uri): File? {
            val segments = uri.pathSegments
            if (segments.size < 2) {
                return null
            }

            val root = when (segments.first()) {
                CACHE_ROOT -> context.cacheDir.canonicalFile
                FILES_ROOT -> context.filesDir.canonicalFile
                else -> return null
            }

            val relativePath = segments.drop(1).joinToString(File.separator)
            val resolved = File(root, relativePath).canonicalFile
            return resolved.takeIf {
                it.path == root.path || it.path.startsWith(root.path + File.separator)
            }
        }
    }
}
