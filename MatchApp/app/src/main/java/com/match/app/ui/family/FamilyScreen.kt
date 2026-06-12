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
import androidx.compose.ui.graphics.Color
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
    val familyType: String = "Nuclear",       // Nuclear / Joint
    val familyStatus: String = "Middle Class", // Affluent / Middle Class / Upper Middle Class
    val familyValues: String = "Moderate",     // Orthodox / Traditional / Moderate / Liberal
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
        // In a full implementation, load from DB. Here we pre-fill with defaults.
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val p = auth.currentProfile(uid) ?: return@launch
            _info.value = FamilyInfo(
                fatherOccupation = p.fatherOccupation,
                motherOccupation = p.motherOccupation,
                siblings = p.siblings,
                familyType = p.familyType.ifBlank { "Nuclear" },
                nativePlace = p.city,
                gotra = p.gothra
            )
        }
    }

    fun update(info: FamilyInfo) { _info.value = info }
    fun save() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val info = _info.value
        auth.updateFamilyDetails(
            userId = uid,
            fatherOccupation = info.fatherOccupation,
            motherOccupation = info.motherOccupation,
            siblings = info.siblings,
            familyType = info.familyType,
            familyValues = info.familyValues,
            nativePlace = info.nativePlace,
            gotra = info.gotra,
            aboutFamily = info.aboutFamily
        )
        isSaved.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit = {},
    vm: FamilyViewModel = hiltViewModel()
) {
    val info by vm.info.collectAsState()
    val saved by vm.isSaved.collectAsState()

    var father by remember(info) { mutableStateOf(info.fatherOccupation) }
    var mother by remember(info) { mutableStateOf(info.motherOccupation) }
    var siblings by remember(info) { mutableStateOf(info.siblings.toString()) }
    var familyType by remember(info) { mutableStateOf(info.familyType) }
    var familyStatus by remember(info) { mutableStateOf(info.familyStatus) }
    var familyValues by remember(info) { mutableStateOf(info.familyValues) }
    var nativePlace by remember(info) { mutableStateOf(info.nativePlace) }
    var gotra by remember(info) { mutableStateOf(info.gotra) }
    var aboutFamily by remember(info) { mutableStateOf(info.aboutFamily) }
    var familyIncome by remember { mutableStateOf("") }
    var propertyDetails by remember { mutableStateOf("") }
    var brothersMarried by remember { mutableStateOf("") }
    var sistersMarried by remember { mutableStateOf("") }

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbar.showSnackbar("Family details saved!")
            vm.isSaved.value = false
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
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("family_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Info banner ──────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sharing family details helps matches understand your background and build trust.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ── Parents ──────────────────────────────────────────────────
            SectionHeader(Icons.Filled.Group, "Parents & Siblings")

            OutlinedTextField(
                value = father, onValueChange = { father = it },
                label = { Text("Father's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_father"),
                singleLine = true
            )
            OutlinedTextField(
                value = mother, onValueChange = { mother = it },
                label = { Text("Mother's occupation") },
                leadingIcon = { Icon(Icons.Filled.Work, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_mother"),
                singleLine = true
            )
            OutlinedTextField(
                value = siblings, onValueChange = { siblings = it },
                label = { Text("Number of siblings") },
                leadingIcon = { Icon(Icons.Filled.People, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_siblings"),
                singleLine = true
            )

            // ── Family type ────────────────────────────────────────────
            SectionHeader(Icons.Filled.Home, "Family Background")

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

            // ── Origin ────────────────────────────────────────────────
            SectionHeader(Icons.Filled.Place, "Origin & Tradition")

            OutlinedTextField(
                value = nativePlace, onValueChange = { nativePlace = it },
                label = { Text("Native place") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_native"),
                singleLine = true
            )
            OutlinedTextField(
                value = gotra, onValueChange = { gotra = it },
                label = { Text("Gotra (optional)") },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_gotra"),
                singleLine = true
            )

            // ── Siblings detail ──────────────────────────────────────────
            SectionHeader(Icons.Filled.People, "Siblings Detail")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = brothersMarried, onValueChange = { brothersMarried = it },
                    label = { Text("Brothers married") },
                    modifier = Modifier.weight(1f).testTag("family_bros_married"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sistersMarried, onValueChange = { sistersMarried = it },
                    label = { Text("Sisters married") },
                    modifier = Modifier.weight(1f).testTag("family_sis_married"),
                    singleLine = true
                )
            }

            // ── Family income & property ────────────────────────────────
            SectionHeader(Icons.Filled.AccountBalance, "Family Finances")

            Text(t("family_annual_income", "Family annual income"), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("< 5L", "5-10L", "10-25L", "25-50L", "50L-1Cr", "1Cr+").forEach { band ->
                    FilterChip(
                        selected = familyIncome == band,
                        onClick = { familyIncome = band },
                        label = { Text(band, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = propertyDetails, onValueChange = { propertyDetails = it },
                label = { Text("Property / assets (optional)") },
                leadingIcon = { Icon(Icons.Filled.HomeWork, null) },
                modifier = Modifier.fillMaxWidth().testTag("family_property"),
                singleLine = true
            )

            // ── About ─────────────────────────────────────────────────
            SectionHeader(Icons.Filled.Description, "About Family")

            OutlinedTextField(
                value = aboutFamily, onValueChange = { aboutFamily = it },
                label = { Text("Describe your family (optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp).testTag("family_about"),
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.update(
                        FamilyInfo(
                            fatherOccupation = father, motherOccupation = mother,
                            siblings = siblings.toIntOrNull() ?: 0,
                            familyType = familyType, familyStatus = familyStatus,
                            familyValues = familyValues, nativePlace = nativePlace,
                            gotra = gotra, aboutFamily = aboutFamily
                        )
                    )
                    vm.save()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("family_save")
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(t("save_family_details", "Save family details"))
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
private fun ChipGroup(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}
