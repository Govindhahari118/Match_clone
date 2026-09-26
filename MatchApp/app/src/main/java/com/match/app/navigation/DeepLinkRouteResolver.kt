package com.match.app.navigation

/**
 * Pure deep-link route policy. Android URI parsing stays in MainActivity; this object decides
 * whether already-parsed input is allowed to enter the app navigation graph.
 *
 * Repositories/screens remain the final authorization boundary for profile/chat content.
 */
object DeepLinkRouteResolver {
    const val SCHEME = "matrimonyconnect"

    fun fromUri(
        scheme: String?,
        userInfo: String?,
        host: String?,
        pathSegments: List<String>,
        hasQueryOrFragment: Boolean
    ): String? {
        if (!scheme.equals(SCHEME, ignoreCase = true) || userInfo != null) return null

        return when (host?.lowercase()) {
            "match" -> positiveIdRoute(pathSegments, hasQueryOrFragment, "detail")
            "chat" -> positiveIdRoute(pathSegments, hasQueryOrFragment, "chat")
            "notifications" -> staticRoute(pathSegments, hasQueryOrFragment, "notifications")
            "interests" -> staticRoute(pathSegments, hasQueryOrFragment, "interests")
            "matches" -> staticRoute(pathSegments, hasQueryOrFragment, "matches")
            "who_viewed" -> staticRoute(pathSegments, hasQueryOrFragment, "who_viewed")
            "pricing" -> staticRoute(pathSegments, hasQueryOrFragment, "pricing")
            "verification" -> staticRoute(pathSegments, hasQueryOrFragment, "verification")
            "nearby" -> staticRoute(pathSegments, hasQueryOrFragment, "nearby")
            else -> null
        }
    }

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

    private fun positiveIdRoute(
        pathSegments: List<String>,
        hasQueryOrFragment: Boolean,
        route: String
    ): String? {
        if (hasQueryOrFragment || pathSegments.size != 1) return null
        return pathSegments.single().toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { "$route/$it" }
    }

    private fun staticRoute(
        pathSegments: List<String>,
        hasQueryOrFragment: Boolean,
        route: String
    ): String? = route.takeIf { pathSegments.isEmpty() && !hasQueryOrFragment }

    private fun positiveId(value: Long?): Long? = value?.takeIf { it > 0 }
}
