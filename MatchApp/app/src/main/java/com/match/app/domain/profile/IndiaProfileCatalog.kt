package com.match.app.domain.profile

import com.match.app.domain.model.ReligionId

/**
 * Shared catalog for the one-app, all-India onboarding and discovery experience.
 *
 * State selection is used only to improve defaults and usability. Religion/community are always
 * explicitly selected by the member; the app never infers caste or religion from geography,
 * language, name or any other attribute.
 */
object IndiaProfileCatalog {
    val statesAndUnionTerritories = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa",
        "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
        "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
        "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
        "Uttar Pradesh", "Uttarakhand", "West Bengal",
        "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    )

    val indianLanguages = listOf(
        "Assamese", "Bengali", "Bodo", "Dogri", "Gujarati", "Hindi", "Kannada", "Kashmiri",
        "Konkani", "Maithili", "Malayalam", "Manipuri", "Marathi", "Nepali", "Odia", "Punjabi",
        "Sanskrit", "Santali", "Sindhi", "Tamil", "Telugu", "Urdu", "English", "Other"
    )

    /** Display labels only. Persist [ReligionId.storageKey] in new canonical identity data. */
    val religions = ReligionId.entries.map { it.label }

    val maritalStatuses = listOf(
        "Never Married", "Divorced", "Widowed", "Awaiting Divorce", "Annulled"
    )
    val educationLevels = listOf(
        "High School", "Diploma", "Bachelor's", "Graduate", "Post Graduate", "Master's",
        "Professional Degree", "Doctorate", "Other"
    )
    val occupationCategories = listOf(
        "Private Sector", "Government", "Public Sector / PSU", "Business / Self-Employed",
        "Professional", "Defence", "Healthcare", "Education", "Agriculture", "Student",
        "Homemaker", "Not Working", "Retired", "Other"
    )
    val employerTypes = listOf("MNC", "Startup", "Private Company", "Government", "PSU", "Self-Employed", "Other")
    val familyTypes = listOf("Nuclear", "Joint", "Extended", "Other")
    val familyStatuses = listOf("Middle Class", "Upper Middle Class", "Affluent", "Other")
    val familyValues = listOf("Traditional", "Moderate", "Liberal", "Other")
    val diets = listOf("Vegetarian", "Non-Vegetarian", "Eggetarian", "Vegan", "Jain", "Other")
    val habitOptions = listOf("Never", "Occasionally", "Socially", "Regularly", "Prefer not to say")
    val physicalStatuses = listOf("No disability", "Person with disability", "Prefer not to say")
    val residentialStatuses = listOf("Citizen", "Permanent Resident", "Work Permit", "Student Visa", "Dependent Visa", "Other")
    val countriesCommon = listOf(
        "India", "United States", "United Kingdom", "United Arab Emirates", "Canada", "Australia",
        "Singapore", "Germany", "New Zealand", "Saudi Arabia", "Qatar", "Kuwait", "Oman", "Other"
    )

    /** Language shortcuts only; these are suggestions, never automatic identity inference. */
    fun languageSuggestionsForState(state: String): List<String> = when (state) {
        "Andhra Pradesh", "Telangana" -> listOf("Telugu", "Urdu", "Hindi", "English")
        "Karnataka" -> listOf("Kannada", "Tulu", "Konkani", "Urdu", "Telugu", "English")
        "Tamil Nadu", "Puducherry" -> listOf("Tamil", "Telugu", "Malayalam", "English")
        "Kerala", "Lakshadweep" -> listOf("Malayalam", "Tamil", "English")
        "Maharashtra" -> listOf("Marathi", "Hindi", "Gujarati", "Konkani", "English")
        "Odisha" -> listOf("Odia", "Hindi", "Bengali", "English")
        "Punjab", "Chandigarh" -> listOf("Punjabi", "Hindi", "English")
        "Gujarat", "Dadra and Nagar Haveli and Daman and Diu" -> listOf("Gujarati", "Hindi", "English")
        "West Bengal" -> listOf("Bengali", "Hindi", "Nepali", "English")
        "Assam" -> listOf("Assamese", "Bengali", "Bodo", "Hindi", "English")
        "Bihar", "Jharkhand" -> listOf("Hindi", "Maithili", "Bengali", "Urdu", "English")
        "Uttar Pradesh", "Uttarakhand" -> listOf("Hindi", "Urdu", "English")
        "Rajasthan" -> listOf("Hindi", "Marwari", "English")
        "Goa" -> listOf("Konkani", "Marathi", "English")
        "Jammu and Kashmir" -> listOf("Kashmiri", "Dogri", "Urdu", "Hindi", "English")
        "Ladakh" -> listOf("Ladakhi", "Hindi", "English")
        "Manipur" -> listOf("Manipuri", "English", "Hindi")
        "Meghalaya" -> listOf("Khasi", "Garo", "English", "Hindi")
        "Mizoram" -> listOf("Mizo", "English", "Hindi")
        "Nagaland" -> listOf("English", "Nagamese", "Hindi")
        "Tripura" -> listOf("Bengali", "Kokborok", "English", "Hindi")
        "Sikkim" -> listOf("Nepali", "English", "Hindi")
        else -> listOf("Hindi", "English")
    }.distinct()

    /** Optional editable community shortcuts. Members can always type a community not listed. */
    fun communitySuggestions(religion: String): List<String> = when (ReligionId.fromProfileValue(religion)) {
        ReligionId.HINDU -> listOf(
            "Brahmin", "Reddy", "Kamma", "Kapu", "Naidu", "Velama", "Lingayat", "Vokkaliga",
            "Iyer", "Iyengar", "Nair", "Pillai", "Mudaliar", "Gounder", "Maratha", "Rajput",
            "Jat", "Yadav", "Bania / Vaishya", "Kayastha", "Other"
        )
        ReligionId.MUSLIM -> listOf("Sunni", "Shia", "Other / Prefer self-description")
        ReligionId.CHRISTIAN -> listOf("Catholic", "Orthodox", "Protestant", "Pentecostal", "Other")
        ReligionId.SIKH -> listOf("Jat Sikh", "Khatri", "Arora", "Ramgarhia", "Other")
        ReligionId.JAIN -> listOf("Digambar", "Shwetambar", "Other")
        ReligionId.BUDDHIST -> listOf("Buddhist", "Other")
        ReligionId.PARSI_ZOROASTRIAN -> listOf("Parsi / Zoroastrian", "Other")
        ReligionId.OTHER -> listOf("Other")
        ReligionId.PREFER_NOT_TO_SAY, null -> emptyList()
    }
}
