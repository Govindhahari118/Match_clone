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
import com.match.app.ui.components.MatreeHero
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign

private data class OnboardPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val tag: String
)

private val PAGES = listOf(
    OnboardPage(
        icon = Icons.Filled.Favorite,
        title = "Welcome to Matree",
        body = "Build a genuine matrimony profile, set your preferences and discover eligible people with clear privacy controls.",
        bullets = listOf("Real profile and activity states", "Granular verification signals", "Privacy and safety controls"),
        tag = "ob_welcome"
    ),
    OnboardPage(
        icon = Icons.Filled.Groups,
        title = "Community Matching",
        body = "Search across supported Indian religions, communities, languages and locations using the preferences that matter to you.",
        bullets = listOf("Religion & community filters where applicable", "Mother tongue preferences", "Regional match discovery"),
        tag = "ob_community"
    ),
    OnboardPage(
        icon = Icons.AutoMirrored.Filled.ListAlt,
        title = "Compatibility Quiz",
        body = "Answer 15 questions about yourself and your ideal partner. Our algorithm finds your best matches.",
        bullets = listOf("Personality-driven matching", "Values & lifestyle alignment", "Partner preferences"),
        tag = "ob_quiz"
    ),
    OnboardPage(
        icon = Icons.Filled.Tune,
        title = "Your Preferences, Your Control",
        body = "Set mandatory criteria separately from preferences so discovery can respect what truly matters to you.",
        bullets = listOf("Mandatory filters stay strict", "Preferences influence ordering", "No silent filter relaxation"),
        tag = "ob_preferences"
    ),
    OnboardPage(
        icon = Icons.Filled.VerifiedUser,
        title = "Safe & Trusted",
        body = "Use verification signals, block/report controls and privacy settings to make informed decisions while connecting.",
        bullets = listOf("Verification status shown separately", "Identity verification where completed", "Block, report and privacy controls"),
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
                    .padding(MatreeDesign.spacing.xxl)
                    .testTag(p.tag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(130.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(p.icon, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(MatreeDesign.spacing.xl))
                Text(
                    p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MatreeDesign.spacing.sm))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MatreeDesign.spacing.lg))
                // Bullet points
                Column(
                    Modifier
                        .clip(RoundedCornerShape(MatreeDesign.radii.card))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(MatreeDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    p.bullets.forEach { bullet ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(MatreeDesign.spacing.xs)) {}
                            Text(bullet, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // ── Page indicator dots ────────────────────────────────────────
        Row(Modifier.padding(bottom = MatreeDesign.spacing.sm), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(PAGES.size) { idx ->
                Box(
                    Modifier
                        .size(if (idx == pager.currentPage) 28.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(MatreeDesign.radii.small))
                        .background(
                            if (idx == pager.currentPage) MaterialTheme.colorScheme.primary
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
            modifier = Modifier.padding(bottom = MatreeDesign.spacing.xs)
        )

        // Navigation
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MatreeDesign.spacing.xl, vertical = MatreeDesign.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
        ) {
            if (!isLast) {
                MatreeSecondaryButton(
                    text = t("skip", "Skip"),
                    onClick = { vm.complete(); onDone() },
                    modifier = Modifier.weight(1f).testTag("ob_skip")
                )
            }
            MatreePrimaryButton(
                text = if (isLast) t("get_started", "Get started") else t("next", "Next"),
                onClick = {
                    if (isLast) {
                        vm.complete()
                        onDone()
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
                modifier = Modifier.weight(if (isLast) 2f else 1f).testTag("ob_next")
            )
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
        MatreeHero(
            title = "Choose your language",
            subtitle = "You can change this anytime in Settings",
            modifier = Modifier.padding(MatreeDesign.spacing.md)
        )

        // Language grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).padding(
                horizontal = MatreeDesign.spacing.sm,
                vertical = MatreeDesign.spacing.xs
            ),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
        ) {
            items(ONBOARD_LANGUAGES) { lang ->
                val selected = lang.code == selectedCode
                Surface(
                    shape = RoundedCornerShape(MatreeDesign.radii.card),
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
                        Modifier.padding(MatreeDesign.spacing.sm),
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

        MatreePrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MatreeDesign.spacing.xl, vertical = MatreeDesign.spacing.md)
                .testTag("ob_lang_continue")
        )
    }
}

