package com.match.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.NotificationRepository
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HomeActivityUi(
    val profile: UserProfile? = null,
    val mode: MatchMode = MatchMode.ADVANCED,
    val unreadNotif: Int = 0,
    val pendingInterests: Int = 0,
    val shortlistCount: Int = 0,
    val mutualCount: Int = 0,
    val hasQuestionnaire: Boolean = false,
    val newMutualName: String? = null
)

/**
 * Canonical production Home state.
 *
 * Network-backed repositories remain authoritative; local Room/DataStore values are presentation
 * caches/preferences only. The old catch-all HomeScreen callback surface was removed so hidden
 * prototype screens cannot be accidentally reintroduced through a legacy wrapper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val notifRepo: NotificationRepository,
    private val social: SocialRepository,
    private val shortlistRepo: ShortlistRepository
) : ViewModel() {
    private var lastMutualCount = 0

    val ui: StateFlow<HomeActivityUi> = combine(
        session.userId,
        session.firebaseUid
    ) { localId, firebaseUid ->
        localId to firebaseUid
    }.flatMapLatest { (me, firebaseUid) ->
        if (me == null || firebaseUid.isNullOrBlank()) {
            flowOf(HomeActivityUi())
        } else {
            combine(
                notifRepo.observeUnreadCount(me),
                social.observeReceivedInterestsRemote(firebaseUid),
                shortlistRepo.observeSavedIdsRemote(firebaseUid),
                social.observeMutualIdsRemote(firebaseUid),
                session.hasQuestionnaire
            ) { notif, incomingIds, savedIds, mutualIds, quizDone ->
                val profile = auth.currentProfile(me)
                val mutualCount = mutualIds.size
                val newMutualName =
                    if (mutualCount > lastMutualCount && lastMutualCount > 0) "Someone" else null
                lastMutualCount = mutualCount
                HomeActivityUi(
                    profile = profile,
                    mode = MatchMode.ADVANCED,
                    unreadNotif = notif,
                    pendingInterests = incomingIds.size,
                    shortlistCount = savedIds.size,
                    mutualCount = mutualCount,
                    hasQuestionnaire = quizDone,
                    newMutualName = newMutualName
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeActivityUi()
    )
}
