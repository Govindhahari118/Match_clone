package com.match.app.ui.securecall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.match.app.data.repo.SecureCallRequest
import com.match.app.data.repo.SecureCallRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SecureCallUi(
    val loading: Boolean = true,
    val memberName: String = "",
    val targetUid: String = "",
    val currentUid: String = "",
    val capability: SecureCallCapability? = null,
    val request: SecureCallRequest = SecureCallRequest(),
    val busy: Boolean = false,
    val feedback: String? = null,
    val error: String? = null
)

@HiltViewModel
class SecureCallViewModel @Inject constructor(
    private val userDao: UserDao,
    private val repository: SecureCallRepository,
    private val session: SessionStore
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

        val currentUid = session.firebaseUid.filterNotNull().first()
        _ui.value = SecureCallUi(
            loading = true,
            memberName = peer?.displayName.orEmpty(),
            targetUid = targetUid,
            currentUid = currentUid
        )
        runCatching {
            repository.capability(targetUid) to repository.callRequest(targetUid)
        }
            .onSuccess { (capability, request) ->
                _ui.value = SecureCallUi(
                    loading = false,
                    memberName = peer?.displayName.orEmpty(),
                    targetUid = targetUid,
                    currentUid = currentUid,
                    capability = capability,
                    request = request
                )
            }
            .onFailure { error ->
                _ui.value = SecureCallUi(
                    loading = false,
                    memberName = peer?.displayName.orEmpty(),
                    targetUid = targetUid,
                    currentUid = currentUid,
                    error = error.message ?: "Could not check secure-call availability."
                )
            }
    }

    private suspend fun refreshRequest() {
        val targetUid = _ui.value.targetUid
        if (targetUid.isBlank()) return
        runCatching { repository.callRequest(targetUid) }
            .onSuccess { request -> _ui.value = _ui.value.copy(request = request, busy = false) }
            .onFailure { error -> _ui.value = _ui.value.copy(busy = false, error = error.message) }
    }

    fun requestCall(delayMinutes: Long) = viewModelScope.launch {
        val current = _ui.value
        if (current.busy || current.targetUid.isBlank() || current.capability?.eligible != true) return@launch
        _ui.value = current.copy(busy = true, feedback = null, error = null)
        val proposedAt = System.currentTimeMillis() + delayMinutes * 60_000L
        runCatching { repository.requestCall(current.targetUid, proposedAt, "VOICE") }
            .onSuccess {
                _ui.value = _ui.value.copy(feedback = "requested")
                refreshRequest()
            }
            .onFailure { error ->
                _ui.value = _ui.value.copy(busy = false, error = error.message ?: "Could not request a call.")
            }
    }

    fun respond(accept: Boolean) = viewModelScope.launch {
        val current = _ui.value
        if (current.busy || current.targetUid.isBlank()) return@launch
        _ui.value = current.copy(busy = true, feedback = null, error = null)
        runCatching { repository.respondToCallRequest(current.targetUid, accept) }
            .onSuccess {
                _ui.value = _ui.value.copy(feedback = if (accept) "accepted" else "declined")
                refreshRequest()
            }
            .onFailure { error ->
                _ui.value = _ui.value.copy(busy = false, error = error.message ?: "Could not update the call request.")
            }
    }

    fun cancelRequest() = viewModelScope.launch {
        val current = _ui.value
        if (current.busy || current.targetUid.isBlank()) return@launch
        _ui.value = current.copy(busy = true, feedback = null, error = null)
        runCatching { repository.cancelCallRequest(current.targetUid) }
            .onSuccess {
                _ui.value = _ui.value.copy(feedback = "cancelled")
                refreshRequest()
            }
            .onFailure { error ->
                _ui.value = _ui.value.copy(busy = false, error = error.message ?: "Could not cancel the call request.")
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
                    currentUid = ui.currentUid,
                    capability = ui.capability ?: SecureCallCapability(),
                    request = ui.request,
                    busy = ui.busy,
                    onRequest = vm::requestCall,
                    onAccept = { vm.respond(true) },
                    onDecline = { vm.respond(false) },
                    onCancel = vm::cancelRequest
                )
            }
        }
    }
}

@Composable
private fun SecureCallCapabilityCard(
    memberName: String,
    currentUid: String,
    capability: SecureCallCapability,
    request: SecureCallRequest,
    busy: Boolean,
    onRequest: (Long) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val relationshipMessage = when (capability.reason) {
        "eligible" -> t("secure_call_eligible", "Your relationship is eligible for secure communication.")
        "not_mutual_match" -> t("secure_call_not_mutual", "Secure calling unlocks only after both members express interest.")
        "blocked" -> t("secure_call_blocked", "Secure calling is unavailable because this relationship is blocked.")
        "privacy_restricted" -> t("secure_call_privacy", "Secure calling is unavailable because a member's privacy settings restrict contact.")
        "inactive_account" -> t("secure_call_inactive", "Secure calling is unavailable while either account is inactive.")
        "invalid_pair" -> t("secure_call_invalid", "Secure calling is unavailable for this member.")
        else -> t("secure_call_unconfirmed", "Secure-call eligibility could not be confirmed.")
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
                if (memberName.isBlank()) t("secure_call", "Secure Call") else t("secure_call_with", mapOf("name" to memberName), "Secure Call with {name}"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                relationshipMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (capability.eligible) {
                CallRequestCoordinator(
                    currentUid = currentUid,
                    request = request,
                    busy = busy,
                    onRequest = onRequest,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    onCancel = onCancel
                )
            }

            if (capability.eligible && !capability.providerReady) {
                MatreeInlineNotice(
                    message = t("secure_call_provider_pending", "Your match is eligible, but live calling is not enabled yet. You can still agree on a call time now; Matree will not show a fake dialer or relay number until an audited provider is connected."),
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


@Composable
private fun CallRequestCoordinator(
    currentUid: String,
    request: SecureCallRequest,
    busy: Boolean,
    onRequest: (Long) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val active = request.exists && (request.status == "PENDING" || request.status == "ACCEPTED")
    if (!active) {
        Text(
            t("request_call_time", "Request a call time"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            t("request_call_time_hint", "Choose a convenient time. Your match can accept or decline before live calling is enabled."),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onRequest(30) }, enabled = !busy) {
                Text(t("in_30_minutes", "In 30 min"))
            }
            OutlinedButton(onClick = { onRequest(60) }, enabled = !busy) {
                Text(t("in_1_hour", "In 1 hour"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onRequest(180) }, enabled = !busy) {
                Text(t("in_3_hours", "In 3 hours"))
            }
            OutlinedButton(onClick = { onRequest(24 * 60) }, enabled = !busy) {
                Text(t("tomorrow_same_time", "Tomorrow"))
            }
        }
        return
    }

    val formatted = remember(request.proposedAtMs) {
        SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(request.proposedAtMs))
    }
    Text(
        when (request.status) {
            "ACCEPTED" -> t("call_request_accepted", "Call time accepted")
            else -> t("call_request_pending", "Call request pending")
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        t("proposed_call_time", mapOf("time" to formatted), "Proposed time: {time}"),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )

    val viewerIsTarget = request.targetUid == currentUid
    val viewerIsRequester = request.requesterUid == currentUid
    if (request.status == "PENDING" && viewerIsTarget) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDecline, enabled = !busy) {
                Text(t("decline", "Decline"))
            }
            Button(onClick = onAccept, enabled = !busy) {
                Text(t("accept", "Accept"))
            }
        }
    } else if (viewerIsRequester) {
        OutlinedButton(onClick = onCancel, enabled = !busy) {
            Text(t("cancel_call_request", "Cancel request"))
        }
    }
}
