package com.match.app.ui.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
class QuizViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    fun persistPersonality(type: String) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        auth.updatePersonalityType(uid, type)
    }
}

// ── Quiz data ────────────────────────────────────────────────────────────────

private data class QuizQuestion(
    val id: Int,
    val question: String,
    val emoji: String,
    val category: String,
    val options: List<QuizOption>
)

private data class QuizOption(val label: String, val emoji: String, val weight: Int)

private val QUIZ_QUESTIONS = listOf(
    QuizQuestion(1, "How important is religion and daily rituals in your life?", "🛕",
        "Values",
        listOf(QuizOption("Very important — central to my identity", "🙏", 4),
               QuizOption("Important — I follow traditions", "✨", 3),
               QuizOption("Somewhat — festivals and special occasions", "🎉", 2),
               QuizOption("Not very important to me", "😊", 1))),
    QuizQuestion(2, "Where would you ideally want to live after marriage?", "🏠",
        "Lifestyle",
        listOf(QuizOption("Same city as parents / joint family", "👨‍👩‍👧‍👦", 4),
               QuizOption("Nuclear family but close to parents", "🏡", 3),
               QuizOption("Any city — open to relocation", "🌏", 2),
               QuizOption("Abroad / NRI life", "✈️", 1))),
    QuizQuestion(3, "How do you handle disagreements?", "💬",
        "Communication",
        listOf(QuizOption("Talk it out immediately and resolve", "🤝", 4),
               QuizOption("Take time to cool down, then discuss", "⏳", 3),
               QuizOption("Prefer written communication", "📝", 2),
               QuizOption("Need personal space — resolve slowly", "🌿", 1))),
    QuizQuestion(4, "What's your ideal weekend?", "🌅",
        "Lifestyle",
        listOf(QuizOption("Family time — relatives, gatherings", "👨‍👩‍👧", 4),
               QuizOption("Quiet time at home with partner", "🏠", 3),
               QuizOption("Exploring new places / travel", "🏔️", 2),
               QuizOption("Social events, parties, meeting friends", "🎊", 1))),
    QuizQuestion(5, "How important is career growth to you?", "💼",
        "Ambition",
        listOf(QuizOption("Top priority — I'm highly career-focused", "🚀", 4),
               QuizOption("Important but balanced with family", "⚖️", 3),
               QuizOption("Family first, career secondary", "❤️", 2),
               QuizOption("I value experience over ambition", "🌸", 1))),
    QuizQuestion(6, "What are your thoughts on children?", "👶",
        "Family",
        listOf(QuizOption("Want children soon after marriage", "🍼", 4),
               QuizOption("Want children — but after settling down", "🕐", 3),
               QuizOption("Open to children — flexible timeline", "💕", 2),
               QuizOption("Not sure / would rather adopt", "🌟", 1))),
    QuizQuestion(7, "How do you manage money in a relationship?", "💰",
        "Finance",
        listOf(QuizOption("Joint accounts — complete transparency", "🤝", 4),
               QuizOption("Shared expenses, separate savings", "💳", 3),
               QuizOption("One person manages finances", "📊", 2),
               QuizOption("Fully independent finances", "💼", 1))),
    QuizQuestion(8, "Which best describes your social circle preference?", "👥",
        "Social",
        listOf(QuizOption("Close-knit family and old friends only", "🏠", 4),
               QuizOption("Small circle of close friends + family", "💞", 3),
               QuizOption("Wide social network — love meeting people", "🌐", 2),
               QuizOption("Minimal social commitments — introvert", "📚", 1))),
    QuizQuestion(9, "How do you feel about astrology and kundli matching?", "⭐",
        "Values",
        listOf(QuizOption("Very important — must match before marriage", "🔮", 4),
               QuizOption("Important but not a dealbreaker", "✨", 3),
               QuizOption("Curious but won't make decisions based on it", "🤔", 2),
               QuizOption("I don't believe in astrology at all", "🎲", 1))),
    QuizQuestion(10, "What's your approach to personal independence in marriage?", "🦋",
        "Relationship",
        listOf(QuizOption("Very traditional — strong family roles", "🌹", 4),
               QuizOption("Modern but respect for traditions", "⚖️", 3),
               QuizOption("Equal partnership — shared everything", "💪", 2),
               QuizOption("Fully independent — minimal merging", "🦅", 1)))
)

private data class PersonalityResult(
    val type: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val compatibleWith: List<String>
)

private fun calcResult(answers: Map<Int, Int>): PersonalityResult {
    val avg = if (answers.isEmpty()) 2.5f else answers.values.average().toFloat()
    return when {
        avg >= 3.5f -> PersonalityResult(
            "Traditional Family-First",
            "🏠",
            "You deeply value family bonds, traditions, and cultural roots. You seek a partner who shares religious beliefs and family-oriented values.",
            Color(0xFF8B1A1A),
            listOf("Traditional Family-First", "Values-Driven Partner"))
        avg >= 2.5f -> PersonalityResult(
            "Balanced Modern",
            "⚖️",
            "You balance tradition and modernity beautifully. You value family while embracing personal growth and career. Most compatible with the widest range of partners.",
            Color(0xFF1565C0),
            listOf("Balanced Modern", "Ambitious Professional", "Traditional Family-First"))
        avg >= 1.5f -> PersonalityResult(
            "Ambitious Professional",
            "🚀",
            "Career and personal growth drive you. You seek an equal partner who is independent, ambitious, and understands your drive.",
            Color(0xFF2E7D32),
            listOf("Ambitious Professional", "Balanced Modern"))
        else -> PersonalityResult(
            "Free-Spirited Explorer",
            "🦋",
            "You value experiences, adventure, and personal freedom. You seek a partner who loves exploration and doesn't want a rigid life structure.",
            Color(0xFF6A1B9A),
            listOf("Free-Spirited Explorer", "Ambitious Professional"))
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CompatibilityQuizScreen(onBack: () -> Unit = {}, vm: QuizViewModel = hiltViewModel()) {
    var currentQuestion by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showResult by remember { mutableStateOf(false) }

    val progress = (currentQuestion.toFloat() / QUIZ_QUESTIONS.size).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!showResult) Text(t("compatibility_quiz", "Compatibility Quiz — ${currentQuestion + 1} of ${QUIZ_QUESTIONS.size}"))
                    else Text(t("your_personality_profile", "Your Personality Profile"))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showResult) showResult = false
                        else if (currentQuestion > 0) currentQuestion--
                        else onBack()
                    }, modifier = Modifier.testTag("quiz_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().testTag("compat_quiz_screen")
        ) {
            if (!showResult) {
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                    },
                    label = "quiz_question"
                ) { qIdx ->
                    val q = QUIZ_QUESTIONS[qIdx]
                    QuizQuestionContent(
                        question = q,
                        selectedWeight = answers[q.id],
                        onSelect = { weight ->
                            answers[q.id] = weight
                            if (currentQuestion < QUIZ_QUESTIONS.size - 1) {
                                currentQuestion++
                            } else {
                                showResult = true
                                val result = calcResult(answers)
                                vm.persistPersonality(result.type)
                            }
                        }
                    )
                }
            } else {
                QuizResultContent(
                    answers = answers,
                    onRetake = {
                        answers.clear()
                        currentQuestion = 0
                        showResult = false
                    },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
    question: QuizQuestion,
    selectedWeight: Int?,
    onSelect: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                "  ${question.category}  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        // Emoji + Question
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(question.emoji, style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(question.question,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(8.dp))

        // Options
        question.options.forEach { opt ->
            val isSelected = selectedWeight == opt.weight
            ElevatedCard(
                onClick = { onSelect(opt.weight) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("quiz_opt_${question.id}_${opt.weight}"),
                colors = if (isSelected)
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                else CardDefaults.elevatedCardColors(),
                elevation = if (isSelected) CardDefaults.elevatedCardElevation(8.dp)
                            else CardDefaults.elevatedCardElevation(2.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(opt.emoji, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(12.dp))
                    Text(opt.label, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface)
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultContent(
    answers: Map<Int, Int>,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    val result = remember(answers) { calcResult(answers) }
    val totalQuestions = QUIZ_QUESTIONS.size
    val answered = answers.size

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Result hero
        Surface(
            shape = CircleShape,
            color = result.color.copy(alpha = 0.1f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(result.emoji, style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center)
            }
        }

        Text(result.type,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = result.color,
            textAlign = TextAlign.Center)

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = result.color.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(result.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface)
        }

        // Score card
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t("questions_answered", "Questions answered"), style = MaterialTheme.typography.bodySmall)
                    Text("$answered / $totalQuestions",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { answered.toFloat() / totalQuestions },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = result.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Compatible types
        Text(t("most_compatible_with", "Most Compatible With"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth())
        result.compatibleWith.forEach { type ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    Text(type, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // Category breakdown
        Text(t("your_answers_by_category", "Your Answers by Category"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth())
        val categoryScores = QUIZ_QUESTIONS
            .filter { answers.containsKey(it.id) }
            .groupBy { it.category }
            .mapValues { (_, qs) -> qs.map { q -> answers[q.id] ?: 2 }.average().toFloat() / 4f }
        categoryScores.forEach { (cat, score) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(cat, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp))
                LinearProgressIndicator(
                    progress = { score },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = result.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(36.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Insight tip
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TipsAndUpdates, null, Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Your personality profile is used in our AI matching to find partners most compatible with your values and lifestyle — beyond just age, caste, and location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(t("retake_quiz", "Retake Quiz"))
            }
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(t("find_matches", "Find Matches"))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
