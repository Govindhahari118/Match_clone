package com.match.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WizardState(
    val displayName: String = "",
    val dateOfBirth: String = "",
    val city: String = "",
    val bio: String = "",
    // Community
    val religion: String = "Hindu",
    val caste: String = "",
    val subCaste: String = "",
    val motherTongue: String = "Telugu",
    val gothra: String = "",
    // Education & Career
    val education: String = "",
    val educationField: String = "",
    val institution: String = "",
    val profession: String = "",
    val employer: String = "",
    val incomeBand: String = "",
    // Physical
    val heightCm: Int = 0,
    val weight: Float = 0f,
    val complexion: String = "",
    val maritalStatus: String = "Never Married",
    // Family
    val familyType: String = "",
    val familyStatus: String = "",
    val fatherOccupation: String = "",
    val motherOccupation: String = "",
    val siblings: Int = 0,
    val familyValues: String = "",
    // Lifestyle
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    val fitnessActivities: String = "",
    // Astrology
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
    private val firestoreProfile: FirestoreProfileService
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState = _wizardState.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    init {
        // Pre-fill from existing profile if available
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            _wizardState.value = WizardState(
                displayName = user.displayName,
                dateOfBirth = user.dateOfBirth,
                city = user.city,
                bio = user.bio,
                religion = user.religion,
                caste = user.caste,
                subCaste = user.subCaste,
                motherTongue = user.motherTongue,
                gothra = user.gothra,
                education = user.education,
                educationField = user.educationField,
                institution = user.institution,
                profession = user.profession,
                employer = user.employer,
                incomeBand = user.incomeBand,
                heightCm = user.heightCm,
                weight = user.weight,
                complexion = user.complexion,
                maritalStatus = user.maritalStatus,
                familyType = user.familyType,
                familyStatus = user.familyStatus,
                fatherOccupation = user.fatherOccupation,
                motherOccupation = user.motherOccupation,
                siblings = user.siblings,
                familyValues = user.familyValues,
                diet = user.diet,
                smoking = user.smoking,
                drinking = user.drinking,
                fitnessActivities = user.fitnessActivities,
                rasi = user.rasi,
                nakshatra = user.nakshatra,
                manglik = user.manglik,
                birthTime = user.birthTime,
                birthPlace = user.birthPlace
            )
        }
    }

    fun update(state: WizardState) { _wizardState.value = state }

    fun nextStep() {
        saveCurrentStep()
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(6)
    }

    fun previousStep() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

    fun skipStep() {
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(6)
    }

    fun finish(onComplete: () -> Unit) {
        saveCurrentStep()
        viewModelScope.launch {
            _saving.value = true
            // Recalculate completeness
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            val completeness = calculateCompleteness()
            val updated = user.copy(profileCompleteness = completeness)
            userDao.update(updated)
            // Push to Firestore
            if (user.firebaseUid.isNotBlank()) {
                try { firestoreProfile.pushProfile(updated) } catch (_: Exception) {}
            }
            _saving.value = false
            onComplete()
        }
    }

    private fun saveCurrentStep() {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            val s = _wizardState.value
            val updated = user.copy(
                displayName = s.displayName.ifBlank { user.displayName },
                dateOfBirth = s.dateOfBirth,
                city = s.city.ifBlank { user.city },
                bio = s.bio,
                religion = s.religion,
                caste = s.caste,
                subCaste = s.subCaste,
                motherTongue = s.motherTongue,
                gothra = s.gothra,
                education = s.education,
                educationField = s.educationField,
                institution = s.institution,
                profession = s.profession,
                employer = s.employer,
                incomeBand = s.incomeBand,
                heightCm = s.heightCm,
                weight = s.weight,
                complexion = s.complexion,
                maritalStatus = s.maritalStatus,
                familyType = s.familyType,
                familyStatus = s.familyStatus,
                fatherOccupation = s.fatherOccupation,
                motherOccupation = s.motherOccupation,
                siblings = s.siblings,
                familyValues = s.familyValues,
                diet = s.diet,
                smoking = s.smoking,
                drinking = s.drinking,
                fitnessActivities = s.fitnessActivities,
                rasi = s.rasi,
                nakshatra = s.nakshatra,
                manglik = s.manglik,
                birthTime = s.birthTime,
                birthPlace = s.birthPlace
            )
            userDao.update(updated)
        }
    }

    private fun calculateCompleteness(): Float {
        val s = _wizardState.value
        var filled = 0
        val total = 20
        if (s.displayName.isNotBlank()) filled++
        if (s.dateOfBirth.isNotBlank()) filled++
        if (s.city.isNotBlank()) filled++
        if (s.bio.isNotBlank()) filled++
        if (s.religion.isNotBlank()) filled++
        if (s.motherTongue.isNotBlank()) filled++
        if (s.education.isNotBlank()) filled++
        if (s.profession.isNotBlank()) filled++
        if (s.heightCm > 0) filled++
        if (s.maritalStatus.isNotBlank()) filled++
        if (s.familyType.isNotBlank()) filled++
        if (s.rasi.isNotBlank()) filled++
        if (s.nakshatra.isNotBlank()) filled++
        if (s.employer.isNotBlank()) filled++
        if (s.diet.isNotBlank()) filled++
        if (s.caste.isNotBlank()) filled++
        if (s.familyValues.isNotBlank()) filled++
        if (s.weight > 0f) filled++
        if (s.birthTime.isNotBlank()) filled++
        if (s.incomeBand.isNotBlank()) filled++
        return filled.toFloat() / total
    }
}
