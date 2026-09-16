package com.match.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
}

/**
 * Declared religion is canonical, protected profile data. Discovery lenses and appearance are
 * separate reversible preferences. A correction is deliberately routed to support/account review;
 * this card never writes the religion field itself.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReligionExperienceCard(
    profileReligion: String,
    onRequestCorrection: () -> Unit,
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
                        "Your declared religion is protected profile data. Discovery choices below do not modify it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (preference.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
            }

            Text("My religion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(profileReligion.ifBlank { "Not specified" }) },
                leadingIcon = { Icon(Icons.Filled.Lock, "Protected profile field", Modifier.size(16.dp)) }
            )
            Text(
                "If this was entered incorrectly during onboarding, request a reviewed correction. It cannot be changed by a normal client profile edit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRequestCorrection, modifier = Modifier.fillMaxWidth()) {
                Text("Request religion correction")
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
                    Text("Restrict discovery to my religion", fontWeight = FontWeight.SemiBold)
                    Text(
                        "When enabled, Home and Discover default to ${declared.label} only. This is a partner-discovery preference, not the religion lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preference.locked, onCheckedChange = { vm.setLocked(it, profileReligion) })
            }

            Text(
                "Visual theme is managed separately in Settings > Appearance and can always be neutral or manually selected.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
