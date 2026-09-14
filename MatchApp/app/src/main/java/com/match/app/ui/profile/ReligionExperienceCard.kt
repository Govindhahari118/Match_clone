package com.match.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
            session.setReligionExperience(
                preference.value.copy(selected = setOf(category), locked = true)
            )
        } else {
            session.setReligionLocked(false)
        }
    }

    fun setThemeEnabled(enabled: Boolean) = viewModelScope.launch {
        session.setReligionThemeEnabled(enabled)
    }
}

/**
 * Profile-level control for the discovery lens. It deliberately separates a
 * member's declared religion from what they want to browse. Locking is an
 * explicit user action and can always be reversed.
 */
@Composable
fun ReligionExperienceCard(
    profileReligion: String,
    vm: ReligionExperienceViewModel = hiltViewModel()
) {
    val preference by vm.preference.collectAsState()
    val effective = preference.effective(profileReligion)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Religion & discovery", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Your profile religion is $profileReligion. Choose which communities you want to discover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (preference.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReligionCategory.entries.forEach { category ->
                    AssistChip(
                        enabled = !preference.locked,
                        onClick = { vm.toggleLens(category, profileReligion) },
                        label = {
                            Text(if (category in effective) "✓ ${category.label}" else category.label)
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock to my religion", fontWeight = FontWeight.SemiBold)
                    Text(
                        "When on, Home and Discover default to ${ReligionCategory.fromReligion(profileReligion).label} only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.locked,
                    onCheckedChange = { vm.setLocked(it, profileReligion) }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Religion-inspired theme", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional. The normal matrimony theme stays the default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preference.religionThemeEnabled,
                    onCheckedChange = vm::setThemeEnabled,
                    enabled = effective.size == 1
                )
            }
        }
    }
}
