package com.match.app.data.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChatOutboxMediaPolicyTest {

    @Test
    fun `only direct files in chat outbox are managed`() {
        val filesDir = File("/tmp/matree-files")
        assertTrue(
            ChatOutboxMediaPolicy.isManagedPath(
                filesDir,
                File(filesDir, "chat_outbox/client_123.jpg").path
            )
        )
        assertFalse(
            ChatOutboxMediaPolicy.isManagedPath(
                filesDir,
                File(filesDir, "protected_media/client_123.jpg").path
            )
        )
        assertFalse(
            ChatOutboxMediaPolicy.isManagedPath(
                filesDir,
                File(filesDir, "chat_outbox/nested/client_123.jpg").path
            )
        )
        assertFalse(ChatOutboxMediaPolicy.isManagedPath(filesDir, ""))
    }

    @Test
    fun `path traversal cannot escape managed directory`() {
        val filesDir = File("/tmp/matree-files")
        val escaped = File(filesDir, "chat_outbox/../protected_media/secret.jpg").path
        assertFalse(ChatOutboxMediaPolicy.isManagedPath(filesDir, escaped))
    }
}
