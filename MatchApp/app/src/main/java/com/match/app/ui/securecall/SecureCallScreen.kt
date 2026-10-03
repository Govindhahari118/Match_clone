package com.match.app.ui.securecall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.SecureCallCapability
import com.match.app.data.repo.SecureCallRepository
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecureCallUi(
    val loading: Boolean = true,
    val memberName: String = "",
    val capability: SecureCallCapability? = null,
    val error: String? = null
)

@HiltViewModel
class SecureCallViewModel @Inject constructor(
    private val userDao: UserDao,
    private val repository: SecureCallRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(SecureCallUi())
    val ui: StateFlow<SecureCallUi> = _ui.asStateFlow()

    fun load(peerId: Long) = viewModelScope.launch {
        if (peerId <= 0L) {
            _ui.value = SecureCallUi(loading = false, error = "Invalid member")
            return@launch
        }
        val peer = userDao.findById(peerId)
        val targetUid = peer?.firebaseUid.orEmpty()
        if (targetUid.isBlank()) {
            _ui.value = SecureCallUi(
                loading = false,
                memberName = peer?.displayName.orEmpty(),
                error = "This profile is not linked to a verified remote account."
            )
            return@launch
        }

        _ui.value = SecureCallUi(loading = true, memberName = peer?.displayName.orEmpty())
        runCatching { repository.capability(targetUid) }
            .onSuccess { capability ->
                _ui.value = SecureCallUi(
                    loading = false,
                    memberName = peer?.displayName.orEmpty(),
                    capability = capability
                )
            }
            .onFailure { error ->
                _ui.value = SecureCallUi(
                    loading = false,
                    memberName = peer?.displayName.orEmpty(),
                    error = error.message ?: "Could not check secure-call availability."
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureCallScreen(
    peerId: Long,
    onBack: () -> Unit = {},
    vm: SecureCallViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(peerId) { vm.load(peerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("secure_calls", "Secure Calls")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("securecall_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)
                .testTag("secure_call_screen"),
            contentAlignment = Alignment.Center
        ) {
            when {
                ui.loading -> CircularProgressIndicator()
                ui.error != null -> MatreeInlineNotice(
                    message = ui.error.orEmpty(),
                    icon = Icons.Filled.Security
                )
                else -> SecureCallCapabilityCard(
                    memberName = ui.memberName,
                    capability = ui.capability ?: SecureCallCapability()
                )
            }
        }
    }
}

@Composable
private fun SecureCallCapabilityCard(
    memberName: String,
    capability: SecureCallCapability
) {
    val relationshipMessage = when (capability.reason) {
        "eligible" -> "Your relationship is eligible for secure communication."
        "not_mutual_match" -> "Secure calling unlocks only after both members express interest."
        "blocked" -> "Secure calling is unavailable because this relationship is blocked."
        "privacy_restricted" -> "Secure calling is unavailable because a member's privacy settings restrict contact."
        "inactive_account" -> "Secure calling is unavailable while either account is inactive."
        "invalid_pair" -> "Secure calling is unavailable for this member."
        else -> "Secure-call eligibility could not be confirmed."
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("secure_call_capability"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                if (memberName.isBlank()) "Secure Call" else "Secure Call with $memberName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                relationshipMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (capability.eligible && !capability.providerReady) {
                MatreeInlineNotice(
                    message = "Your match is eligible, but live calling is not enabled yet. Matree will not expose a fake dialer, relay number, call history or minutes until a real audited provider is connected.",
                    icon = Icons.Filled.Security
                )
            } else if (capability.providerReady) {
                Text(
                    buildString {
                        append("Provider capabilities: ")
                        val values = buildList {
                            if (capability.voiceAvailable) add("voice")
                            if (capability.videoAvailable) add("video")
                            if (capability.numberMaskingAvailable) add("number masking")
                        }
                        append(values.ifEmpty { listOf("none") }.joinToString())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
