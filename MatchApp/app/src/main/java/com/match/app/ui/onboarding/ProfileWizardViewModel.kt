package com.match.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.repo.ReligionProfileRepository
import com.match.app.data.repo.UsernameRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.ReligionId
import com.match.app.domain.profile.IndiaProfileCatalog
import com.match.app.domain.profile.ReligionFieldKey
import com.match.app.domain.profile.ReligionFieldRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class WizardState(
    val username: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",
    val state: String = "",
    val city: String = "",
    val motherTongue: String = "",
    val bio: String = "",
    val religion: String = "",
    val caste: String = "",
    val subCaste: String = "",
    val gothra: String = "",
    val education: String = "",
    val educationField: String = "",
    val institution: String = "",
    val graduationYear: Int = 0,
    val profession: String = "",
    val occupationCategory: String = "",
    val employer: String = "",
    val employerType: String = "",
    val incomeBand: String = "",
    val heightCm: Int = 0,
    val weight: Float = 0f,
    val complexion: String = "",
    val physicalStatus: String = "",
    val maritalStatus: String = "",
    val hasChildren: Boolean = false,
    val nativeState: String = "",
    val familyType: String = "",
    val familyStatus: String = "",
    val fatherOccupation: String = "",
    val motherOccupation: String = "",
    val siblings: Int = 0,
    val familyValues: String = "",
    val aboutFamily: String = "",
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    val hobbies: String = "",
    val spokenLanguages: String = "",
    val personalityType: String = "",
    val fitnessActivities: String = "",
    val countryOfResidence: String = "",
    val citizenship: String = "",
    val residentialStatus: String = "",
    val visaStatus: String = "",
    val willingToRelocate: Boolean = false,
    val rasi: String = "",
    val nakshatra: String = "",
    val manglik: String = "",
    val birthTime: String = "",
    val birthPlace: String = ""
)

@HiltViewModel
class ProfileWizardViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val firestoreProfile: FirestoreProfileService,
    private val usernameRepository: UsernameRepository,
    private val religionProfileRepository: ReligionProfileRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()
    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState = _wizardState.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()
    private val _religionLocked = MutableStateFlow(false)
    val religionLocked = _religionLocked.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            _wizardState.value = user.toWizardState()
            if (user.firebaseUid.isNotBlank()) {
                religionProfileRepository.observeConfirmed(user.firebaseUid)
                    .catch { emit(false) }
                    .collect { _religionLocked.value = it }
            }
        }
    }

    fun update(state: WizardState) {
        _wizardState.value = sanitizeForReligion(state)
        _saveError.value = null
    }

    fun nextStep() {
        validateStep(_currentStep.value)?.let { _saveError.value = it; return }
        persistDraft()
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(LAST_STEP)
    }

    fun previousStep() {
        persistDraft()
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

    fun skipStep() {
        val step = _currentStep.value
        if (step in REQUIRED_STEPS) {
            _saveError.value = when (step) {
                0 -> "Please complete your identity, state, city and mother tongue first."
                1 -> "Please choose your religion before continuing. You can choose Prefer not to say if you do not want to specify one."
                2 -> "Please add your highest education and occupation before continuing."
                3 -> "Please enter a valid height and select your marital status before continuing."
                else -> "Complete the required fields before continuing."
            }
            return
        }
        persistDraft()
        _currentStep.value = (step + 1).coerceAtMost(LAST_STEP)
    }

    fun clearError() { _saveError.value = null }

    fun finish(onComplete: () -> Unit) = viewModelScope.launch {
        validateCompleteProfile()?.let { _saveError.value = it; return@launch }
        if (_saving.value) return@launch
        _saving.value = true
        _saveError.value = null

        try {
            val localId = session.userId.first() ?: error("You are not signed in.")
            val current = userDao.findById(localId) ?: error("Your local profile could not be loaded.")
            val selectedReligion = ReligionId.fromProfileValue(_wizardState.value.religion)
                ?: error("Choose your religion before completing your profile.")
            val state = sanitizeForReligion(_wizardState.value.copy(religion = selectedReligion.label))
            val desiredUsername = usernameRepository.normalize(state.username)
            val username = if (!current.username.equals(desiredUsername, ignoreCase = true)) {
                usernameRepository.reserve(desiredUsername).getOrThrow()
            } else desiredUsername
            val updated = current.applyWizard(state, username)
                .copy(profileCompleteness = calculateCompleteness(state))

            if (updated.firebaseUid.isNotBlank()) {
                religionProfileRepository.confirm(selectedReligion.label)
                firestoreProfile.pushProfile(updated)
            }
            userDao.update(updated)

            val currentFilter = session.filter.first()
            session.setFilter(
                currentFilter.copy(
                    state = state.state,
                    motherTongue = state.motherTongue,
                    religion = selectedReligion.label
                )
            )
            session.setCommunitySetupDone(true)
            onComplete()
        } catch (e: Exception) {
            _saveError.value = e.message?.take(180) ?: "Could not save your profile. Please try again."
        } finally {
            _saving.value = false
        }
    }

    private fun persistDraft() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val user = userDao.findById(uid) ?: return@launch
        userDao.update(user.applyWizard(sanitizeForReligion(_wizardState.value), user.username))
    }

    private fun validateStep(step: Int): String? {
        val s = _wizardState.value
        return when (step) {
            0 -> validateRequiredIdentityAndLocation()
            1 -> if (s.religion !in IndiaProfileCatalog.religions) "Select your religion or choose Prefer not to say." else null
            2 -> if (s.education.isBlank() || s.profession.isBlank()) "Add your education and occupation." else null
            3 -> when {
                s.heightCm !in 90..250 -> "Enter a valid height in centimetres."
                s.maritalStatus !in IndiaProfileCatalog.maritalStatuses -> "Select your marital status."
                else -> null
            }
            else -> null
        }
    }

    private fun validateRequiredIdentityAndLocation(): String? {
        val s = _wizardState.value
        if (s.displayName.trim().length < 2) return "Enter your full name."
        usernameRepository.validate(s.username)?.let { return it }
        val dob = runCatching { LocalDate.parse(s.dateOfBirth.trim()) }.getOrNull()
            ?: return "Enter date of birth as YYYY-MM-DD."
        if (dob.isAfter(LocalDate.now())) return "Date of birth cannot be in the future."
        val age = Period.between(dob, LocalDate.now()).years
        if (age !in 18..99) return "This matrimony service is available only to adults aged 18 or above."
        if (s.state.isBlank()) return "Select your state or union territory."
        if (s.city.trim().length < 2) return "Enter your city."
        if (s.motherTongue.isBlank()) return "Select your mother tongue."
        return null
    }

    private fun validateCompleteProfile(): String? {
        validateRequiredIdentityAndLocation()?.let { return it }
        val s = _wizardState.value
        if (s.religion !in IndiaProfileCatalog.religions) return "Select your religion or choose Prefer not to say."
        if (s.education.isBlank()) return "Add your highest education."
        if (s.profession.trim().length < 2) return "Add your occupation or profession."
        if (s.heightCm !in 90..250) return "Enter a valid height in centimetres."
        if (s.maritalStatus !in IndiaProfileCatalog.maritalStatuses) return "Select your marital status."
        return null
    }

    private fun UserEntity.toWizardState() = WizardState(
        username = username,
        displayName = displayName,
        dateOfBirth = dateOfBirth,
        state = state,
        city = city,
        motherTongue = motherTongue,
        bio = bio,
        religion = religion,
        caste = caste,
        subCaste = subCaste,
        gothra = gothra,
        education = education,
        educationField = educationField,
        institution = institution,
        graduationYear = graduationYear,
        profession = profession,
        occupationCategory = occupationCategory,
        employer = employer,
        employerType = employerType,
        incomeBand = incomeBand,
        heightCm = heightCm,
        weight = weight,
        complexion = complexion,
        physicalStatus = physicalStatus,
        maritalStatus = maritalStatus,
        hasChildren = hasChildren,
        nativeState = nativeState,
        familyType = familyType,
        familyStatus = familyStatus,
        fatherOccupation = fatherOccupation,
        motherOccupation = motherOccupation,
        siblings = siblings,
        familyValues = familyValues,
        aboutFamily = aboutFamily,
        diet = diet,
        smoking = smoking,
        drinking = drinking,
        hobbies = hobbies,
        spokenLanguages = spokenLanguages,
        personalityType = personalityType,
        fitnessActivities = fitnessActivities,
        countryOfResidence = countryOfResidence,
        citizenship = citizenship,
        residentialStatus = residentialStatus,
        visaStatus = visaStatus,
        willingToRelocate = willingToRelocate,
        rasi = rasi,
        nakshatra = nakshatra,
        manglik = manglik,
        birthTime = birthTime,
        birthPlace = birthPlace
    )

    private fun UserEntity.applyWizard(s: WizardState, reservedUsername: String): UserEntity {
        val clean = sanitizeForReligion(s)
        val ageFromDob = runCatching {
            Period.between(LocalDate.parse(clean.dateOfBirth.trim()), LocalDate.now()).years
        }.getOrNull()
        val isNri = clean.countryOfResidence.isNotBlank() && !clean.countryOfResidence.equals("India", ignoreCase = true)
        return copy(
            username = reservedUsername,
            displayName = clean.displayName.trim(),
            dateOfBirth = clean.dateOfBirth.trim(),
            age = ageFromDob?.takeIf { it in 18..99 } ?: age,
            state = clean.state.trim(),
            city = clean.city.trim(),
            motherTongue = clean.motherTongue.trim(),
            bio = clean.bio.trim().take(1000),
            religion = clean.religion.trim(),
            caste = clean.caste.trim(),
            subCaste = clean.subCaste.trim(),
            gothra = clean.gothra.trim(),
            education = clean.education.trim(),
            educationField = clean.educationField.trim(),
            institution = clean.institution.trim(),
            graduationYear = clean.graduationYear,
            profession = clean.profession.trim(),
            occupationCategory = clean.occupationCategory.trim(),
            employer = clean.employer.trim(),
            employerType = clean.employerType.trim(),
            incomeBand = clean.incomeBand.trim(),
            heightCm = clean.heightCm,
            weight = clean.weight,
            complexion = clean.complexion.trim(),
            physicalStatus = clean.physicalStatus.trim(),
            maritalStatus = clean.maritalStatus.trim(),
            hasChildren = clean.hasChildren,
            nativeState = clean.nativeState.trim(),
            familyType = clean.familyType.trim(),
            familyStatus = clean.familyStatus.trim(),
            fatherOccupation = clean.fatherOccupation.trim(),
            motherOccupation = clean.motherOccupation.trim(),
            siblings = clean.siblings.coerceAtLeast(0),
            familyValues = clean.familyValues.trim(),
            aboutFamily = clean.aboutFamily.trim().take(1000),
            diet = clean.diet.trim(),
            smoking = clean.smoking.trim(),
            drinking = clean.drinking.trim(),
            hobbies = clean.hobbies.trim(),
            spokenLanguages = clean.spokenLanguages.trim(),
            personalityType = clean.personalityType.trim(),
            fitnessActivities = clean.fitnessActivities.trim(),
            countryOfResidence = clean.countryOfResidence.trim(),
            citizenship = clean.citizenship.trim(),
            residentialStatus = clean.residentialStatus.trim(),
            visaStatus = clean.visaStatus.trim(),
            willingToRelocate = clean.willingToRelocate,
            isNRI = isNri,
            rasi = clean.rasi.trim(),
            nakshatra = clean.nakshatra.trim(),
            manglik = clean.manglik.trim(),
            birthTime = clean.birthTime.trim(),
            birthPlace = clean.birthPlace.trim()
        )
    }

    private fun sanitizeForReligion(s: WizardState): WizardState {
        val religion = ReligionId.fromProfileValue(s.religion) ?: return s.copy(
            caste = "",
            subCaste = "",
            gothra = "",
            rasi = "",
            nakshatra = "",
            manglik = "",
            birthTime = "",
            birthPlace = ""
        )
        val schema = ReligionFieldRegistry.schemaFor(religion)
        return s.copy(
            religion = religion.label,
            caste = if (schema.supports(ReligionFieldKey.COMMUNITY)) s.caste else "",
            subCaste = if (schema.supports(ReligionFieldKey.SUB_COMMUNITY)) s.subCaste else "",
            gothra = if (schema.supports(ReligionFieldKey.GOTHRA)) s.gothra else "",
            rasi = if (schema.supports(ReligionFieldKey.RASHI)) s.rasi else "",
            nakshatra = if (schema.supports(ReligionFieldKey.NAKSHATRA)) s.nakshatra else "",
            manglik = if (schema.supports(ReligionFieldKey.MANGLIK)) s.manglik else "",
            birthTime = if (schema.supports(ReligionFieldKey.BIRTH_TIME)) s.birthTime else "",
            birthPlace = if (schema.supports(ReligionFieldKey.BIRTH_PLACE)) s.birthPlace else ""
        )
    }

    private fun calculateCompleteness(s: WizardState): Float {
        val checks = listOf(
            s.username.isNotBlank(),
            s.displayName.isNotBlank(),
            s.dateOfBirth.isNotBlank(),
            s.state.isNotBlank(),
            s.city.isNotBlank(),
            s.motherTongue.isNotBlank(),
            s.bio.isNotBlank(),
            s.religion.isNotBlank(),
            s.education.isNotBlank(),
            s.profession.isNotBlank(),
            s.heightCm > 0,
            s.maritalStatus.isNotBlank(),
            s.familyType.isNotBlank(),
            s.familyValues.isNotBlank(),
            s.diet.isNotBlank(),
            s.countryOfResidence.isNotBlank(),
            s.incomeBand.isNotBlank(),
            s.employer.isNotBlank(),
            s.aboutFamily.isNotBlank(),
            s.spokenLanguages.isNotBlank()
        )
        return checks.count { it }.toFloat() / checks.size.toFloat()
    }

    private companion object {
        const val LAST_STEP = 7
        val REQUIRED_STEPS = setOf(0, 1, 2, 3)
    }
}
