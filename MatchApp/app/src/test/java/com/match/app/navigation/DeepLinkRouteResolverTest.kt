package com.match.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkRouteResolverTest {

    @Test
    fun `static routes accept only empty paths without query or fragment`() {
        val routes = mapOf(
            "notifications" to "notifications",
            "interests" to "interests",
            "matches" to "matches",
            "who_viewed" to "who_viewed",
            "pricing" to "pricing",
            "verification" to "verification",
            "nearby" to "nearby"
        )
        routes.forEach { (host, route) ->
            assertEquals(
                route,
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.SCHEME,
                    null,
                    host,
                    emptyList(),
                    false
                )
            )
            assertNull(
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.SCHEME,
                    null,
                    host,
                    listOf("unexpected"),
                    false
                )
            )
            assertNull(
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.SCHEME,
                    null,
                    host,
                    emptyList(),
                    true
                )
            )
        }
    }

    @Test
    fun `profile and chat links require exactly one positive numeric id`() {
        assertEquals(
            "detail/42",
            DeepLinkRouteResolver.fromUri("matrimonyconnect", null, "match", listOf("42"), false)
        )
        assertEquals(
            "chat/7",
            DeepLinkRouteResolver.fromUri("MATRIMONYCONNECT", null, "chat", listOf("7"), false)
        )
        listOf("0", "-1", "abc", "", "9223372036854775808").forEach { id ->
            assertNull(
                DeepLinkRouteResolver.fromUri("matrimonyconnect", null, "match", listOf(id), false)
            )
        }
        assertNull(
            DeepLinkRouteResolver.fromUri("matrimonyconnect", null, "chat", listOf("1", "2"), false)
        )
        assertNull(
            DeepLinkRouteResolver.fromUri("matrimonyconnect", null, "chat", listOf("1"), true)
        )
    }

    @Test
    fun `unsupported schemes hosts and userinfo are rejected`() {
        assertNull(
            DeepLinkRouteResolver.fromUri("https", null, "matches", emptyList(), false)
        )
        assertNull(
            DeepLinkRouteResolver.fromUri("matrimonyconnect", "attacker", "matches", emptyList(), false)
        )
        assertNull(
            DeepLinkRouteResolver.fromUri("matrimonyconnect", null, "unknown", emptyList(), false)
        )
        assertNull(
            DeepLinkRouteResolver.fromUri(null, null, "matches", emptyList(), false)
        )
    }

    @Test
    fun `notification routes use validated ids and truthful fallbacks`() {
        assertEquals("chat/9", DeepLinkRouteResolver.fromNotification("message", null, 9))
        assertEquals("chat_list", DeepLinkRouteResolver.fromNotification("message", null, -1))
        assertEquals("detail/4", DeepLinkRouteResolver.fromNotification("interest_received", 4, null))
        assertEquals("interests", DeepLinkRouteResolver.fromNotification("interest_received", 0, null))
        assertEquals("detail/5", DeepLinkRouteResolver.fromNotification("mutual_match", 5, null))
        assertEquals("matches", DeepLinkRouteResolver.fromNotification("new_match", null, null))
        assertEquals("who_viewed", DeepLinkRouteResolver.fromNotification("profile_viewed", null, null))
        assertEquals("profile", DeepLinkRouteResolver.fromNotification("profile_incomplete", null, null))
        assertEquals("pricing", DeepLinkRouteResolver.fromNotification("subscription_expiry", null, null))
        assertEquals("verification", DeepLinkRouteResolver.fromNotification("verification_update", null, null))
        assertNull(DeepLinkRouteResolver.fromNotification("unknown", null, null))
    }
}
