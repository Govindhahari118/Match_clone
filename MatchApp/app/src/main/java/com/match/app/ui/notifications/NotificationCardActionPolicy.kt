package com.match.app.ui.notifications

enum class NotificationCardDestination {
    INTERESTS,
    CHAT,
    PROFILE,
    NONE
}

fun notificationCardDestination(type: String, hasActor: Boolean): NotificationCardDestination =
    when (type.trim().uppercase()) {
        "LIKE", "INTEREST" -> NotificationCardDestination.INTERESTS
        "MATCH", "MESSAGE", "CALL_REQUEST", "CALL_ACCEPTED", "CALL_DECLINED", "CALL_CANCELLED" ->
            if (hasActor) NotificationCardDestination.CHAT else NotificationCardDestination.NONE
        "VIEW" -> if (hasActor) NotificationCardDestination.PROFILE else NotificationCardDestination.NONE
        else -> NotificationCardDestination.NONE
    }
