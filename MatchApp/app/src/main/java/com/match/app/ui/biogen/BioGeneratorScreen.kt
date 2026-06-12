package com.match.app.ui.biogen

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
import androidx.compose.ui.platform.ClipboardManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BioGeneratorViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    var saved = mutableStateOf(false)
        private set

    fun saveToProfile(bio: String) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        auth.updateBio(uid, bio)
        saved.value = true
    }
}

private val ORANGE = Color(0xFFE65100)

private enum class BioTone(val label: String, val emoji: String) {
    Traditional("Traditional", "🕉"),
    Modern("Modern", "✨"),
    Professional("Professional", "💼"),
    Casual("Casual", "😊")
}
private enum class BioLength(val label: String, val words: Int) {
    Short("Short", 40), Medium("Medium", 80), Long("Long", 140)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioGeneratorScreen(onBack: () -> Unit = {}, vm: BioGeneratorViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("Priya") }
    var age by remember { mutableStateOf("28") }
    var profession by remember { mutableStateOf("Software Engineer") }
    var religion by remember { mutableStateOf("Hindu, Brahmin") }
    var education by remember { mutableStateOf("M.Tech, IIT Delhi") }
    var hobbies by remember { mutableStateOf("reading, classical music, yoga") }
    var values by remember { mutableStateOf("family, honesty, spirituality") }
    var tone by remember { mutableStateOf(BioTone.Modern) }
    var length by remember { mutableStateOf(BioLength.Medium) }
    var generatedBio by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("bio_generator", "Bio Generator")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("biogen_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = ORANGE.copy(alpha = 0.08f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = ORANGE)
                        Spacer(Modifier.width(8.dp))
                        Text(t("ai_bio_generator", "AI Bio Generator"),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = ORANGE)
                    }
                    Text(t("bio_generator_desc", "Generate a polished matrimonial bio from your profile in seconds."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(t("your_details", "Your details"), fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it },
                    label = { Text("Age") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = religion, onValueChange = { religion = it },
                    label = { Text("Religion / Community") }, modifier = Modifier.weight(2f))
            }
            OutlinedTextField(value = profession, onValueChange = { profession = it },
                label = { Text("Profession") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = education, onValueChange = { education = it },
                label = { Text("Education") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = hobbies, onValueChange = { hobbies = it },
                label = { Text("Hobbies (comma separated)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = values, onValueChange = { values = it },
                label = { Text("Values") }, modifier = Modifier.fillMaxWidth())

            Text(t("tone", "Tone"), fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                BioTone.values().forEach { t ->
                    FilterChip(
                        selected = tone == t,
                        onClick = { tone = t },
                        label = { Text("${t.emoji} ${t.label}") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(t("length", "Length"), fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                BioLength.values().forEach { l ->
                    FilterChip(
                        selected = length == l,
                        onClick = { length = l },
                        label = { Text(l.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = {
                    generatedBio = composeBio(name, age, profession, religion, education,
                        hobbies, values, tone, length)
                },
                modifier = Modifier.fillMaxWidth().testTag("biogen_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ORANGE)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (generatedBio.isBlank()) "Generate Bio" else "Regenerate")
            }

            if (generatedBio.isNotBlank()) {
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your bio",
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            color = ORANGE)
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
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ORANGE)
                            ) {
                                Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (vm.saved.value) "Saved!" else "Use in profile")
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
    name: String, age: String, profession: String, religion: String, education: String,
    hobbies: String, values: String, tone: BioTone, length: BioLength
): String {
    val opener = when (tone) {
        BioTone.Traditional -> listOf(
            "With blessings of my family, I am ${name.ifBlank { "a seeker" }}",
            "Namaste. My name is ${name.ifBlank { "..." }}",
            "By God's grace and my family's blessings, I, ${name.ifBlank { "..." }}"
        ).random()
        BioTone.Modern -> listOf(
            "Hi — I'm ${name.ifBlank { "someone" }}",
            "Hello! ${name.ifBlank { "I'm" }} here",
            "Hey, I'm ${name.ifBlank { "a dreamer" }}"
        ).random()
        BioTone.Professional -> listOf(
            "${name.ifBlank { "I am" }}, $age, a $profession",
            "I'm ${name.ifBlank { "a professional" }}, $age, working as a $profession"
        ).random()
        BioTone.Casual -> listOf(
            "Hey there! ${name.ifBlank { "I'm just someone" }}",
            "Yo! I'm ${name.ifBlank { "someone cool" }}, $age",
            "Hi 👋 I'm ${name.ifBlank { "me" }}"
        ).random()
    }
    val middle = when (tone) {
        BioTone.Traditional -> listOf(
            "belonging to a $religion family. I have completed $education and work as a $profession.",
            "from a respected $religion household. I hold a degree from $education and serve as a $profession.",
            "rooted in the $religion tradition. Educated at $education, I now work as $profession."
        ).random()
        BioTone.Modern -> listOf(
            "— $age, $profession with a degree from $education. Proud of my $religion roots.",
            "— $age, $profession, $education alumni. My $religion heritage means a lot to me.",
            "— $age years young, $profession by day, $religion traditions at heart."
        ).random()
        BioTone.Professional -> listOf(
            "by profession. Educated at $education. I belong to a $religion background.",
            ". My education at $education has shaped my career. I come from a $religion family."
        ).random()
        BioTone.Casual -> listOf(
            "— $age, $profession, $religion, $education alum.",
            "— $age, love my $profession life, $religion vibes, studied at $education."
        ).random()
    }
    val hobbiesList = hobbies.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val valuesList = values.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val hobbyPart = if (hobbiesList.isNotEmpty())
        listOf(
            "In my free time I enjoy ${hobbiesList.joinToString(", ")}.",
            "My passions include ${hobbiesList.joinToString(", ")}.",
            "When not working, you'll find me ${hobbiesList.joinToString(", ")}."
        ).random()
    else ""
    val valuesPart = if (valuesList.isNotEmpty())
        listOf(
            "What matters to me most: ${valuesList.joinToString(", ")}.",
            "I deeply value ${valuesList.joinToString(", ")}.",
            "Core to who I am: ${valuesList.joinToString(", ")}."
        ).random()
    else ""
    val closer = when (tone) {
        BioTone.Traditional -> listOf(
            "Looking for a life partner who shares our traditions and values family.",
            "Seeking a compatible match who respects our culture and family values.",
            "I pray for a partner who walks this path of life together with devotion and respect."
        ).random()
        BioTone.Modern -> listOf(
            "Looking for someone kind, ambitious, and genuine — let's grow together.",
            "If you value real connections and good conversations, let's talk!",
            "Searching for my person — someone who matches my energy and ambition."
        ).random()
        BioTone.Professional -> listOf(
            "Seeking a well-educated, career-oriented partner for a long-term partnership.",
            "Looking for an equally driven individual to build a meaningful future together.",
            "My ideal partner is intellectually curious and professionally accomplished."
        ).random()
        BioTone.Casual -> listOf(
            "If this sounds like your vibe — say hi! Let's chat.",
            "Swipe right if you like what you see 😊",
            "Let's grab coffee (or chai) and see where it goes!"
        ).random()
    }
    val full = "$opener $middle $hobbyPart $valuesPart $closer"
    val words = full.split(" ").filter { it.isNotBlank() }
    return if (words.size <= length.words) full.trim()
    else words.take(length.words).joinToString(" ") + "…"
}
