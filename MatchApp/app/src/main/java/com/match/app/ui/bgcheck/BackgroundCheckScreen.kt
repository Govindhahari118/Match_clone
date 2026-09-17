package com.match.app.ui.bgcheck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.BackgroundCheckService
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

private data class CheckPackage(
    val name: String,
    val scope: List<String>
)

private val CHECK_PACKAGES = listOf(
    CheckPackage(
        "Basic Verify",
        listOf("Identity and address checks", "Basic public-record checks", "Marital-status review")
    ),
    CheckPackage(
        "Professional",
        listOf("Basic scope", "Education and employment review", "Professional-claim review")
    ),
    CheckPackage(
        "Premium 360°",
        listOf("Professional scope", "Additional family/reference review", "Expanded records review")
    )
)

private val MATRIMONY_ID_PATTERN = Regex("^TLG-[A-Z0-9]{5}$")

data class BackgroundCheckUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val targetProfileId: String = "",
    val selectedPlan: String = "Professional",
    val requests: List<BackgroundCheckService.Request> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BackgroundCheckViewModel @Inject constructor(
    private val session: SessionStore,
    private val service: BackgroundCheckService
) : ViewModel() {
    private val _ui = MutableStateFlow(BackgroundCheckUiState())
    val ui: StateFlow<BackgroundCheckUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun onProfileIdChange(value: String) {
        val normalized = value.uppercase()
            .filter { it.isLetterOrDigit() || it == '-' }
            .take(9)
        _ui.update { it.copy(targetProfileId = normalized, error = null) }
    }

    fun onPlanChange(value: String) {
        if (CHECK_PACKAGES.any { it.name == value }) {
            _ui.update { it.copy(selectedPlan = value, error = null) }
        }
    }

    fun refresh() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull()
        if (uid.isNullOrBlank()) {
            _ui.update { it.copy(loading = false, error = "Sign in to view background-check requests.") }
            return@launch
        }
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { service.list(uid) }
            .onSuccess { requests -> _ui.update { it.copy(loading = false, requests = requests) } }
            .onFailure {
                _ui.update {
                    it.copy(
                        loading = false,
                        error = "Unable to load background-check requests."
                    )
                }
            }
    }

    fun submit() = viewModelScope.launch {
        val state = _ui.value
        if (!MATRIMONY_ID_PATTERN.matches(state.targetProfileId)) {
            _ui.update { it.copy(error = "Enter a valid Matrimony ID such as TLG-A1B2C.") }
            return@launch
        }
        _ui.update { it.copy(submitting = true, error = null) }
        runCatching { service.request(state.targetProfileId, state.selectedPlan) }
            .onSuccess {
                _ui.update { current ->
                    current.copy(submitting = false, targetProfileId = "")
                }
                refresh()
            }
            .onFailure { error ->
                val message = error.message.orEmpty()
                _ui.update {
                    it.copy(
                        submitting = false,
                        error = when {
                            message.contains("mutual match", ignoreCase = true) ->
                                "Background-check requests are available only for mutual matches."
                            message.contains("not found", ignoreCase = true) ->
                                "No profile was found for that Matrimony ID."
                            else -> "Unable to submit the request. Check the profile ID and try again."
                        }
                    )
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundCheckScreen(
    onBack: () -> Unit = {},
    vm: BackgroundCheckViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("background_verification", "Background Verification")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("bgcheck_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.loading) {
                        Icon(Icons.Filled.Refresh, "Refresh requests")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("bg_check_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        t("verify_before_you_trust", "Request a Background Check"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "This screen records service requests only. It never fabricates verification findings. Availability, consent, scope, pricing, and any provider report must be confirmed by the operations workflow before a result is shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (ui.error != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        ui.error.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                "Request for a mutual match",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = ui.targetProfileId,
                onValueChange = vm::onProfileIdChange,
                label = { Text("Matrimony ID") },
                placeholder = { Text("TLG-A1B2C") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.PersonSearch, null) },
                supportingText = { Text("Only a real mutual match can be submitted.") }
            )

            Text(
                t("choose_package", "Requested Scope"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            CHECK_PACKAGES.forEach { pkg ->
                val selected = ui.selectedPlan == pkg.name
                ElevatedCard(
                    onClick = { vm.onPlanChange(pkg.name) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pkg.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { vm.onPlanChange(pkg.name) }
                            )
                        }
                        Text(
                            "Requested scope; final availability is confirmed before processing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        pkg.scope.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = vm::submit,
                enabled = MATRIMONY_ID_PATTERN.matches(ui.targetProfileId) && !ui.submitting,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("request_bgcheck_btn")
            ) {
                if (ui.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Shield, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Service Request")
                }
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            if (!ui.loading && ui.requests.isEmpty()) {
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("No background-check requests yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "No verification report exists until a real request is processed by the backend/provider workflow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            ui.requests.forEach { request ->
                RequestCard(request)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RequestCard(request: BackgroundCheckService.Request) {
    val statusLabel = when (request.status.lowercase()) {
        "submitted" -> "Submitted"
        "processing" -> "Processing"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        "unable_to_verify" -> "Unable to verify"
        else -> request.status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        request.targetProfileId.ifBlank { "Matched profile" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        request.plan,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(onClick = {}, enabled = false, label = { Text(statusLabel) })
            }
            if (request.createdAtMillis > 0) {
                Text(
                    "Requested ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(request.createdAtMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                when (request.status.lowercase()) {
                    "submitted" -> "The request is recorded. No verification finding has been produced yet."
                    "processing" -> "The server workflow reports processing. Findings are not assumed until a real report is returned."
                    "completed" -> "The server marks this request completed. This screen does not invent or infer individual check results."
                    "unable_to_verify" -> "The workflow could not verify the requested information."
                    "cancelled" -> "This request is cancelled and has no active verification result."
                    else -> "Status is provided by the backend workflow."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
