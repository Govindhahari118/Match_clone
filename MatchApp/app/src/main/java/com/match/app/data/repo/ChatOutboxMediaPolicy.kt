package com.match.app.data.repo

import java.io.File

/**
 * Identifies files owned by the durable chat outbox. Cleanup must never delete an arbitrary path
 * supplied by a content provider or another app directory.
 */
internal object ChatOutboxMediaPolicy {
    fun isManagedPath(filesDir: File, path: String): Boolean {
        if (path.isBlank()) return false
        return runCatching {
            val root = File(filesDir, "chat_outbox").canonicalFile
            val candidate = File(path).canonicalFile
            candidate.parentFile == root
        }.getOrDefault(false)
    }

    fun deleteIfManaged(filesDir: File, path: String) {
        if (isManagedPath(filesDir, path)) {
            runCatching { File(path).delete() }
        }
    }
}
