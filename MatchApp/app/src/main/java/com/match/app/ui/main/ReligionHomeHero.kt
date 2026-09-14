package com.match.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ReligionExperiencePreference
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

    /**
     * Selecting a religion applies it to the existing production MatchFilter,
     * so Home and Discover cannot drift into two different sources of truth.
     */
    fun open(category: ReligionCategory, current: MatchFilter, navigate: () -> Unit) = viewModelScope.launch {
        session.setFilter(current.copy(religion = category.label))
        navigate()
    }
}

private data class ReligionCardStyle(
    val category: ReligionCategory,
    val icon: ImageVector,
    val start: Color,
    val end: Color,
    val subtitle: String
)

private val religionStyles = listOf(
    ReligionCardStyle(
        ReligionCategory.HINDU,
        Icons.Filled.TempleHindu,
        Color(0xFFC76B00), Color(0xFF8C4A3A),
        "Community, caste & astrology preferences"
    ),
    ReligionCardStyle(
        ReligionCategory.CHRISTIAN,
        Icons.Filled.Church,
        Color(0xFF315E8A), Color(0xFF79536B),
        "Denomination, values & family preferences"
    ),
    ReligionCardStyle(
        ReligionCategory.MUSLIM,
        Icons.Filled.Mosque,
        Color(0xFF17705A), Color(0xFF78613A),
        "Community, values & family preferences"
    ),
    ReligionCardStyle(
        ReligionCategory.OTHER,
        Icons.Filled.Diversity3,
        Color(0xFF6D4C7D), Color(0xFF8B5E66),
        "Explore compatible communities with flexibility"
    )
)

/**
 * Home entry modeled after a category marketplace, but optimized for matrimony:
 * one locked lens becomes a large hero while unlocked multi-lens users see all
 * selected categories equally. Alternate communities remain reachable without
 * forcing them into the primary experience.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReligionHomeHero(
    profileReligion: String,
    currentFilter: MatchFilter,
    onOpenMatches: () -> Unit,
    vm: ReligionHomeViewModel = hiltViewModel()
) {
    val preference by vm.preference.collectAsState()
    val effective = preference.effective(profileReligion)
    val profileCategory = ReligionCategory.fromReligion(profileReligion)
    val primary = if (preference.locked) profileCategory else effective.singleOrNull()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(Icons.Filled.Lock, "Religion locked", Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (primary != null) {
            val style = religionStyles.first { it.category == primary }
            LargeReligionCard(style) {
                vm.open(style.category, currentFilter, onOpenMatches)
            }
            val alternates = ReligionCategory.entries.filter { it != primary }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alternates.forEach { category ->
                    val styleAlt = religionStyles.first { it.category == category }
                    Surface(
                        modifier = Modifier.clickable(enabled = !preference.locked) {
                            vm.open(category, currentFilter, onOpenMatches)
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(styleAlt.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(6.dp))
                            Text(styleAlt.category.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            val visible = if (effective.isEmpty()) ReligionCategory.entries.toSet() else effective
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                religionStyles.filter { it.category in visible }.chunked(2).forEach { rowStyles ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowStyles.forEach { style ->
                            CompactReligionCard(style, Modifier.weight(1f)) {
                                vm.open(style.category, currentFilter, onOpenMatches)
                            }
                        }
                        if (rowStyles.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (primary == ReligionCategory.HINDU) "Special Home can rank compatible profiles using Rasi, Nakshatra and Kundali preferences."
                    else "Special Home focuses on partner preferences, values and lifestyle compatibility for your selected community.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LargeReligionCard(style: ReligionCardStyle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier.fillMaxWidth().height(190.dp)
                .background(Brush.linearGradient(listOf(style.start, style.end)))
                .padding(22.dp)
        ) {
            Icon(style.icon, null, Modifier.size(54.dp), tint = Color.White.copy(alpha = 0.95f))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text("${style.category.label} Matches", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(style.subtitle, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ArrowForward, "Open", Modifier.align(Alignment.BottomEnd), tint = Color.White)
        }
    }
}

@Composable
private fun CompactReligionCard(style: ReligionCardStyle, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(155.dp), onClick = onClick, shape = RoundedCornerShape(22.dp)) {
        Box(
            Modifier.fillMaxWidth().height(155.dp)
                .background(Brush.linearGradient(listOf(style.start, style.end)))
                .padding(16.dp)
        ) {
            Icon(style.icon, null, Modifier.size(36.dp), tint = Color.White)
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(style.category.label, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Explore matches", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
