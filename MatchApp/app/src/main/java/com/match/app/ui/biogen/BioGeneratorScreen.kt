package com.match.app.ui.biogen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BioDraftSeed(
    val name: String = "",
    val age: String = "",
    val profession: String = "",
    val religionCommunity: String = "",
    val education: String = "",
    val hobbies: String = "",
    val values: String = ""
)

@HiltViewModel
class BioGeneratorViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val _seed = MutableStateFlow<BioDraftSeed?>(null)
    val seed = _seed.asStateFlow()

    var saved = mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val profile = auth.currentProfile(uid) ?: run {
                _seed.value = BioDraftSeed()
                return@launch
            }
            _seed.value = BioDraftSeed(
                name = profile.displayName,
                age = profile.age.takeIf { it > 0 }?.toString().orEmpty(),
                profession = profile.profession,
                religionCommunity = listOf(profile.religion, profile.caste)
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                education = listOf(profile.education, profile.institution)
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                hobbies = profile.hobbies.joinToString(", "),
                values = profile.familyValues
            )
        }
    }

    fun saveToProfile(bio: String) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        auth.updateBio(uid, bio.trim())
        saved.value = true
    }

    fun onDraftChanged() {
        saved.value = false
    }
}

private enum class BioTone(val label: String, val emoji: String) {
    Traditional("Traditional", "🙏"),
    Modern("Modern", "✨"),
    Professional("Professional", "💼"),
    Warm("Warm", "😊")
}

private enum class BioLength(val label: String, val words: Int) {
    Short("Short", 45), Medium("Medium", 85), Long("Long", 140)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BioGeneratorScreen(
    onBack: () -> Unit = {},
    vm: BioGeneratorViewModel = hiltViewModel()
) {
    val seed by vm.seed.collectAsState()
    var initialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var hobbies by remember { mutableStateOf("") }
    var values by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(BioTone.Modern) }
    var length by remember { mutableStateOf(BioLength.Medium) }
    var generatedBio by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(seed) {
        val value = seed ?: return@LaunchedEffect
        if (!initialized) {
            name = value.name
            age = value.age
            profession = value.profession
            religion = value.religionCommunity
            education = value.education
            hobbies = value.hobbies
            values = value.values
            initialized = true
        }
    }

    val hasUsefulInput = listOf(name, profession, education, hobbies, values, religion)
        .any { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("bio_generator", "Bio Generator"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("biogen_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("bio_generator_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            t("bio_generator_title", "Profile bio assistant"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "Your saved profile details are used when available. Review every sentence before saving it to your profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (seed == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Text(t("your_details", "Your details"), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; vm.onDraftChanged() },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.all(Char::isDigit)) age = it.take(3); vm.onDraftChanged() },
                    label = { Text("Age") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = religion,
                    onValueChange = { religion = it; vm.onDraftChanged() },
                    label = { Text("Religion / community (optional)") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = profession,
                onValueChange = { profession = it; vm.onDraftChanged() },
                label = { Text("Profession") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = education,
                onValueChange = { education = it; vm.onDraftChanged() },
                label = { Text("Education") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = hobbies,
                onValueChange = { hobbies = it; vm.onDraftChanged() },
                label = { Text("Hobbies") },
                supportingText = { Text("Separate multiple interests with commas") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = values,
                onValueChange = { values = it; vm.onDraftChanged() },
                label = { Text("Values") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(t("tone", "Tone"), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BioTone.entries.forEach { option ->
                    FilterChip(
                        selected = tone == option,
                        onClick = { tone = option; vm.onDraftChanged() },
                        label = { Text("${option.emoji} ${option.label}") }
                    )
                }
            }

            Text(t("length", "Length"), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BioLength.entries.forEach { option ->
                    FilterChip(
                        selected = length == option,
                        onClick = { length = option; vm.onDraftChanged() },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = {
                    generatedBio = composeBio(
                        name = name,
                        age = age,
                        profession = profession,
                        religion = religion,
                        education = education,
                        hobbies = hobbies,
                        values = values,
                        tone = tone,
                        length = length
                    )
                    vm.onDraftChanged()
                },
                enabled = hasUsefulInput,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("biogen_btn"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (generatedBio.isBlank()) "Create draft" else "Create another draft")
            }

            if (!hasUsefulInput) {
                Text(
                    "Add at least one real profile detail before creating a bio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (generatedBio.isNotBlank()) {
                ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Draft — review before saving",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(generatedBio, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { clipboard.setText(AnnotatedString(generatedBio)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = { vm.saveToProfile(generatedBio) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (vm.saved.value) "Saved" else "Use in profile")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun composeBio(
    name: String,
    age: String,
    profession: String,
    religion: String,
    education: String,
    hobbies: String,
    values: String,
    tone: BioTone,
    length: BioLength
): String {
    val cleanName = name.trim()
    val cleanAge = age.trim()
    val cleanProfession = profession.trim()
    val cleanReligion = religion.trim()
    val cleanEducation = education.trim()
    val hobbiesList = hobbies.split(",").map(String::trim).filter(String::isNotBlank)
    val valuesList = values.split(",").map(String::trim).filter(String::isNotBlank)

    val introduction = when (tone) {
        BioTone.Traditional -> if (cleanName.isNotBlank()) "With my family's blessings, I am $cleanName." else "I value family, respect and meaningful companionship."
        BioTone.Modern -> if (cleanName.isNotBlank()) "Hi, I'm $cleanName." else "Hello! Here's a little about me."
        BioTone.Professional -> if (cleanName.isNotBlank()) "I'm $cleanName." else "A little about my background and goals."
        BioTone.Warm -> if (cleanName.isNotBlank()) "Hi 👋 I'm $cleanName." else "Hi 👋 Thanks for reading my profile."
    }

    val facts = buildList {
        if (cleanAge.isNotBlank()) add("I'm $cleanAge years old")
        if (cleanProfession.isNotBlank()) add("I work as $cleanProfession")
        if (cleanEducation.isNotBlank()) add("my education is $cleanEducation")
        if (cleanReligion.isNotBlank()) add("I identify with $cleanReligion")
    }
    val factsSentence = if (facts.isEmpty()) "" else facts.joinToString(", ").replaceFirstChar { it.uppercase() } + "."

    val interestsSentence = if (hobbiesList.isEmpty()) "" else
        "Outside work, I enjoy ${hobbiesList.joinToString(", ")} .".replace(" ,", ",").replace(" .", ".")
    val valuesSentence = if (valuesList.isEmpty()) "" else
        "I value ${valuesList.joinToString(", ")} in everyday life and relationships."

    val closing = when (tone) {
        BioTone.Traditional -> "I hope to build a respectful partnership where both people and families are valued."
        BioTone.Modern -> "I'm looking for a kind, genuine partner with whom I can build a balanced future."
        BioTone.Professional -> "I value mutual respect, clear communication and a partnership that supports both people's goals."
        BioTone.Warm -> "I'm here to meet someone genuine, communicate openly and see whether we can build something meaningful together."
    }

    val full = listOf(introduction, factsSentence, interestsSentence, valuesSentence, closing)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val words = full.split(" ").filter { it.isNotBlank() }
    return if (words.size <= length.words) full else words.take(length.words).joinToString(" ") + "…"
}
