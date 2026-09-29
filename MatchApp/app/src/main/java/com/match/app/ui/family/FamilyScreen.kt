package com.match.app.ui.family

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyInfo(
    val fatherOccupation: String = "",
    val motherOccupation: String = "",
    val siblings: Int = 0,
    val familyType: String = "Nuclear",
    val familyStatus: String = "Middle Class",
    val familyValues: String = "Moderate",
    val nativeState: String = "",
    val gotra: String = "",
    val aboutFamily: String = ""
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val _info = MutableStateFlow(FamilyInfo())
    val info: StateFlow<FamilyInfo> = _info.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first()
            val profile = uid?.let { auth.currentProfile(it) }
            _info.value = if (profile == null) {
                FamilyInfo()
            } else {
                FamilyInfo(
                    fatherOccupation = profile.fatherOccupation,
                    motherOccupation = profile.motherOccupation,
                    siblings = profile.siblings.coerceIn(0, 20),
                    familyType = profile.familyType.ifBlank { "Nuclear" },
                    familyStatus = profile.familyStatus.ifBlank { "Middle Class" },
                    familyValues = profile.familyValues.ifBlank { "Moderate" },
                    nativeState = profile.nativeState,
                    gotra = profile.gothra,
                    aboutFamily = profile.aboutFamily
                )
            }
            _loading.value = false
        }
    }

    fun update(info: FamilyInfo) {
        _info.value = info
    }

    fun save() = viewModelScope.launch {
        val uid = session.userId.first()
        if (uid == null) {
            _message.value = "Sign in to save family details."
            return@launch
        }
        val value = _info.value
        if (value.siblings !in 0..20) {
            _message.value = "Enter a valid number of siblings from 0 to 20."
            return@launch
        }

        _saving.value = true
        auth.updateFamilyDetails(
            userId = uid,
            fatherOccupation = value.fatherOccupation,
            motherOccupation = value.motherOccupation,
            siblings = value.siblings,
            familyType = value.familyType,
            familyStatus = value.familyStatus,
            familyValues = value.familyValues,
            nativeState = value.nativeState,
            gotra = value.gotra,
            aboutFamily = value.aboutFamily
        ).onSuccess {
            _message.value = "Family details saved to your profile."
        }.onFailure {
            _message.value = it.message?.take(180) ?: "Could not save family details. Please retry."
        }
        _saving.value = false
    }

    fun consumeMessage() {
        _message.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit = {},
    vm: FamilyViewModel = hiltViewModel()
) {
    val info by vm.info.collectAsState()
    val loading by vm.loading.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()

    var father by remember(info) { mutableStateOf(info.fatherOccupation) }
    var mother by remember(info) { mutableStateOf(info.motherOccupation) }
    var siblings by remember(info) { mutableStateOf(info.siblings.toString()) }
    var familyType by remember(info) { mutableStateOf(info.familyType) }
    var familyStatus by remember(info) { mutableStateOf(info.familyStatus) }
    var familyValues by remember(info) { mutableStateOf(info.familyValues) }
    var nativeState by remember(info) { mutableStateOf(info.nativeState) }
    var gotra by remember(info) { mutableStateOf(info.gotra) }
    var aboutFamily by remember(info) { mutableStateOf(info.aboutFamily) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("family_details", "Family Details")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("family_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        if (loading) {
            Box(
                Modifier.padding(pad).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("family_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Share only the family background you want visible on your matrimonial profile. Every field on this screen is persisted; Matree does not show decorative fields that are silently discarded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            SectionHeader(Icons.Filled.Group, "Parents & siblings")
            OutlinedTextField(
                value = father,
                onValueChange = { father = it.take(120) },
                label = { Text("Father's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_father"),
                singleLine = true
            )
            OutlinedTextField(
                value = mother,
                onValueChange = { mother = it.take(120) },
                label = { Text("Mother's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_mother"),
                singleLine = true
            )
            OutlinedTextField(
                value = siblings,
                onValueChange = { input ->
                    if (input.isBlank() || input.all(Char::isDigit)) siblings = input.take(2)
                },
                label = { Text("Number of siblings") },
                supportingText = { Text("0 to 20") },
                leadingIcon = { Icon(Icons.Filled.People, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_siblings"),
                singleLine = true
            )

            SectionHeader(Icons.Filled.Home, "Family background")
            ChipGroup(
                label = "Family type",
                options = listOf("Nuclear", "Joint"),
                selected = familyType,
                onSelect = { familyType = it }
            )
            ChipGroup(
                label = "Family status",
                options = listOf("Middle Class", "Upper Middle Class", "Affluent"),
                selected = familyStatus,
                onSelect = { familyStatus = it }
            )
            ChipGroup(
                label = "Family values",
                options = listOf("Orthodox", "Traditional", "Moderate", "Liberal"),
                selected = familyValues,
                onSelect = { familyValues = it }
            )

            SectionHeader(Icons.Filled.Place, "Origin & tradition")
            OutlinedTextField(
                value = nativeState,
                onValueChange = { nativeState = it.take(100) },
                label = { Text("Native state / region") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_native_state"),
                singleLine = true
            )
            OutlinedTextField(
                value = gotra,
                onValueChange = { gotra = it.take(100) },
                label = { Text("Gotra (optional)") },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_gotra"),
                singleLine = true
            )

            SectionHeader(Icons.Filled.Description, "About family")
            OutlinedTextField(
                value = aboutFamily,
                onValueChange = { aboutFamily = it.take(1000) },
                label = { Text("Describe your family (optional)") },
                supportingText = { Text("${aboutFamily.length}/1000") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("family_about"),
                minLines = 4,
                maxLines = 8
            )

            Button(
                onClick = {
                    vm.update(
                        FamilyInfo(
                            fatherOccupation = father,
                            motherOccupation = mother,
                            siblings = siblings.toIntOrNull() ?: 0,
                            familyType = familyType,
                            familyStatus = familyStatus,
                            familyValues = familyValues,
                            nativeState = nativeState,
                            gotra = gotra,
                            aboutFamily = aboutFamily
                        )
                    )
                    vm.save()
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("family_save")
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Save, null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (saving) "Saving…" else t("save_family_details", "Save family details"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}
