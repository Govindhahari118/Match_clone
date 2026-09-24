package com.match.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.repo.ReligionProfileRepository
import com.match.app.data.repo.UsernameRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.profile.ReligionProfileSchemas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val faithTradition: String = "",
    val faithSubTradition: String = "",
    val faithInstitution: String = "",
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
    val countryOfResidence: String = "India",
    val citizenship: String = "Indian",
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
    private val _religionConfirmed = MutableStateFlow(false)
    val religionConfirmed = _religionConfirmed.asStateFlow()
    private val _religionConfirmationRequired = MutableStateFlow(false)
    val religionConfirmationRequired = _religionConfirmationRequired.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            _wizardState.value = user.toWizardState()
        }
    }

    fun update(state: WizardState) {
        val previous = _wizardState.value
        val religionChanged = !state.religion.equals(previous.religion, ignoreCase = true)
        if (religionChanged) {
            _religionConfirmed.value = false
            _religionConfirmationRequired.value = false
            _wizardState.value = state.copy(
                caste = "",
                subCaste = "",
                gothra = "",
                faithTradition = "",
                faithSubTradition = "",
                faithInstitution = "",
                rasi = "",
                nakshatra = "",
                manglik = ""
            )
        } else {
            _wizardState.value = state
        }
        _saveError.value = null
    }

    fun nextStep() {
        val step = _currentStep.value
        validateStep(step)?.let { _saveError.value = it; return }
        if (step == RELIGION_STEP && !_religionConfirmed.value) {
            _religionConfirmationRequired.value = true
            return
        }
        persistDraft()
        _currentStep.value = (step + 1).coerceAtMost(LAST_STEP)
    }

    fun confirmReligionAndContinue() {
        validateStep(RELIGION_STEP)?.let {
            _saveError.value = it
            _religionConfirmationRequired.value = false
            return
        }
        _religionConfirmed.value = true
        _religionConfirmationRequired.value = false
        persistDraft()
        _currentStep.value = (RELIGION_STEP + 1).coerceAtMost(LAST_STEP)
    }

    fun cancelReligionConfirmation() { _religionConfirmationRequired.value = false }

    fun previousStep() {
        persistDraft()
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

    fun skipStep() {
        val step = _currentStep.value
        if (step in REQUIRED_STEPS) {
            _saveError.value = when (step) {
                0 -> "Please complete your identity, state, city and mother tongue first."
                1 -> "Please choose and confirm your religion before continuing. You can choose Prefer not to say if you do not want to specify one."
                2 -> "Please add your highest education and occupation before continuing."
                3 -> "Please enter a valid height and marital status before continuing."
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
        if (!_religionConfirmed.value) {
            _saveError.value = "Review and confirm your religion before completing your profile."
            _currentStep.value = RELIGION_STEP
            return@launch
        }
        if (_saving.value) return@launch
        _saving.value = true
        _saveError.value = null

        try {
            val localId = session.userId.first() ?: error("You are not signed in.")
            val current = userDao.findById(localId) ?: error("Your local profile could not be loaded.")
            val state = _wizardState.value
            val desiredUsername = usernameRepository.normalize(state.username)
            val username = if (!current.username.equals(desiredUsername, ignoreCase = true)) {
                usernameRepository.reserve(desiredUsername).getOrThrow()
            } else desiredUsername
            val updated = current.applyWizard(state, username)
                .copy(profileCompleteness = calculateCompleteness(state))

            if (updated.firebaseUid.isNotBlank()) {
                religionProfileRepository.confirm(state.religion)
                firestoreProfile.pushProfile(updated)
            }
            userDao.update(updated)
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
        val draft = user.applyWizard(_wizardState.value, user.username)
        val safeDraft = if (_religionConfirmed.value) {
            draft
        } else {
            // A temporary picker selection is not canonical religion data until explicit confirmation.
            draft.copy(
                religion = user.religion,
                caste = user.caste,
                subCaste = user.subCaste,
                gothra = user.gothra,
                faithTradition = user.faithTradition,
                faithSubTradition = user.faithSubTradition,
                faithInstitution = user.faithInstitution,
                rasi = user.rasi,
                nakshatra = user.nakshatra,
                manglik = user.manglik
            )
        }
        userDao.update(safeDraft)
    }

    private fun validateStep(step: Int): String? {
        val s = _wizardState.value
        return when (step) {
            0 -> validateRequiredIdentityAndLocation()
            1 -> if (s.religion.isBlank()) "Select your religion or choose Prefer not to say." else null
            2 -> if (s.education.isBlank() || s.profession.isBlank()) "Add your education and occupation." else null
            3 -> when {
                s.heightCm !in 90..250 -> "Enter a valid height in centimetres."
                s.maritalStatus.isBlank() -> "Select your marital status."
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
        if (s.religion.isBlank()) return "Select your religion or choose Prefer not to say."
        if (s.education.isBlank()) return "Add your highest education."
        if (s.profession.trim().length < 2) return "Add your occupation or profession."
        if (s.heightCm !in 90..250) return "Enter a valid height in centimetres."
        if (s.maritalStatus.isBlank()) return "Select your marital status."
        return null
    }

    private fun UserEntity.toWizardState() = WizardState(
        username = username, displayName = displayName, dateOfBirth = dateOfBirth,
        state = state, city = city, motherTongue = motherTongue, bio = bio,
        religion = religion, caste = caste, subCaste = subCaste, gothra = gothra,
        faithTradition = faithTradition, faithSubTradition = faithSubTradition, faithInstitution = faithInstitution,
        education = education, educationField = educationField, institution = institution,
        graduationYear = graduationYear, profession = profession, occupationCategory = occupationCategory,
        employer = employer, employerType = employerType, incomeBand = incomeBand,
        heightCm = heightCm, weight = weight, complexion = complexion, physicalStatus = physicalStatus,
        maritalStatus = maritalStatus, hasChildren = hasChildren, nativeState = nativeState,
        familyType = familyType, familyStatus = familyStatus, fatherOccupation = fatherOccupation,
        motherOccupation = motherOccupation, siblings = siblings, familyValues = familyValues,
        aboutFamily = aboutFamily, diet = diet, smoking = smoking, drinking = drinking,
        hobbies = hobbies, spokenLanguages = spokenLanguages, personalityType = personalityType,
        fitnessActivities = fitnessActivities, countryOfResidence = countryOfResidence.ifBlank { "India" },
        citizenship = citizenship.ifBlank { "Indian" }, residentialStatus = residentialStatus,
        visaStatus = visaStatus, willingToRelocate = willingToRelocate, rasi = rasi,
        nakshatra = nakshatra, manglik = manglik, birthTime = birthTime, birthPlace = birthPlace
    )

    private fun UserEntity.applyWizard(s: WizardState, reservedUsername: String): UserEntity {
        val ageFromDob = runCatching { Period.between(LocalDate.parse(s.dateOfBirth.trim()), LocalDate.now()).years }.getOrNull()
        val isNri = s.countryOfResidence.isNotBlank() && !s.countryOfResidence.equals("India", ignoreCase = true)
        val religionCategory = ReligionCategory.fromReligion(s.religion)
        val isHindu = religionCategory == ReligionCategory.HINDU
        val schema = ReligionProfileSchemas.forReligion(s.religion)
        return copy(
            username = reservedUsername, displayName = s.displayName.trim(), dateOfBirth = s.dateOfBirth.trim(),
            age = ageFromDob?.takeIf { it in 18..99 } ?: age, state = s.state.trim(), city = s.city.trim(),
            motherTongue = s.motherTongue.trim(), bio = s.bio.trim().take(1000), religion = s.religion.trim(),
            caste = s.caste.trim(), subCaste = s.subCaste.trim(), gothra = if (isHindu) s.gothra.trim() else "",
            faithTradition = if (schema.primary != null) s.faithTradition.trim().take(120) else "",
            faithSubTradition = if (schema.secondary != null) s.faithSubTradition.trim().take(120) else "",
            faithInstitution = if (schema.institution != null) s.faithInstitution.trim().take(160) else "",
            education = s.education.trim(), educationField = s.educationField.trim(), institution = s.institution.trim(),
            graduationYear = s.graduationYear, profession = s.profession.trim(), occupationCategory = s.occupationCategory.trim(),
            employer = s.employer.trim(), employerType = s.employerType.trim(), incomeBand = s.incomeBand.trim(),
            heightCm = s.heightCm, weight = s.weight, complexion = s.complexion.trim(), physicalStatus = s.physicalStatus.trim(),
            maritalStatus = s.maritalStatus.trim(), hasChildren = s.hasChildren, nativeState = s.nativeState.trim(),
            familyType = s.familyType.trim(), familyStatus = s.familyStatus.trim(), fatherOccupation = s.fatherOccupation.trim(),
            motherOccupation = s.motherOccupation.trim(), siblings = s.siblings.coerceAtLeast(0), familyValues = s.familyValues.trim(),
            aboutFamily = s.aboutFamily.trim().take(1000), diet = s.diet.trim(), smoking = s.smoking.trim(), drinking = s.drinking.trim(),
            hobbies = s.hobbies.trim(), spokenLanguages = s.spokenLanguages.trim(), personalityType = s.personalityType.trim(),
            fitnessActivities = s.fitnessActivities.trim(), countryOfResidence = s.countryOfResidence.trim(), citizenship = s.citizenship.trim(),
            residentialStatus = s.residentialStatus.trim(), visaStatus = s.visaStatus.trim(), willingToRelocate = s.willingToRelocate,
            isNRI = isNri, rasi = if (isHindu) s.rasi.trim() else "", nakshatra = if (isHindu) s.nakshatra.trim() else "",
            manglik = if (isHindu) s.manglik.trim() else "", birthTime = s.birthTime.trim(), birthPlace = s.birthPlace.trim()
        )
    }

    private fun calculateCompleteness(s: WizardState): Float {
        val checks = listOf(
            s.username.isNotBlank(), s.displayName.isNotBlank(), s.dateOfBirth.isNotBlank(),
            s.state.isNotBlank(), s.city.isNotBlank(), s.motherTongue.isNotBlank(), s.bio.isNotBlank(),
            s.religion.isNotBlank(), s.education.isNotBlank(), s.profession.isNotBlank(),
            s.heightCm > 0, s.maritalStatus.isNotBlank(), s.familyType.isNotBlank(), s.familyValues.isNotBlank(),
            s.diet.isNotBlank(), s.countryOfResidence.isNotBlank(), s.incomeBand.isNotBlank(),
            s.employer.isNotBlank(), s.aboutFamily.isNotBlank(), s.spokenLanguages.isNotBlank()
        )
        return checks.count { it }.toFloat() / checks.size.toFloat()
    }

    private companion object {
        const val LAST_STEP = 7
        const val RELIGION_STEP = 1
        val REQUIRED_STEPS = setOf(0, 1, 2, 3)
    }
}
