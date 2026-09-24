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

/** Dedicated Coil model for media that must pass current Firebase Storage authorization. */
data class ProtectedFirebaseStorageModel(val source: String)

/** Claims only Firebase Storage references; all other String models continue through Coil defaults. */
class ProtectedFirebaseStorageMapper : Mapper<String, ProtectedFirebaseStorageModel> {
    override fun map(data: String, options: Options): ProtectedFirebaseStorageModel? {
        val normalized = data.trim()
        return normalized.takeIf(::isProtectedFirebaseStorageSource)
            ?.let(::ProtectedFirebaseStorageModel)
    }
}

/**
 * No memory-cache key is provided for protected media. Each visual request reaches the authenticated
 * fetcher, which re-checks Storage Rules before its private byte cache can be reused.
 */
class ProtectedFirebaseStorageKeyer : Keyer<ProtectedFirebaseStorageModel> {
    override fun key(data: ProtectedFirebaseStorageModel, options: Options): String? = null
}

class ProtectedFirebaseStorageFetcher(
    private val context: Context,
    private val model: ProtectedFirebaseStorageModel,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val file = resolveProtectedMediaFile(context, model.source) ?: return null
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
