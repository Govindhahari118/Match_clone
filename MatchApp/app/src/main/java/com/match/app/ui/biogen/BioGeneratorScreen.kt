package com.match.app.ui.biogen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BioDraftUiState(
    val loading: Boolean = true,
    val name: String = "",
    val age: String = "",
    val profession: String = "",
    val education: String = "",
    val religion: String = "",
    val city: String = "",
    val draft: String = "",
    val saving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class BioGeneratorViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(BioDraftUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = session.userId.first()
            val profile = userId?.let { auth.currentProfile(it) }
            _ui.value = if (profile == null) {
                BioDraftUiState(loading = false, message = "Your profile could not be loaded.")
            } else {
                BioDraftUiState(
                    loading = false,
                    name = profile.displayName,
                    age = profile.age.takeIf { it > 0 }?.toString().orEmpty(),
                    profession = profile.profession,
                    education = profile.education,
                    religion = profile.religion,
                    city = profile.city
                )
            }
        }
    }

    fun updateName(value: String) = _ui.update { it.copy(name = value.take(100), message = null) }
    fun updateProfession(value: String) = _ui.update { it.copy(profession = value.take(160), message = null) }
    fun updateEducation(value: String) = _ui.update { it.copy(education = value.take(160), message = null) }
    fun updateReligion(value: String) = _ui.update { it.copy(religion = value.take(120), message = null) }
    fun updateCity(value: String) = _ui.update { it.copy(city = value.take(120), message = null) }

    fun createDraft() {
        val value = _ui.value
        val facts = buildList {
            value.age.takeIf { it.isNotBlank() }?.let { add(it + " years old") }
            value.city.takeIf { it.isNotBlank() }?.let { add("based in " + it) }
            value.profession.takeIf { it.isNotBlank() }?.let { add("working as " + it) }
        }
        val intro = when {
            value.name.isNotBlank() && facts.isNotEmpty() ->
                "I'm " + value.name + ", " + facts.joinToString(", ") + "."
            value.name.isNotBlank() -> "I'm " + value.name + "."
            facts.isNotEmpty() -> "I'm " + facts.joinToString(", ") + "."
            else -> "I'm looking to build a meaningful partnership based on mutual respect."
        }
        val details = buildList {
            value.education.takeIf { it.isNotBlank() }?.let {
                add("My education background is " + it + ".")
            }
            value.religion.takeIf { it.isNotBlank() }?.let {
                add("My profile lists my religion/community as " + it + ".")
            }
        }
        val closing = "I value honest communication, respect, and taking the time to understand each other."
        _ui.update {
            it.copy(
                draft = (listOf(intro) + details + closing).joinToString(" "),
                message = null
            )
        }
    }

    fun updateDraft(value: String) = _ui.update { it.copy(draft = value.take(1200), message = null) }

    fun saveDraft() = viewModelScope.launch {
        val userId = session.userId.first()
        val draft = _ui.value.draft.trim()
        if (userId == null || draft.length < 20) {
            _ui.update { it.copy(message = "Create and review a bio before saving.") }
            return@launch
        }
        _ui.update { it.copy(saving = true, message = null) }
        runCatching { auth.updateBio(userId, draft) }
            .onSuccess { _ui.update { it.copy(saving = false, message = "Bio saved to your profile.") } }
            .onFailure { _ui.update { it.copy(saving = false, message = "Could not save the bio.") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioGeneratorScreen(
    onBack: () -> Unit = {},
    vm: BioGeneratorViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bio Draft Helper") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("biogen_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("bio_generator_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Icon(Icons.Filled.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Deterministic draft helper", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "This is not an AI model. It builds an editable draft only from the details shown below, initially loaded from your signed-in profile. Review every sentence before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(ui.name, vm::updateName, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.profession, vm::updateProfession, label = { Text("Profession") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.education, vm::updateEducation, label = { Text("Education") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.religion, vm::updateReligion, label = { Text("Religion / community") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.city, vm::updateCity, label = { Text("City") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = vm::createDraft,
                enabled = !ui.loading && !ui.saving,
                modifier = Modifier.fillMaxWidth().testTag("biogen_btn")
            ) {
                Text(if (ui.draft.isBlank()) "Create draft" else "Rebuild draft")
            }

            if (ui.draft.isNotBlank()) {
                OutlinedTextField(
                    value = ui.draft,
                    onValueChange = vm::updateDraft,
                    label = { Text("Review and edit your bio") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth().testTag("bio_draft")
                )
                Button(
                    onClick = vm::saveDraft,
                    enabled = !ui.saving && ui.draft.trim().length >= 20,
                    modifier = Modifier.fillMaxWidth().testTag("bio_save")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.saving) "Saving…" else "Save to profile")
                }
            }

            ui.message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
