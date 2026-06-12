package com.match.app.ui.deepcompat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.matching.MatchScoreEngine
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first

data class CompatScoreState(
    val isLoading: Boolean = true,
    val totalPct: Int = 0,
    val astrology: Int = 0,
    val religionCaste: Int = 0,
    val educationCareer: Int = 0,
    val location: Int = 0,
    val age: Int = 0,
    val familyValues: Int = 0,
    val lifestyle: Int = 0,
    val physical: Int = 0,
    val personality: Int = 0
)

@HiltViewModel
class CompatibilityDeepDiveViewModel @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(CompatScoreState())
    val state = _state.asStateFlow()

    val planKey: StateFlow<String> = session.subscriptionPlan
        .stateIn(viewModelScope, SharingStarted.Eagerly, "FREE")

    fun load(candidateId: Long) {
        viewModelScope.launch {
            val myId = session.userId.first()
            val me = if (myId != null) userDao.findById(myId) else null
            val candidate = userDao.findById(candidateId)
            if (me != null && candidate != null) {
                val bd = MatchScoreEngine.compute(me, candidate)
                _state.value = CompatScoreState(
                    isLoading      = false,
                    totalPct       = (bd.total * 100).toInt(),
                    astrology      = (bd.astrology * 100).toInt(),
                    religionCaste  = (bd.religionCaste * 100).toInt(),
                    educationCareer = (bd.educationCareer * 100).toInt(),
                    location       = (bd.location * 100).toInt(),
                    age            = (bd.age * 100).toInt(),
                    familyValues   = (bd.familyValues * 100).toInt(),
                    lifestyle      = (bd.lifestyle * 100).toInt(),
                    physical       = (bd.physical * 100).toInt(),
                    personality    = (bd.personality * 100).toInt()
                )
            } else {
                _state.value = CompatScoreState(isLoading = false)
            }
        }
    }
}
