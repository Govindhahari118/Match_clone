package com.match.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class InterestListPolicyTest {

    @Test
    fun `mutual members are removed from pending lists`() {
        assertEquals(
            listOf("pending-a", "pending-b"),
            InterestListPolicy.pendingCounterparts(
                listOf("pending-a", "mutual", "pending-b"),
                setOf("mutual")
            )
        )
    }

    @Test
    fun `pending lists discard blanks and duplicate counterparts`() {
        assertEquals(
            listOf("alice", "bob"),
            InterestListPolicy.pendingCounterparts(
                listOf("alice", "", "alice", "bob"),
                emptySet()
            )
        )
    }
}
