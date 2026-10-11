package com.match.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListUiStateTest {
    private val aliceConversation = ConversationItem(
        peerId = 1L,
        peerFirebaseUid = "alice",
        peerName = "Alice",
        username = "alice",
        lastMessage = "Hello",
        lastAt = 100L,
        lastActiveAt = 90L,
        showLastActive = false,
        isVerified = true
    )

    @Test
    fun onlyTheActiveAccountCanSeeCachedConversationPreviews() {
        val loaded = ChatListUiState(
            accountUid = "account-a",
            items = listOf(aliceConversation),
            loading = false
        )
        assertEquals(listOf(aliceConversation), loaded.visibleConversationsFor("account-a"))
        assertTrue(loaded.visibleConversationsFor("account-b").isEmpty())
        assertTrue(loaded.visibleConversationsFor(null).isEmpty())
        assertTrue(loaded.visibleConversationsFor("").isEmpty())
    }

    @Test
    fun pendingAndFailedLoadsNeverExposeStalePreviews() {
        val pending = ChatListUiState(
            accountUid = "account-a",
            items = listOf(aliceConversation),
            loading = true
        )
        val failed = pending.copy(loading = false, failed = true)
        assertTrue(pending.visibleConversationsFor("account-a").isEmpty())
        assertTrue(failed.visibleConversationsFor("account-a").isEmpty())
    }
}
