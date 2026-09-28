package com.matree.app.domain

/**
 * Saved/confirmed profile religion. This is domain data, not an appearance setting.
 * UI appearance code must never infer, mutate, or fabricate this value.
 */
enum class ProfileReligion {
    HINDU,
    MUSLIM,
    CHRISTIAN,
    SIKH,
    BUDDHIST,
    JAIN,
    PARSI,
    OTHER,
    UNSPECIFIED,
}
