package com.match.app.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.FamilyAccessRepository
import com.match.app.data.repo.FamilyAccessSnapshot
import com.match.app.data.repo.FamilyInvite
import com.match.app.data.repo.ManagedFamilyProfile
import com.match.app.data.repo.ManagedFamilyProfileDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FamilyAccessUi(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val snapshot: FamilyAccessSnapshot = FamilyAccessSnapshot(emptyList(), emptyList()),
    val role: String = "PARENT",
    val canEdit: Boolean = false,
    val invite: FamilyInvite? = null,
    val acceptToken: String = "",
    val selectedAccess: ManagedFamilyProfile? = null,
    val selectedProfile: ManagedFamilyProfileDetail? = null,
    val editCity: String = "",
    val editBio: String = "",
    val editEducation: String = "",
    val editProfession: String = "",
    val message: String? = null
)

@HiltViewModel
class FamilyAccessViewModel @Inject constructor(
    private val repository: FamilyAccessRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(FamilyAccessUi())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, message = null) }
        runCatching { repository.listAccess() }
            .onSuccess { snapshot ->
                _ui.update { it.copy(loading = false, snapshot = snapshot) }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        loading = false,
                        message = error.message?.take(180) ?: "Could not load family access."
                    )
                }
            }
    }

    fun setRole(value: String) {
        if (value in setOf("PARENT", "SIBLING", "GUARDIAN")) {
            _ui.update { it.copy(role = value, message = null) }
        }
    }

    fun setCanEdit(value: Boolean) = _ui.update { it.copy(canEdit = value, message = null) }

    fun createInvite() = viewModelScope.launch {
        if (_ui.value.busy) return@launch
        _ui.update { it.copy(busy = true, message = null, invite = null) }
        runCatching { repository.createInvite(_ui.value.role, _ui.value.canEdit) }
            .onSuccess { invite ->
                _ui.update {
                    it.copy(
                        busy = false,
                        invite = invite,
                        message = "Family invite created. Share the token only with the intended person."
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        busy = false,
                        message = error.message?.take(180) ?: "Could not create family invite."
                    )
                }
            }
    }

    fun setAcceptToken(value: String) {
        _ui.update { it.copy(acceptToken = value.trim().take(160), message = null) }
    }

    fun acceptInvite() = viewModelScope.launch {
        if (_ui.value.busy || _ui.value.acceptToken.isBlank()) return@launch
        _ui.update { it.copy(busy = true, message = null) }
        runCatching { repository.acceptInvite(_ui.value.acceptToken) }
            .onSuccess {
                val snapshot = repository.listAccess()
                _ui.update {
                    it.copy(
                        busy = false,
                        acceptToken = "",
                        snapshot = snapshot,
                        message = "Family access accepted."
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        busy = false,
                        message = error.message?.take(180) ?: "Could not accept this invite."
                    )
                }
            }
    }

    fun revoke(delegateUid: String) = viewModelScope.launch {
        if (_ui.value.busy) return@launch
        _ui.update { it.copy(busy = true, message = null) }
        runCatching {
            repository.revoke(delegateUid)
            repository.listAccess()
        }.onSuccess { snapshot ->
            _ui.update {
                it.copy(
                    busy = false,
                    snapshot = snapshot,
                    message = "Family access revoked."
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    busy = false,
                    message = error.message?.take(180) ?: "Could not revoke family access."
                )
            }
        }
    }

    fun openManaged(access: ManagedFamilyProfile) = viewModelScope.launch {
        if (_ui.value.busy) return@launch
        _ui.update {
            it.copy(
                busy = true,
                message = null,
                selectedAccess = access,
                selectedProfile = null
            )
        }
        runCatching { repository.getManagedProfile(access.ownerUid) }
            .onSuccess { profile ->
                _ui.update {
                    it.copy(
                        busy = false,
                        selectedProfile = profile,
                        editCity = profile.city,
                        editBio = profile.bio,
                        editEducation = profile.education,
                        editProfession = profile.profession
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        busy = false,
                        selectedAccess = null,
                        message = error.message?.take(180) ?: "Could not open managed profile."
                    )
                }
            }
    }

    fun closeManaged() {
        _ui.update { it.copy(selectedAccess = null, selectedProfile = null, message = null) }
    }

    fun editCity(value: String) = _ui.update { it.copy(editCity = value.take(160)) }
    fun editBio(value: String) = _ui.update { it.copy(editBio = value.take(1000)) }
    fun editEducation(value: String) = _ui.update { it.copy(editEducation = value.take(160)) }
    fun editProfession(value: String) = _ui.update { it.copy(editProfession = value.take(160)) }

    fun saveManaged() = viewModelScope.launch {
        val access = _ui.value.selectedAccess ?: return@launch
        val profile = _ui.value.selectedProfile ?: return@launch
        if ("EDIT_PROFILE" !in access.permissions || _ui.value.busy) return@launch

        _ui.update { it.copy(busy = true, message = null) }
        runCatching {
            repository.updateManagedProfile(
                ownerUid = access.ownerUid,
                expectedRevision = profile.profileRevision,
                city = _ui.value.editCity,
                bio = _ui.value.editBio,
                education = _ui.value.editEducation,
                profession = _ui.value.editProfession
            )
            repository.getManagedProfile(access.ownerUid)
        }.onSuccess { updated ->
            _ui.update {
                it.copy(
                    busy = false,
                    selectedProfile = updated,
                    editCity = updated.city,
                    editBio = updated.bio,
                    editEducation = updated.education,
                    editProfession = updated.profession,
                    message = "Managed profile updated."
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    busy = false,
                    message = error.message?.take(180)
                        ?: "Could not update the managed profile. Refresh and try again."
                )
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyAccessScreen(
    onBack: () -> Unit = {},
    vm: FamilyAccessViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Access") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("family_access_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.busy) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh family access")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("family_access_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Explicit, revocable family access", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Invites expire after 24 hours. View access never grants chat, payments, verification, privacy, religion-lock, contact or account-lifecycle authority. Edit access is limited by the backend to non-sensitive profile fields.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ui.loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Loading family access…")
                }
            }

            Text("Invite a family member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PARENT", "SIBLING", "GUARDIAN").forEach { role ->
                    FilterChip(
                        selected = ui.role == role,
                        onClick = { vm.setRole(role) },
                        label = { Text(role.lowercase().replaceFirstChar { it.uppercase() }) },
                        enabled = !ui.busy
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Allow non-sensitive profile edits", fontWeight = FontWeight.Medium)
                    Text(
                        "City, bio, education and profession in this screen; the server enforces a wider safe allowlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = ui.canEdit, onCheckedChange = vm::setCanEdit, enabled = !ui.busy)
            }
            Button(
                onClick = vm::createInvite,
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create 24-hour invite") }

            ui.invite?.let { invite ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Invite token", fontWeight = FontWeight.SemiBold)
                        SelectionContainer { Text(invite.inviteToken) }
                        Text(
                            "Expires " + DateFormat.getDateTimeInstance().format(Date(invite.expiresAtMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(invite.inviteToken)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy invite token")
                        }
                    }
                }
            }

            Text("Accept an invite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = ui.acceptToken,
                onValueChange = vm::setAcceptToken,
                label = { Text("Invite token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = vm::acceptInvite,
                enabled = !ui.busy && ui.acceptToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Accept family access") }

            Text("People with access to my profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val activeDelegates = ui.snapshot.delegates.filter { it.active }
            if (activeDelegates.isEmpty()) {
                Text("No active family delegates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            activeDelegates.forEach { delegate ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(delegate.role.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Account " + delegate.delegateUid.take(8) + "… • " +
                                delegate.permissions.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { vm.revoke(delegate.delegateUid) },
                            enabled = !ui.busy
                        ) { Text("Revoke access") }
                    }
                }
            }

            Text("Profiles I can help manage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (ui.snapshot.managedProfiles.isEmpty()) {
                Text("No managed profiles.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ui.snapshot.managedProfiles.forEach { access ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(access.role.lowercase().replaceFirstChar { it.uppercase() } + " access", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Profile " + access.ownerUid.take(8) + "… • " + access.permissions.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { vm.openManaged(access) },
                            enabled = !ui.busy
                        ) { Text("Open managed profile") }
                    }
                }
            }

            val selected = ui.selectedProfile
            val selectedAccess = ui.selectedAccess
            if (selected != null && selectedAccess != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            selected.displayName.ifBlank { "Managed profile" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if ("EDIT_PROFILE" in selectedAccess.permissions) {
                                "You have explicit edit access to the fields below."
                            } else {
                                "View-only family access. Editing is disabled."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = ui.editCity,
                            onValueChange = vm::editCity,
                            label = { Text("City") },
                            enabled = "EDIT_PROFILE" in selectedAccess.permissions && !ui.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.editBio,
                            onValueChange = vm::editBio,
                            label = { Text("Bio") },
                            enabled = "EDIT_PROFILE" in selectedAccess.permissions && !ui.busy,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.editEducation,
                            onValueChange = vm::editEducation,
                            label = { Text("Education") },
                            enabled = "EDIT_PROFILE" in selectedAccess.permissions && !ui.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.editProfession,
                            onValueChange = vm::editProfession,
                            label = { Text("Profession") },
                            enabled = "EDIT_PROFILE" in selectedAccess.permissions && !ui.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if ("EDIT_PROFILE" in selectedAccess.permissions) {
                            Button(
                                onClick = vm::saveManaged,
                                enabled = !ui.busy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save delegated edits") }
                        }
                        OutlinedButton(onClick = vm::closeManaged, modifier = Modifier.fillMaxWidth()) {
                            Text("Close managed profile")
                        }
                    }
                }
            }

            ui.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
