package com.match.app.ui.notifications

enum class NotificationCardDestination {
    INTERESTS,
    CHAT,
    PROFILE,
    OWN_PROFILE,
    NONE
}

fun notificationCardDestination(type: String, hasActor: Boolean): NotificationCardDestination =
    when (type.trim().uppercase()) {
        "LIKE", "INTEREST" -> NotificationCardDestination.INTERESTS
        "MATCH", "MESSAGE" -> if (hasActor) NotificationCardDestination.CHAT else NotificationCardDestination.NONE
        "VIEW" -> if (hasActor) NotificationCardDestination.PROFILE else NotificationCardDestination.NONE
        "PHOTO_REQUEST" -> NotificationCardDestination.OWN_PROFILE
        else -> NotificationCardDestination.NONE
    }
