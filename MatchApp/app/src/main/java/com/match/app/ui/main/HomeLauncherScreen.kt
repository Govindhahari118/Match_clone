package com.match.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.domain.model.ReligionCategory

/** Production home intentionally exposes only audited journeys. */
@Composable
fun HomeLauncherScreen(
    onGoMatches:       () -> Unit,
    onGoQuiz:          () -> Unit,
    onGoPricing:       () -> Unit,
    onGoInterests:     () -> Unit,
    onGoNotifications: () -> Unit,
    onGoShortlists:    () -> Unit,
    onGoMessages:      () -> Unit,
    onGoProfile:       () -> Unit,
    onGoVerification:  () -> Unit,
    onGoKundli:        () -> Unit,
    onGoPrivacyDash:   () -> Unit,
    onGoNearby:        () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val p = ui.profile

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (p == null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Complete your profile to discover relevant matches", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onGoProfile) { Text("Open profile") }
                }
            }
            return@Column
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Welcome, ${p.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Your preferences decide what you see — you can change them anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ReligionHomeHero(profileReligion = p.religion, onOpenMatches = onGoMatches)

        HomeSectionTitle("Your activity")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeActionCard(Icons.Filled.Favorite, "Matches", ui.mutualCount.toString(), Modifier.weight(1f), onGoMatches)
            HomeActionCard(Icons.Filled.AutoAwesome, "Interests", ui.pendingInterests.toString(), Modifier.weight(1f), onGoInterests)
            HomeActionCard(Icons.Filled.Bookmark, "Saved", ui.shortlistCount.toString(), Modifier.weight(1f), onGoShortlists)
            BadgedBox(
                badge = { if (ui.unreadNotif > 0) Badge { Text(ui.unreadNotif.toString()) } },
                modifier = Modifier.weight(1f)
            ) {
                HomeActionCard(Icons.Filled.Notifications, "Alerts", ui.unreadNotif.toString(), Modifier.fillMaxWidth(), onGoNotifications)
            }
        }

        val religion = ReligionCategory.fromReligion(p.religion)
        HomeSectionTitle(if (religion == ReligionCategory.HINDU) "Special Home" else "Compatibility Home")
        Card(
            onClick = if (religion == ReligionCategory.HINDU) onGoKundli else onGoQuiz,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.padding(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (religion == ReligionCategory.HINDU) "Astrology-compatible matches" else "Preference-compatible matches",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (religion == ReligionCategory.HINDU)
                            "Use birth details, Rasi, Nakshatra and Kundali compatibility when those details are available."
                        else
                            "Use values, lifestyle, family and partner preferences without forcing irrelevant astrology fields.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        HomeSectionTitle("Essentials")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EssentialButton(Icons.Filled.Search, "Discover", Modifier.weight(1f), onGoMatches)
                EssentialButton(Icons.Filled.LocationOn, "Nearby", Modifier.weight(1f), onGoNearby)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EssentialButton(Icons.Filled.Chat, "Messages", Modifier.weight(1f), onGoMessages)
                EssentialButton(Icons.Filled.Person, "Profile", Modifier.weight(1f), onGoProfile)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EssentialButton(Icons.Filled.Verified, "Verification", Modifier.weight(1f), onGoVerification)
                EssentialButton(Icons.Filled.PrivacyTip, "Privacy", Modifier.weight(1f), onGoPrivacyDash)
            }
            EssentialButton(Icons.Filled.WorkspacePremium, "Membership", Modifier.fillMaxWidth(), onGoPricing)
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EssentialButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp)) {
        Icon(icon, null)
        Spacer(Modifier.padding(4.dp))
        Text(label)
    }
}
