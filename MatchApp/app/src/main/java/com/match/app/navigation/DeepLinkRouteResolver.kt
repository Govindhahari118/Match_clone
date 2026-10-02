package com.match.app.navigation

/**
 * Pure deep-link route policy. Android URI parsing stays in MainActivity; this object decides
 * whether already-parsed input is allowed to enter the app navigation graph.
 *
 * Repositories/screens remain the final authorization boundary for profile/chat content.
 */
object DeepLinkRouteResolver {
    const val LEGACY_SCHEME = "matrimonyconnect"
    const val APP_LINK_SCHEME = "https"
    private const val APP_LINK_PREFIX = "app"

    fun fromUri(
        scheme: String?,
        userInfo: String?,
        host: String?,
        pathSegments: List<String>,
        hasQueryOrFragment: Boolean,
        appLinkHost: String? = null
    ): String? {
        if (userInfo != null || hasQueryOrFragment) return null

        if (scheme.equals(LEGACY_SCHEME, ignoreCase = true)) {
            return legacyRoute(host, pathSegments)
        }

        if (scheme.equals(APP_LINK_SCHEME, ignoreCase = true)) {
            val expectedHost = appLinkHost?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (!host.equals(expectedHost, ignoreCase = true)) return null
            if (pathSegments.firstOrNull() != APP_LINK_PREFIX) return null
            return appLinkRoute(pathSegments.drop(1))
        }

        return null
    }

    fun notificationAccountMatches(expectedUid: String?, currentUid: String?): Boolean =
        expectedUid.isNullOrBlank() || (!currentUid.isNullOrBlank() && expectedUid == currentUid)

    fun fromNotification(
        type: String?,
        fromUserId: Long?,
        chatPeerId: Long?
    ): String? = when (type) {
        "message" -> positiveId(chatPeerId)?.let { "chat/$it" } ?: "chat_list"
        "interest_received" -> positiveId(fromUserId)?.let { "detail/$it" } ?: "interests"
        "new_match", "mutual_match" -> positiveId(fromUserId)?.let { "detail/$it" } ?: "matches"
        "profile_viewed" -> "who_viewed"
        "profile_incomplete" -> "profile"
        "inactivity_nudge" -> "matches"
        "like" -> "interests"
        "notification", "reward" -> "notifications"
        "boost_expiring", "subscription_expiry" -> "pricing"
        "verification", "verification_update" -> "verification"
        else -> null
    }

    private fun legacyRoute(host: String?, pathSegments: List<String>): String? = when (host?.lowercase()) {
        "match" -> positiveIdRoute(pathSegments, "detail")
        "chat" -> positiveIdRoute(pathSegments, "chat")
        "notifications" -> staticRoute(pathSegments, "notifications")
        "interests" -> staticRoute(pathSegments, "interests")
        "matches" -> staticRoute(pathSegments, "matches")
        "who_viewed" -> staticRoute(pathSegments, "who_viewed")
        "pricing" -> staticRoute(pathSegments, "pricing")
        "verification" -> staticRoute(pathSegments, "verification")
        "nearby" -> staticRoute(pathSegments, "nearby")
        else -> null
    }

    private fun appLinkRoute(pathSegments: List<String>): String? {
        val route = pathSegments.firstOrNull()?.lowercase() ?: return null
        val remainder = pathSegments.drop(1)
        return when (route) {
            "match" -> positiveIdRoute(remainder, "detail")
            "chat" -> positiveIdRoute(remainder, "chat")
            "notifications" -> staticRoute(remainder, "notifications")
            "interests" -> staticRoute(remainder, "interests")
            "matches" -> staticRoute(remainder, "matches")
            "who_viewed" -> staticRoute(remainder, "who_viewed")
            "pricing" -> staticRoute(remainder, "pricing")
            "verification" -> staticRoute(remainder, "verification")
            "nearby" -> staticRoute(remainder, "nearby")
            else -> null
        }
    }

    private fun positiveIdRoute(pathSegments: List<String>, route: String): String? =
        pathSegments.singleOrNull()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { "$route/$it" }

    private fun staticRoute(pathSegments: List<String>, route: String): String? =
        route.takeIf { pathSegments.isEmpty() }

    private fun positiveId(value: Long?): Long? = value?.takeIf { it > 0 }
}
