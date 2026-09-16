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

    /** This lock belongs only to discovery defaults; it never changes or unlocks profile religion. */
    fun setDiscoveryLocked(locked: Boolean, profileReligion: String) = viewModelScope.launch {
        if (locked) {
            val category = ReligionCategory.fromReligion(profileReligion)
            session.setReligionExperience(preference.value.copy(selected = setOf(category), locked = true))
        } else {
            session.setReligionLocked(false)
        }
    }

    /** Appearance is independent from discovery lenses and cannot mutate profile identity. */
    fun setThemeEnabled(enabled: Boolean) = viewModelScope.launch {
        session.setReligionThemeEnabled(enabled)
    }
}

/**
 * Profile-level religion experience controls. The declared religion is protected canonical profile
 * data; discovery lenses and appearance are independent, reversible preferences.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReligionExperienceCard(
    profileReligion: String,
    @Suppress("UNUSED_PARAMETER") onReligionChange: (ReligionCategory) -> Unit = {},
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
                        "Your confirmed religion is protected. Discovery and appearance preferences remain separate and reversible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.Lock, "Confirmed religion", tint = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = declared.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("My religion") },
                trailingIcon = { Icon(Icons.Filled.Lock, "Protected profile information") },
                supportingText = { Text("If this is incorrect, use the protected support correction process.") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("Discovery communities", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "These choices affect discovery only. They do not change your own religion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                Icon(if (preference.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock discovery to my religion", fontWeight = FontWeight.SemiBold)
                    Text(
                        "When enabled, Home and Discover default to ${declared.label} only. This is not the profile religion lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preference.locked, onCheckedChange = { vm.setDiscoveryLocked(it, profileReligion) })
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Follow my religion theme", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Presentation only. Turning this on or off never changes religion, matching data, privacy, prices or entitlements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.religionThemeEnabled,
                    onCheckedChange = vm::setThemeEnabled
                )
            }
        }
    }
}
