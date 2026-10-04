package com.match.app.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCardActionPolicyTest {
    @Test
    fun interestNotificationsOpenActionableInbox() {
        assertEquals(
            NotificationCardDestination.INTERESTS,
            notificationCardDestination("INTEREST", hasActor = true)
        )
        assertEquals(
            NotificationCardDestination.INTERESTS,
            notificationCardDestination("LIKE", hasActor = false)
        )
    }

    @Test
    fun mutualMatchAndMessageOpenChatOnlyWithResolvedPeer() {
        assertEquals(
            NotificationCardDestination.CHAT,
            notificationCardDestination("MATCH", hasActor = true)
        )
        assertEquals(
            NotificationCardDestination.CHAT,
            notificationCardDestination("MESSAGE", hasActor = true)
        )
        assertEquals(
            NotificationCardDestination.NONE,
            notificationCardDestination("MESSAGE", hasActor = false)
        )
    }

    @Test
    fun callCoordinationNotificationsOpenChatOnlyWithResolvedPeer() {
        listOf("CALL_REQUEST", "CALL_ACCEPTED", "CALL_DECLINED", "CALL_CANCELLED").forEach { type ->
            assertEquals(
                NotificationCardDestination.CHAT,
                notificationCardDestination(type, hasActor = true)
            )
            assertEquals(
                NotificationCardDestination.NONE,
                notificationCardDestination(type, hasActor = false)
            )
        }
    }

    @Test
    fun profileViewsOpenProfileOnlyWithResolvedPeer() {
        assertEquals(
            NotificationCardDestination.PROFILE,
            notificationCardDestination("VIEW", hasActor = true)
        )
        assertEquals(
            NotificationCardDestination.NONE,
            notificationCardDestination("VIEW", hasActor = false)
        )
    }
}
