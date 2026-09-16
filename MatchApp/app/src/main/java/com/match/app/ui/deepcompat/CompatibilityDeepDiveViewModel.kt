package com.match.app.ui.deepcompat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.matching.MatchScoreEngine
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompatScoreState(
    val isLoading: Boolean = true,
    val totalPct: Int = 0,
    val coveragePct: Int = 0,
    val astrologyApplicable: Boolean = false,
    val astrology: Int = 0,
    val religionCaste: Int = 0,
    val educationCareer: Int = 0,
    val location: Int = 0,
    val age: Int = 0,
    val familyValues: Int = 0,
    val lifestyle: Int = 0,
    val personality: Int = 0
)

@HiltViewModel
class CompatibilityDeepDiveViewModel @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(CompatScoreState())
    val state = _state.asStateFlow()

    fun load(candidateId: Long) {
        viewModelScope.launch {
            _state.value = CompatScoreState(isLoading = true)
            val myId = session.userId.first()
            val me = if (myId != null) userDao.findById(myId) else null
            val candidate = userDao.findById(candidateId)
            if (me != null && candidate != null) {
                val bd = MatchScoreEngine.compute(me, candidate)
                _state.value = CompatScoreState(
                    isLoading = false,
                    totalPct = (bd.total * 100).toInt().coerceIn(0, 100),
                    coveragePct = (bd.coverage * 100).toInt().coerceIn(0, 100),
                    astrologyApplicable = bd.astrologyApplicable,
                    astrology = (bd.astrology * 100).toInt().coerceIn(0, 100),
                    religionCaste = (bd.religionCaste * 100).toInt().coerceIn(0, 100),
                    educationCareer = (bd.educationCareer * 100).toInt().coerceIn(0, 100),
                    location = (bd.location * 100).toInt().coerceIn(0, 100),
                    age = (bd.age * 100).toInt().coerceIn(0, 100),
                    familyValues = (bd.familyValues * 100).toInt().coerceIn(0, 100),
                    lifestyle = (bd.lifestyle * 100).toInt().coerceIn(0, 100),
                    personality = (bd.personality * 100).toInt().coerceIn(0, 100)
                )
            } else {
                _state.value = CompatScoreState(isLoading = false)
            }
        }
    }
}
