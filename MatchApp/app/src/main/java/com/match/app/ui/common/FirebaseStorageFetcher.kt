package com.match.app.ui.common

import android.content.Context
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import java.io.File
import okio.Path.Companion.toOkioPath

/**
 * Global Coil fetcher for protected Firebase Storage `gs://` references.
 *
 * The default Coil fetchers continue handling HTTP, content and local files. This factory only
 * claims gs:// strings and resolves them through Firebase Auth/Storage Rules first.
 */
class FirebaseStorageFetcher(
    private val context: Context,
    private val gsUrl: String,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val file = resolveProtectedMediaFile(context, gsUrl) ?: return null
        return SourceResult(
            source = ImageSource(file = file.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.startsWith("gs://", ignoreCase = true)) {
                FirebaseStorageFetcher(context.applicationContext, data)
            } else {
                null
            }
        }
    }
}
