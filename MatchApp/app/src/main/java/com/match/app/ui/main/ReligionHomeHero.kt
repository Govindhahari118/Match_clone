package com.match.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ReligionExperiencePreference
import com.match.app.ui.theme.AppPalette
import com.match.app.ui.theme.MatreeDesign
import com.match.app.ui.theme.colorSchemeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReligionHomeViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    val preference = session.religionExperience.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReligionExperiencePreference()
    )
    val currentFilter = session.filter.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        MatchFilter()
    )

    fun open(category: ReligionCategory, navigate: () -> Unit) = viewModelScope.launch {
        session.setFilter(currentFilter.value.copy(religion = category.label))
        navigate()
    }
}

private data class ReligionCardStyle(
    val category: ReligionCategory,
    val icon: ImageVector,
    val palette: AppPalette,
    val subtitle: String
)

private val religionStyles = mapOf(
    ReligionCategory.HINDU to ReligionCardStyle(
        ReligionCategory.HINDU, Icons.Filled.TempleHindu, AppPalette.HINDU,
        "Community, caste and astrology preferences"
    ),
    ReligionCategory.CHRISTIAN to ReligionCardStyle(
        ReligionCategory.CHRISTIAN, Icons.Filled.Church, AppPalette.CHRISTIAN,
        "Denomination, values and family preferences"
    ),
    ReligionCategory.MUSLIM to ReligionCardStyle(
        ReligionCategory.MUSLIM, Icons.Filled.Mosque, AppPalette.MUSLIM,
        "Community, values and family preferences"
    ),
    ReligionCategory.SIKH to ReligionCardStyle(
        ReligionCategory.SIKH, Icons.Filled.Diversity3, AppPalette.SIKH,
        "Community, values and family preferences"
    ),
    ReligionCategory.BUDDHIST to ReligionCardStyle(
        ReligionCategory.BUDDHIST, Icons.Filled.Diversity3, AppPalette.BUDDHIST,
        "Tradition, values and family preferences"
    ),
    ReligionCategory.JAIN to ReligionCardStyle(
        ReligionCategory.JAIN, Icons.Filled.Diversity3, AppPalette.JAIN,
        "Community, values and family preferences"
    ),
    ReligionCategory.PARSI to ReligionCardStyle(
        ReligionCategory.PARSI, Icons.Filled.Diversity3, AppPalette.PARSI,
        "Community, values and family preferences"
    ),
    ReligionCategory.OTHER to ReligionCardStyle(
        ReligionCategory.OTHER, Icons.Filled.Diversity3, AppPalette.VIVAH,
        "Explore compatible communities with flexibility"
    )
)

private fun religionStyle(category: ReligionCategory): ReligionCardStyle =
    religionStyles[category] ?: religionStyles.getValue(ReligionCategory.OTHER)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReligionHomeHero(
    profileReligion: String,
    onOpenMatches: () -> Unit,
    vm: ReligionHomeViewModel = hiltViewModel()
) {
    val preference by vm.preference.collectAsState()
    val effective = preference.effective(profileReligion)
    val profileCategory = ReligionCategory.fromReligion(profileReligion)
    val primary = if (preference.locked) profileCategory else effective.singleOrNull()
    val spacing = MatreeDesign.spacing

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Find your life partner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (preference.locked) "Your ${profileCategory.label} discovery is locked in"
                    else "Choose a community lens. You can broaden or narrow it anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (preference.locked) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Religion discovery locked",
                        modifier = Modifier.padding(spacing.xs).size(MatreeDesign.sizes.iconSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (primary != null) {
            val style = religionStyle(primary)
            LargeReligionCard(style) { vm.open(style.category, onOpenMatches) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                ReligionCategory.entries.filter { it != primary }.forEach { category ->
                    val alternative = religionStyle(category)
                    Surface(
                        modifier = Modifier.clickable(enabled = !preference.locked) {
                            vm.open(category, onOpenMatches)
                        },
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                alternative.icon,
                                contentDescription = null,
                                modifier = Modifier.size(MatreeDesign.sizes.iconSmall),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(spacing.xs))
                            Text(alternative.category.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            val visible = if (effective.isEmpty()) ReligionCategory.entries.toSet() else effective
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ReligionCategory.entries
                    .filter { it in visible }
                    .map(::religionStyle)
                    .chunked(2)
                    .forEach { rowStyles ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            rowStyles.forEach { style ->
                                CompactReligionCard(style, Modifier.weight(1f)) {
                                    vm.open(style.category, onOpenMatches)
                                }
                            }
                            if (rowStyles.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
            }
        }

        Surface(
            shape = RoundedCornerShape(MatreeDesign.radii.medium),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.size(spacing.xs))
                Text(
                    if (primary == ReligionCategory.HINDU) {
                        "Special Home can use applicable Rasi, Nakshatra and Kundali preferences when you choose them."
                    } else {
                        "Special Home focuses on partner preferences, values and lifestyle compatibility for your selected community."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LargeReligionCard(style: ReligionCardStyle, onClick: () -> Unit) {
    val preview = colorSchemeFor(style.palette, dark = false)
    val spacing = MatreeDesign.spacing
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(MatreeDesign.sizes.avatarHero + MatreeDesign.spacing.display + MatreeDesign.spacing.xxl),
        onClick = onClick,
        shape = RoundedCornerShape(MatreeDesign.radii.hero),
        colors = CardDefaults.cardColors(containerColor = preview.surface.copy(alpha = 0f))
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(preview.primary, preview.primary.copy(alpha = 0.78f))))
                .padding(spacing.xl)
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                modifier = Modifier.size(MatreeDesign.sizes.avatarCompact),
                tint = preview.onPrimary
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    "${style.category.label} Matches",
                    color = preview.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(style.subtitle, color = preview.onPrimary.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Open ${style.category.label} matches",
                modifier = Modifier.align(Alignment.BottomEnd),
                tint = preview.onPrimary
            )
        }
    }
}

@Composable
private fun CompactReligionCard(
    style: ReligionCardStyle,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val preview = colorSchemeFor(style.palette, dark = false)
    val spacing = MatreeDesign.spacing
    Card(
        modifier = modifier.height(MatreeDesign.sizes.avatarHero + MatreeDesign.spacing.display),
        onClick = onClick,
        shape = RoundedCornerShape(MatreeDesign.radii.large),
        colors = CardDefaults.cardColors(containerColor = preview.surface.copy(alpha = 0f))
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(preview.primary, preview.primary.copy(alpha = 0.78f))))
                .padding(spacing.md)
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                modifier = Modifier.size(MatreeDesign.sizes.iconLarge),
                tint = preview.onPrimary
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    style.category.label,
                    color = preview.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Explore matches", color = preview.onPrimary.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
