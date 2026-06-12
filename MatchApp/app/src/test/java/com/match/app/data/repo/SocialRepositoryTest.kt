package com.match.app.data.repo

import com.match.app.data.local.dao.BlockDao
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.entity.BlockEntity
import com.match.app.data.local.entity.LikeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SocialRepositoryTest {

    private lateinit var repo: SocialRepository

    // ── Fake DAOs ──────────────────────────────────────────────────────────
    private val fakeLikeDao = object : LikeDao {
        val likes = mutableListOf<LikeEntity>()
        val flow = MutableStateFlow(0)
        override suspend fun like(l: LikeEntity): Long { likes.add(l); flow.value++; return l.id }
        override suspend fun unlike(from: Long, to: Long) { likes.removeAll { it.fromUserId == from && it.toUserId == to }; flow.value++ }
        override suspend fun isLiked(from: Long, to: Long) = likes.any { it.fromUserId == from && it.toUserId == to }
        override fun observeIsLiked(from: Long, to: Long): Flow<Boolean> = flow.map { likes.any { it.fromUserId == from && it.toUserId == to } }
        override fun observeMutualLikes(me: Long): Flow<List<LikeEntity>> = flow.map {
            likes.filter { l -> l.fromUserId == me && likes.any { r -> r.fromUserId == l.toUserId && r.toUserId == me } }
        }
        override fun observeLikedByMe_notYet(me: Long): Flow<List<Long>> = flow.map {
            likes.filter { it.toUserId == me && !likes.any { r -> r.fromUserId == me && r.toUserId == it.fromUserId } }.map { it.fromUserId }
        }
        override fun observeIncomingCount(me: Long): Flow<Int> = flow.map {
            likes.count { it.toUserId == me && !likes.any { r -> r.fromUserId == me && r.toUserId == it.fromUserId } }
        }
        override fun observeReceivedInterests(me: Long): Flow<List<Long>> = flow.map {
            likes.filter { it.toUserId == me }.map { it.fromUserId }.distinct()
        }
        override fun observeSentInterests(me: Long): Flow<List<Long>> = flow.map {
            likes.filter { it.fromUserId == me }.map { it.toUserId }.distinct()
        }
    }

    private val fakeBlockDao = object : BlockDao {
        val blocks = mutableListOf<BlockEntity>()
        val flow = MutableStateFlow(0)
        override suspend fun block(b: BlockEntity): Long { blocks.add(b); flow.value++; return b.id }
        override suspend fun unblock(blocker: Long, blocked: Long) { blocks.removeAll { it.blockerId == blocker && it.blockedId == blocked }; flow.value++ }
        override suspend fun isBlocked(blocker: Long, blocked: Long) = blocks.any { it.blockerId == blocker && it.blockedId == blocked }
        override suspend fun blockedIds(me: Long) = blocks.filter { it.blockerId == me }.map { it.blockedId }
        override fun observeBlockedIds(me: Long): Flow<List<Long>> = flow.map { blocks.filter { it.blockerId == me }.map { it.blockedId } }
    }

    @Before fun setup() {
        repo = SocialRepository(fakeLikeDao, fakeBlockDao)
    }

    @Test fun `like and unlike`() = runTest {
        assertFalse(repo.isLiked(1, 2))
        repo.like(1, 2)
        assertTrue(repo.isLiked(1, 2))
        repo.unlike(1, 2)
        assertFalse(repo.isLiked(1, 2))
    }

    @Test fun `toggleLike toggles`() = runTest {
        val first = repo.toggleLike(1, 2)
        assertTrue(first)
        assertTrue(repo.isLiked(1, 2))
        val second = repo.toggleLike(1, 2)
        assertFalse(second)
        assertFalse(repo.isLiked(1, 2))
    }

    @Test fun `block and unblock`() = runTest {
        assertFalse(repo.isBlocked(1, 2))
        repo.block(1, 2)
        assertTrue(repo.isBlocked(1, 2))
        assertEquals(listOf(2L), repo.blockedIds(1))
        repo.unblock(1, 2)
        assertFalse(repo.isBlocked(1, 2))
    }

    @Test fun `block is directional`() = runTest {
        repo.block(1, 2)
        assertTrue(repo.isBlocked(1, 2))
        assertFalse(repo.isBlocked(2, 1))
    }
}
