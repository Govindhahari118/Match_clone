package com.match.app.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfileAnalyticsUi(
    val loading: Boolean = true,
    val profileViews: Int = 0,
    val completeness: Int = 0,
    val verificationLevel: Int = 0,
    val hasBio: Boolean = false,
    val hasFamilyDetails: Boolean = false,
    val hasVideo: Boolean = false
)

@HiltViewModel
class ProfileAnalyticsViewModel @Inject constructor(
    session: SessionStore,
    userDao: UserDao
) : ViewModel() {
    val ui: StateFlow<ProfileAnalyticsUi> = session.userId
        .filterNotNull()
        .flatMapLatest { userDao.observeById(it) }
        .map { user ->
            if (user == null) ProfileAnalyticsUi(loading = false)
            else ProfileAnalyticsUi(
                loading = false,
                profileViews = user.profileViewCount.coerceAtLeast(0),
                completeness = (user.profileCompleteness.coerceIn(0f, 1f) * 100).toInt(),
                verificationLevel = user.verificationLevel.coerceAtLeast(0),
                hasBio = user.bio.isNotBlank(),
                hasFamilyDetails = user.familyType.isNotBlank() || user.aboutFamily.isNotBlank(),
                hasVideo = user.videoUrl.isNotBlank()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileAnalyticsUi())
}

private data class Metric(val icon: ImageVector, val label: String, val value: String, val description: String)
private data class Improvement(val icon: ImageVector, val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAnalyticsScreen(
    onBack: () -> Unit = {},
    vm: ProfileAnalyticsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_analytics", "Profile Analytics"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analytics_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (ui.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val metrics = listOf(
            Metric(Icons.Filled.Visibility, "Profile views", ui.profileViews.toString(), "Total recorded views on your profile"),
            Metric(Icons.Filled.Person, "Profile complete", "${ui.completeness}%", "Calculated from the profile fields you have actually provided"),
            Metric(Icons.Filled.Verified, "Verification level", ui.verificationLevel.toString(), "Current verification level stored on your account")
        )
        val improvements = buildList {
            if (!ui.hasBio) add(Improvement(Icons.Filled.Info, "Add an introduction", "A clear bio helps another member understand you before starting a conversation."))
            if (!ui.hasFamilyDetails) add(Improvement(Icons.Filled.FamilyRestroom, "Review family details", "Add only the family information you are comfortable representing on your profile."))
            if (!ui.hasVideo) add(Improvement(Icons.Filled.Videocam, "Consider a video profile", "Use video only if you are comfortable with the visibility and privacy settings."))
            if (ui.verificationLevel <= 0) add(Improvement(Icons.Filled.Verified, "Review verification options", "Verification can add account context, but it should never be presented as a guarantee about a person."))
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp).testTag("analytics_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Only real account metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Weekly trends, rankings and demographic percentages are intentionally hidden until a real analytics pipeline produces them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            metrics.forEach { metric ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(metric.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(metric.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text("Profile improvements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (improvements.isEmpty()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Your main profile sections are populated. Keep them accurate as your situation changes.")
                    }
                }
            } else {
                improvements.forEach { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
