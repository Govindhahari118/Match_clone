package com.match.app.data.repo

import com.match.app.data.remote.FirestoreChatService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure JVM coverage for the canonical chat identity contract.
 *
 * The repository itself depends on Android Context, Room and Firebase. Those boundaries are
 * covered by Android/integration tests; this unit test protects the deterministic remote thread
 * identifier that must remain stable across devices and message direction.
 */
class ChatRepositoryTest {

    @Test
    fun `thread id is deterministic regardless of sender order`() {
        val first = FirestoreChatService.threadId("uid-alice", "uid-bob")
        val reversed = FirestoreChatService.threadId("uid-bob", "uid-alice")

        assertEquals(first, reversed)
        assertEquals(64, first.length)
    }

    @Test
    fun `different participant pairs produce different thread ids`() {
        val one = FirestoreChatService.threadId("uid-alice", "uid-bob")
        val two = FirestoreChatService.threadId("uid-alice", "uid-charlie")

        assertNotEquals(one, two)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `same firebase uid cannot create a chat thread`() {
        FirestoreChatService.threadId("uid-alice", "uid-alice")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank firebase uid cannot create a chat thread`() {
        FirestoreChatService.threadId("", "uid-bob")
    }
}
