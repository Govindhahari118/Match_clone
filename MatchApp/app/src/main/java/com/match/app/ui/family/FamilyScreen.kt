package com.match.app.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Work
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
    val familyType: String = "",
    val familyValues: String = "",
    val nativePlace: String = "",
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
    val isSaved = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val p = auth.currentProfile(uid) ?: return@launch
            _info.value = FamilyInfo(
                fatherOccupation = p.fatherOccupation,
                motherOccupation = p.motherOccupation,
                siblings = p.siblings,
                familyType = p.familyType,
                familyValues = p.familyValues,
                nativePlace = p.nativeState.ifBlank { p.city },
                gotra = p.gothra,
                aboutFamily = p.aboutFamily
            )
        }
    }

    fun update(info: FamilyInfo) { _info.value = info }

    fun save() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val value = _info.value
        auth.updateFamilyDetails(
            userId = uid,
            fatherOccupation = value.fatherOccupation.trim(),
            motherOccupation = value.motherOccupation.trim(),
            siblings = value.siblings.coerceAtLeast(0),
            familyType = value.familyType,
            familyValues = value.familyValues,
            nativePlace = value.nativePlace.trim(),
            gotra = value.gotra.trim(),
            aboutFamily = value.aboutFamily.trim()
        )
        isSaved.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit = {},
    vm: FamilyViewModel = hiltViewModel()
) {
    val info by vm.info.collectAsState()
    val saved by vm.isSaved.collectAsState()

    var father by remember(info) { mutableStateOf(info.fatherOccupation) }
    var mother by remember(info) { mutableStateOf(info.motherOccupation) }
    var siblings by remember(info) { mutableStateOf(if (info.siblings > 0) info.siblings.toString() else "") }
    var familyType by remember(info) { mutableStateOf(info.familyType) }
    var familyValues by remember(info) { mutableStateOf(info.familyValues) }
    var nativePlace by remember(info) { mutableStateOf(info.nativePlace) }
    var gotra by remember(info) { mutableStateOf(info.gotra) }
    var aboutFamily by remember(info) { mutableStateOf(info.aboutFamily) }

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbar.showSnackbar("Family details saved")
            vm.isSaved.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("family_details", "Family Details"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("family_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("family_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Add only the family information you want represented on your profile. Nothing here is pre-filled as a personal fact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            SectionHeader(Icons.Filled.Group, "Parents & siblings")
            OutlinedTextField(
                value = father,
                onValueChange = { father = it },
                label = { Text("Father's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_father"),
                singleLine = true
            )
            OutlinedTextField(
                value = mother,
                onValueChange = { mother = it },
                label = { Text("Mother's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_mother"),
                singleLine = true
            )
            OutlinedTextField(
                value = siblings,
                onValueChange = { if (it.all(Char::isDigit)) siblings = it.take(2) },
                label = { Text("Number of siblings") },
                leadingIcon = { Icon(Icons.Filled.People, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_siblings"),
                singleLine = true
            )

            SectionHeader(Icons.Filled.Home, "Family background")
            ChoiceChips(
                label = "Family type",
                options = listOf("Nuclear", "Joint"),
                selected = familyType,
                onSelect = { familyType = it }
            )
            ChoiceChips(
                label = "Family values",
                options = listOf("Orthodox", "Traditional", "Moderate", "Liberal"),
                selected = familyValues,
                onSelect = { familyValues = it }
            )

            SectionHeader(Icons.Filled.Place, "Origin & tradition")
            OutlinedTextField(
                value = nativePlace,
                onValueChange = { nativePlace = it },
                label = { Text("Native place") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_native"),
                singleLine = true
            )
            OutlinedTextField(
                value = gotra,
                onValueChange = { gotra = it },
                label = { Text("Lineage / gotra (optional, if applicable)") },
                supportingText = { Text("Leave blank when this field is not part of your community tradition.") },
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
                maxLines = 7
            )

            Button(
                onClick = {
                    vm.update(
                        FamilyInfo(
                            fatherOccupation = father,
                            motherOccupation = mother,
                            siblings = siblings.toIntOrNull() ?: 0,
                            familyType = familyType,
                            familyValues = familyValues,
                            nativePlace = nativePlace,
                            gotra = gotra,
                            aboutFamily = aboutFamily
                        )
                    )
                    vm.save()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("family_save"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(t("save_family_details", "Save family details"))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}
