package com.match.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.match.app.ui.i18n.t

private data class OnboardPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val iconTint: Color,
    val bgColor: Color,
    val tag: String
)

private val PAGES = listOf(
    OnboardPage(
        icon = Icons.Filled.Favorite,
        title = "Welcome to Matree",
        body = "Build a genuine matrimony profile, set your preferences and discover eligible people with clear privacy controls.",
        bullets = listOf("Real profile and activity states", "Granular verification signals", "Privacy and safety controls"),
        iconTint = Color(0xFFE91E63),
        bgColor = Color(0xFFFCE4EC),
        tag = "ob_welcome"
    ),
    OnboardPage(
        icon = Icons.Filled.Groups,
        title = "Community Matching",
        body = "Search across supported Indian religions, communities, languages and locations using the preferences that matter to you.",
        bullets = listOf("Religion & caste filters", "Mother tongue preferences", "Regional match discovery"),
        iconTint = Color(0xFF7B1FA2),
        bgColor = Color(0xFFF3E5F5),
        tag = "ob_community"
    ),
    OnboardPage(
        icon = Icons.AutoMirrored.Filled.ListAlt,
        title = "Compatibility Quiz",
        body = "Answer 15 questions about yourself and your ideal partner. Our algorithm finds your best matches.",
        bullets = listOf("Personality-driven matching", "Values & lifestyle alignment", "Partner preferences"),
        iconTint = Color(0xFF1976D2),
        bgColor = Color(0xFFE3F2FD),
        tag = "ob_quiz"
    ),
    OnboardPage(
        icon = Icons.Filled.AutoAwesome,
        title = "Astrology Compatibility",
        body = "Rasi, Nakshatra and Gana — ancient wisdom meets modern matching for a deeper connection.",
        bullets = listOf("Kundli compatibility report", "Rasi & Nakshatra matching", "Gana compatibility"),
        iconTint = Color(0xFFFF8F00),
        bgColor = Color(0xFFFFF8E1),
        tag = "ob_astro"
    ),
    OnboardPage(
        icon = Icons.Filled.VerifiedUser,
        title = "Safe & Trusted",
        body = "Use verification signals, block/report controls and privacy settings to make informed decisions while connecting.",
        bullets = listOf("Verification status shown separately", "Identity verification where completed", "Block, report and privacy controls"),
        iconTint = Color(0xFF2E7D32),
        bgColor = Color(0xFFE8F5E9),
        tag = "ob_trust"
    )
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val session: SessionStore) : ViewModel() {
    fun complete() = viewModelScope.launch { session.setOnboarded(true) }
    fun setLanguage(code: String) = viewModelScope.launch { session.setUiLanguage(code) }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    // Step 0 is a dedicated language picker; steps 1..N are the regular onboarding pages
    var showLangPicker by remember { mutableStateOf(true) }
    val selectedLang = remember { mutableStateOf("en") }

    if (showLangPicker) {
        LanguagePickerPage(
            selectedCode = selectedLang.value,
            onSelect = { code ->
                selectedLang.value = code
                vm.setLanguage(code)
            },
            onContinue = { showLangPicker = false }
        )
        return
    }
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == PAGES.lastIndex
    val currentPage = PAGES[pager.currentPage]

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { idx ->
            val p = PAGES[idx]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag(p.tag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = p.bgColor,
                    modifier = Modifier.size(130.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(p.icon, null, Modifier.size(60.dp), tint = p.iconTint)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                // Bullet points
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.bgColor)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    p.bullets.forEach { bullet ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = CircleShape, color = p.iconTint, modifier = Modifier.size(6.dp)) {}
                            Text(bullet, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = p.iconTint)
                        }
                    }
                }
            }
        }

        // ── Page indicator dots ────────────────────────────────────────
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(PAGES.size) { idx ->
                Box(
                    Modifier
                        .size(if (idx == pager.currentPage) 28.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (idx == pager.currentPage) currentPage.iconTint
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                )
            }
        }

        // ── Page N of N label ──────────────────────────────────────────
        Text(
            "${pager.currentPage + 1} ${t("of", "of")} ${PAGES.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // ── Navigation buttons ────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isLast) {
                OutlinedButton(
                    onClick = { vm.complete(); onDone() },
                    modifier = Modifier.weight(1f).testTag("ob_skip")
                ) { Text(t("skip", "Skip")) }
            }
            Button(
                onClick = {
                    if (isLast) { vm.complete(); onDone() }
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.weight(if (isLast) 2f else 1f).testTag("ob_next"),
                colors = ButtonDefaults.buttonColors(containerColor = currentPage.iconTint)
            ) {
                Text(
                    if (isLast) t("get_started", "Get started") else t("next", "Next"),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Language picker page (shown before the main onboarding slides) ────────────
private data class LangOption(val code: String, val nativeName: String, val flag: String)

private val ONBOARD_LANGUAGES = listOf(
    LangOption("en", "English", "🇮🇳"),
    LangOption("hi", "हिन्दी", "🇮🇳"),
    LangOption("te", "తెలుగు", "🇮🇳"),
    LangOption("ta", "தமிழ்", "🇮🇳"),
    LangOption("kn", "ಕನ್ನಡ", "🇮🇳"),
    LangOption("mr", "मराठी", "🇮🇳"),
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LanguagePickerPage(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ob_lang_picker"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🌐", style = MaterialTheme.typography.displaySmall)
                Text("Choose your language", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Text("You can change this anytime in Settings", style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.8f), textAlign = TextAlign.Center)
            }
        }

        // Language grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ONBOARD_LANGUAGES) { lang ->
                val selected = lang.code == selectedCode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selected)
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang.code) }
                        .testTag("ob_lang_${lang.code}")
                ) {
                    Column(
                        Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(lang.flag, style = MaterialTheme.typography.titleMedium)
                        Text(lang.nativeName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Continue button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(52.dp)
                .testTag("ob_lang_continue"),
        ) {
            Text("Continue →", fontWeight = FontWeight.SemiBold)
        }
    }
}

