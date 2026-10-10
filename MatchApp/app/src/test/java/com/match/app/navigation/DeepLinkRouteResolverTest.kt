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
            "messages" to "chat_list",
            "who_viewed" to "who_viewed",
            "pricing" to "pricing",
            "verification" to "verification",
            "nearby" to "nearby"
        )
        routes.forEach { (host, route) ->
            assertEquals(
                route,
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.LEGACY_SCHEME,
                    null,
                    host,
                    emptyList(),
                    false
                )
            )
            assertNull(
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.LEGACY_SCHEME,
                    null,
                    host,
                    listOf("unexpected"),
                    false
                )
            )
            assertNull(
                DeepLinkRouteResolver.fromUri(
                    DeepLinkRouteResolver.LEGACY_SCHEME,
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
    fun `verified https app links require configured host and app path prefix`() {
        assertEquals(
            "detail/42",
            DeepLinkRouteResolver.fromUri("https", null, "links.matree.example", listOf("app", "match", "42"), false, "links.matree.example")
        )
        assertEquals(
            "matches",
            DeepLinkRouteResolver.fromUri("HTTPS", null, "LINKS.MATREE.EXAMPLE", listOf("app", "matches"), false, "links.matree.example")
        )
        assertEquals(
            "chat_list",
            DeepLinkRouteResolver.fromUri("https", null, "links.matree.example", listOf("app", "messages"), false, "links.matree.example")
        )
        assertNull(DeepLinkRouteResolver.fromUri("https", null, "evil.example", listOf("app", "matches"), false, "links.matree.example"))
        assertNull(DeepLinkRouteResolver.fromUri("https", null, "links.matree.example", listOf("matches"), false, "links.matree.example"))
        assertNull(DeepLinkRouteResolver.fromUri("https", null, "links.matree.example", listOf("app", "match", "42"), true, "links.matree.example"))
        assertNull(DeepLinkRouteResolver.fromUri("https", null, "links.matree.example", listOf("app", "matches"), false, null))
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
    fun `notification route consumption is bound to intended account`() {
        assertEquals(
            true,
            DeepLinkRouteResolver.notificationAccountMatches("uid-a", "uid-a")
        )
        assertEquals(
            false,
            DeepLinkRouteResolver.notificationAccountMatches("uid-a", "uid-b")
        )
        assertEquals(
            false,
            DeepLinkRouteResolver.notificationAccountMatches("uid-a", null)
        )
        assertEquals(
            true,
            DeepLinkRouteResolver.notificationAccountMatches(null, "uid-b")
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
        assertEquals("chat/12", DeepLinkRouteResolver.fromNotification("call_request", 12, null))
        assertEquals("chat/12", DeepLinkRouteResolver.fromNotification("call_accepted", 12, null))
        assertEquals("chat_list", DeepLinkRouteResolver.fromNotification("call_declined", null, null))
        assertNull(DeepLinkRouteResolver.fromNotification("unknown", null, null))
    }
}
