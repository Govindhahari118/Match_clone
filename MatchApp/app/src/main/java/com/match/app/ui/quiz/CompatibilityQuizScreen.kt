package com.match.app.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
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
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    fun persistPersonality(type: String) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        auth.updatePersonalityType(uid, type)
    }
}

private data class QuizOption(val label: String, val weight: Int)
private data class QuizQuestion(
    val id: Int,
    val category: String,
    val question: String,
    val options: List<QuizOption>
)

private val QUESTIONS = listOf(
    QuizQuestion(
        1,
        "Family",
        "How involved would you like families to be in major decisions after marriage?",
        listOf(
            QuizOption("Very involved — important decisions should include family", 4),
            QuizOption("Involved for major milestones, with the couple deciding", 3),
            QuizOption("Supportive but mostly advisory", 2),
            QuizOption("Primarily the couple's decision", 1)
        )
    ),
    QuizQuestion(
        2,
        "Values",
        "What role should faith, culture or personal traditions have in married life?",
        listOf(
            QuizOption("Central to our shared daily life", 4),
            QuizOption("Important and regularly practiced", 3),
            QuizOption("Meaningful on selected occasions", 2),
            QuizOption("Personal choice; no shared practice required", 1)
        )
    ),
    QuizQuestion(
        3,
        "Communication",
        "When a disagreement becomes emotional, what works best for you?",
        listOf(
            QuizOption("Discuss it immediately and resolve it together", 4),
            QuizOption("Pause briefly, then return to the conversation", 3),
            QuizOption("Write or message first, then talk", 2),
            QuizOption("Take substantial personal space before discussing", 1)
        )
    ),
    QuizQuestion(
        4,
        "Career",
        "How should two careers be balanced after marriage?",
        listOf(
            QuizOption("Both careers should receive equal planning priority", 4),
            QuizOption("Balance careers with family needs case by case", 3),
            QuizOption("One career may take priority during key periods", 2),
            QuizOption("Career should remain secondary to home responsibilities", 1)
        )
    ),
    QuizQuestion(
        5,
        "Home",
        "Which living arrangement feels most comfortable long term?",
        listOf(
            QuizOption("Joint or closely connected family household", 4),
            QuizOption("Independent home near family", 3),
            QuizOption("Independent home in whichever city suits us", 2),
            QuizOption("Highly flexible — relocation or living abroad is welcome", 1)
        )
    ),
    QuizQuestion(
        6,
        "Finance",
        "Which approach to money feels most natural in a partnership?",
        listOf(
            QuizOption("Mostly shared finances and joint planning", 4),
            QuizOption("Shared household planning with individual savings", 3),
            QuizOption("Mostly separate finances with agreed shared expenses", 2),
            QuizOption("Strong financial independence for both partners", 1)
        )
    ),
    QuizQuestion(
        7,
        "Lifestyle",
        "How structured do you prefer everyday married life to be?",
        listOf(
            QuizOption("Predictable routines and clearly shared responsibilities", 4),
            QuizOption("Some routines with room for spontaneity", 3),
            QuizOption("Flexible schedules and changing plans are fine", 2),
            QuizOption("Very independent routines work best for me", 1)
        )
    ),
    QuizQuestion(
        8,
        "Independence",
        "How much personal independence should each partner maintain?",
        listOf(
            QuizOption("Most decisions and activities should be shared", 4),
            QuizOption("Close partnership with healthy individual space", 3),
            QuizOption("Significant independent interests and social time", 2),
            QuizOption("High independence with clear agreed boundaries", 1)
        )
    )
)

private data class ReflectionResult(val title: String, val description: String)

private fun reflectionResult(answers: Map<Int, Int>): ReflectionResult {
    val avg = answers.values.average().takeIf { !it.isNaN() } ?: 2.5
    return when {
        avg >= 3.35 -> ReflectionResult(
            "Shared & family-connected",
            "Your answers lean toward shared routines, close family involvement and coordinated decision-making."
        )
        avg >= 2.45 -> ReflectionResult(
            "Balanced partnership",
            "Your answers lean toward balancing shared commitments with personal choice and flexibility."
        )
        avg >= 1.65 -> ReflectionResult(
            "Independent partnership",
            "Your answers lean toward individual autonomy while keeping important decisions collaborative."
        )
        else -> ReflectionResult(
            "Highly independent",
            "Your answers place strong value on individual space, flexibility and clearly negotiated boundaries."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityQuizScreen(
    onBack: () -> Unit = {},
    vm: QuizViewModel = hiltViewModel()
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showResult by rememberSaveable { mutableStateOf(false) }

    val current = QUESTIONS[currentIndex]
    val progress = ((currentIndex + 1).toFloat() / QUESTIONS.size).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showResult) "Your values reflection"
                        else "Compatibility reflection ${currentIndex + 1}/${QUESTIONS.size}",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                showResult -> showResult = false
                                currentIndex > 0 -> currentIndex--
                                else -> onBack()
                            }
                        },
                        modifier = Modifier.testTag("quiz_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).testTag("compat_quiz_screen")
        ) {
            LinearProgressIndicator(
                progress = { if (showResult) 1f else progress },
                modifier = Modifier.fillMaxWidth()
            )

            if (showResult) {
                val result = remember(answers.toMap()) { reflectionResult(answers) }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                result.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                result.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "This is a self-reflection summary, not a scientific personality diagnosis or a prediction of relationship success. Matree should compare explicit preferences and real profile data separately.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            vm.persistPersonality(result.title)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("quiz_save")
                    ) {
                        Text("Save reflection")
                    }

                    OutlinedButton(
                        onClick = {
                            answers.clear()
                            currentIndex = 0
                            showResult = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retake")
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(current.category) },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) }
                    )

                    Text(
                        current.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    current.options.forEach { option ->
                        val selected = answers[current.id] == option.weight
                        ElevatedCard(
                            onClick = {
                                answers[current.id] = option.weight
                                if (currentIndex < QUESTIONS.lastIndex) {
                                    currentIndex++
                                } else {
                                    showResult = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("quiz_option_${current.id}_${option.weight}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(option.label, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
