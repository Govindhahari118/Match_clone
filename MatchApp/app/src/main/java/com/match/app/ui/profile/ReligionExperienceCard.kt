package com.match.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ReligionExperiencePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReligionExperienceViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    val preference = session.religionExperience.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReligionExperiencePreference()
    )

    fun toggleLens(category: ReligionCategory, profileReligion: String) = viewModelScope.launch {
        val current = preference.value
        if (current.locked) return@launch
        val effective = current.effective(profileReligion).toMutableSet()
        if (category in effective && effective.size > 1) effective.remove(category) else effective.add(category)
        session.setReligionLenses(effective)
    }

    fun setLocked(locked: Boolean, profileReligion: String) = viewModelScope.launch {
        if (locked) {
            val category = ReligionCategory.fromReligion(profileReligion)
            session.setReligionExperience(preference.value.copy(selected = setOf(category), locked = true))
        } else {
            session.setReligionLocked(false)
        }
    }

    fun setThemeEnabled(enabled: Boolean, profileReligion: String) = viewModelScope.launch {
        val current = preference.value
        if (enabled && current.selected.isEmpty()) {
            session.setReligionExperience(
                current.copy(
                    selected = setOf(ReligionCategory.fromReligion(profileReligion)),
                    religionThemeEnabled = true
                )
            )
        } else {
            session.setReligionThemeEnabled(enabled)
        }
    }
}

/**
 * Profile-level religion controls. Declared religion is a profile field; discovery
 * lenses are separate preferences. Locking is explicit and reversible. Religion-
 * inspired styling is opt-in and the neutral matrimony theme remains the default.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReligionExperienceCard(
    profileReligion: String,
    onReligionChange: (ReligionCategory) -> Unit = {},
    vm: ReligionExperienceViewModel = hiltViewModel()
) {
    val preference by vm.preference.collectAsState()
    val effective = preference.effective(profileReligion)
    val declared = ReligionCategory.fromReligion(profileReligion)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Religion & discovery", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose your declared religion, then decide whether discovery should stay locked to it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (preference.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
            }

            Text("My religion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReligionCategory.entries.forEach { category ->
                    FilterChip(
                        selected = declared == category,
                        onClick = { if (declared != category) onReligionChange(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            HorizontalDivider()
            Text("Discovery communities", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReligionCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in effective,
                        enabled = !preference.locked,
                        onClick = { vm.toggleLens(category, profileReligion) },
                        label = { Text(category.label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock to my religion", fontWeight = FontWeight.SemiBold)
                    Text(
                        "When enabled, Home and Discover default to ${declared.label} only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preference.locked, onCheckedChange = { vm.setLocked(it, profileReligion) })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Religion-inspired theme", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional visual styling for the active religion. The standard matrimony theme is the default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.religionThemeEnabled,
                    onCheckedChange = { vm.setThemeEnabled(it, profileReligion) },
                    enabled = effective.size == 1
                )
            }
        }
    }
}
