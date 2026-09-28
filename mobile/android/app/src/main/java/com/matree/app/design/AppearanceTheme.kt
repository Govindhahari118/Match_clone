package com.matree.app.design

enum class AppearanceTheme(
    val displayName: String,
) {
    UNIVERSAL("Default"),
    HINDU("Hindu"),
    MUSLIM("Muslim"),
    CHRISTIAN("Christian"),
    SIKH("Sikh"),
    BUDDHIST("Buddhist"),
    JAIN("Jain"),
    PARSI("Parsi"),
    OTHER("Other"),
}

enum class ReferenceStatus {
    APPROVED_DIRECTION,
    REFERENCE_REQUIRED,
}
