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
