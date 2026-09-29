package com.match.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.match.app.ui.components.MatreeActionCard
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeVisualHero
import com.match.app.ui.theme.MatreeDesign

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
            .padding(vertical = MatreeDesign.spacing.sm)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
    ) {
        if (p == null) {
            MatreeStatePanel(
                title = "Complete your profile",
                message = "Add your core matrimonial details to discover relevant matches.",
                icon = Icons.Filled.Person,
                primaryActionLabel = "Open profile",
                onPrimaryAction = onGoProfile,
                modifier = Modifier.padding(horizontal = MatreeDesign.spacing.md)
            )
            return@Column
        }

        Column(Modifier.padding(horizontal = MatreeDesign.spacing.md)) {
            Text("Welcome, ${p.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Your preferences decide what you see — you can change them anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MatreeVisualHero(
            modifier = Modifier.padding(horizontal = MatreeDesign.spacing.md),
            actionLabel = "Discover profiles",
            onAction = onGoMatches
        )

        ReligionHomeHero(profileReligion = p.religion, onOpenMatches = onGoMatches)

        HomeSectionTitle("Your activity")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
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
        MatreeActionCard(
            onClick = if (religion == ReligionCategory.HINDU) onGoKundli else onGoQuiz,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(MatreeDesign.spacing.sm))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HomeSectionTitle("Essentials")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                EssentialButton(Icons.Filled.Search, "Discover", Modifier.weight(1f), onGoMatches)
                EssentialButton(Icons.Filled.LocationOn, "Nearby", Modifier.weight(1f), onGoNearby)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                EssentialButton(Icons.Filled.Chat, "Messages", Modifier.weight(1f), onGoMessages)
                EssentialButton(Icons.Filled.Person, "Profile", Modifier.weight(1f), onGoProfile)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                EssentialButton(Icons.Filled.Verified, "Verification", Modifier.weight(1f), onGoVerification)
                EssentialButton(Icons.Filled.PrivacyTip, "Privacy", Modifier.weight(1f), onGoPrivacyDash)
            }
            EssentialButton(Icons.Filled.WorkspacePremium, "Membership", Modifier.fillMaxWidth(), onGoPricing)
        }

        Spacer(Modifier.height(MatreeDesign.spacing.lg))
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = MatreeDesign.spacing.md)
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
    MatreeActionCard(onClick = onClick, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = MatreeDesign.spacing.xs),
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
    MatreeSecondaryButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        icon = icon
    )
}
