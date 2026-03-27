package com.scanni.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowFileProvider::class])
class ShareDocumentUseCaseTest {
    @Test
    fun createIntent_usesFileProviderAuthority() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "shared/notes.pdf").apply {
            parentFile?.mkdirs()
            writeText("pdf")
        }

        val intent = ShareDocumentUseCase().createIntent(context, file)
        @Suppress("DEPRECATION")
        val streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals("${context.packageName}.fileprovider", streamUri?.authority)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}

@Implements(FileProvider::class)
class ShadowFileProvider {
    companion object {
        @Implementation
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun getUriForFile(unusedContext: Context, authority: String, file: File): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath(file.name)
                .build()
        }
    }
}
