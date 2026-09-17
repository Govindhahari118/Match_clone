package com.match.app.ui.common

import android.content.Context
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.map.Mapper
import coil.request.Options
import okio.Path.Companion.toOkioPath

/** Dedicated Coil model for protected Firebase Storage media. */
data class ProtectedFirebaseStorageModel(val gsUrl: String)

/**
 * Claims only protected `gs://` strings. Returning null for every other String preserves Coil's
 * normal HTTP/content/file mapping chain.
 */
class ProtectedFirebaseStorageMapper : Mapper<String, ProtectedFirebaseStorageModel> {
    override fun map(data: String, options: Options): ProtectedFirebaseStorageModel? {
        val normalized = data.trim()
        return normalized.takeIf { it.startsWith("gs://", ignoreCase = true) }
            ?.let(::ProtectedFirebaseStorageModel)
    }
}

/**
 * Protected media intentionally has no Coil memory-cache key. Every visual request must reach the
 * authenticated fetcher so Firebase Storage Rules are re-evaluated before our private byte cache
 * can be reused.
 */
class ProtectedFirebaseStorageKeyer : Keyer<ProtectedFirebaseStorageModel> {
    override fun key(data: ProtectedFirebaseStorageModel, options: Options): String? = null
}

class ProtectedFirebaseStorageFetcher(
    private val context: Context,
    private val model: ProtectedFirebaseStorageModel,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val file = resolveProtectedMediaFile(context, model.gsUrl) ?: return null
        return SourceResult(
            source = ImageSource(file = file.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<ProtectedFirebaseStorageModel> {
        override fun create(
            data: ProtectedFirebaseStorageModel,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ProtectedFirebaseStorageFetcher(context.applicationContext, data)
    }
}
