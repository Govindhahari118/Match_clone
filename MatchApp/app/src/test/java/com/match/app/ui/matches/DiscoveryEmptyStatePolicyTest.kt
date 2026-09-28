package com.match.app.ui.matches

import com.match.app.domain.model.MatchFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryEmptyStatePolicyTest {

    @Test
    fun `keyword and location filters produce a contextual reset action`() {
        val state = DiscoveryEmptyStatePolicy.resolve(
            MatchFilter(keyword = "doctor", state = "Telangana")
        )

        assertEquals(DiscoveryEmptyState.Action.RESET, state.action)
        assertEquals("Reset search settings", state.actionLabel)
        assert(state.message.contains("keyword"))
        assert(state.message.contains("location"))
    }

    @Test
    fun `default photo requirement offers a broader photo-free search`() {
        val state = DiscoveryEmptyStatePolicy.resolve(MatchFilter())

        assertEquals(DiscoveryEmptyState.Action.INCLUDE_NO_PHOTO, state.action)
        assertEquals("Include profiles without photos", state.actionLabel)
    }

    @Test
    fun `fully widened search does not invent a filter cause`() {
        val state = DiscoveryEmptyStatePolicy.resolve(
            MatchFilter(withPhotoOnly = false)
        )

        assertEquals(DiscoveryEmptyState.Action.NONE, state.action)
        assertNull(state.actionLabel)
        assert(state.message.contains("eligible profiles"))
    }
}
