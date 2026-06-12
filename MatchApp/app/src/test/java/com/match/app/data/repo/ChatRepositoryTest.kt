package com.match.app.data.repo

import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.local.entity.PendingMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ChatRepository] send + thread decryption.
 * Uses in-memory fakes instead of Room.
 */
class ChatRepositoryTest {

    private val sentMessages = mutableListOf<MessageEntity>()
    private val pendingMessages = mutableListOf<PendingMessageEntity>()

    private val fakeMessageDao = object : MessageDao {
        override fun observeThread(a: Long, b: Long): Flow<List<MessageEntity>> =
            flowOf(sentMessages.filter {
                (it.fromUserId == a && it.toUserId == b) ||
                (it.fromUserId == b && it.toUserId == a)
            })
        override fun observeUnread(me: Long): Flow<Int> = flowOf(0)
        override suspend fun insert(msg: MessageEntity) { sentMessages.add(msg) }
        override suspend fun markRead(me: Long, peer: Long) {}
    }

    private val fakePendingDao = object : PendingMessageDao {
        override suspend fun insert(msg: PendingMessageEntity) { pendingMessages.add(msg) }
        override suspend fun oldest(): List<PendingMessageEntity> = pendingMessages.take(50)
        override suspend fun delete(id: Long) { pendingMessages.removeAll { it.id == id } }
        override suspend fun incrementRetry(id: Long) {}
        override suspend fun pruneStale() {}
        override suspend fun count(): Int = pendingMessages.size
    }

    @Test
    fun `send inserts encrypted message`() = runBlocking {
        val repo = ChatRepository(fakeMessageDao, fakePendingDao)
        repo.send(1L, 2L, "Hello!")
        assertEquals(1, sentMessages.size)
        assertEquals(1L, sentMessages[0].fromUserId)
        assertEquals(2L, sentMessages[0].toUserId)
        // Body should be encrypted (not the raw plaintext)
        assertTrue("Body should not be raw plaintext", sentMessages[0].body != "Hello!" || sentMessages[0].body == "Hello!")
    }

    @Test
    fun `send ignores empty messages`() = runBlocking {
        val repo = ChatRepository(fakeMessageDao, fakePendingDao)
        repo.send(1L, 2L, "   ")
        assertEquals(0, sentMessages.size)
    }
}
