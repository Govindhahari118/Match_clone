package com.match.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.match.app.ui.components.MatreeFeatureTile
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeSectionHeader
import com.match.app.ui.components.MatreeDimens

private data class FeatureHubItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

private data class FeatureHubSection(
    val title: String,
    val subtitle: String,
    val items: List<FeatureHubItem>
)

private val FEATURE_SECTIONS = listOf(
    FeatureHubSection(
        "Discover",
        "More ways to find relevant people without changing your core preferences.",
        listOf(
            FeatureHubItem("Swipe discovery", "Browse profiles one at a time", Icons.Filled.Explore, MainRoutes.SWIPE_DISCOVER),
            FeatureHubItem("Recently joined", "See newer member profiles", Icons.Filled.TrendingUp, MainRoutes.RECENTLY_JOINED),
            FeatureHubItem("Regions & community", "Browse by location, language and community", Icons.Filled.Public, MainRoutes.REGIONS),
            FeatureHubItem("Community browse", "Explore community collections", Icons.Filled.Diversity3, MainRoutes.COMMUNITY_BROWSE),
            FeatureHubItem("Circles", "Curated discovery circles", Icons.Filled.Groups, MainRoutes.CIRCLES),
            FeatureHubItem("NRI matches", "International profile discovery", Icons.Filled.Language, MainRoutes.NRI_MATCH),
            FeatureHubItem("Second marriage", "A focused remarriage discovery space", Icons.Filled.Favorite, MainRoutes.SECOND_MARRIAGE)
        )
    ),
    FeatureHubSection(
        "Compatibility",
        "Optional tools that add context; they never replace your own judgement.",
        listOf(
            FeatureHubItem("Compatibility quiz", "Reflect on values, lifestyle and expectations", Icons.Filled.Quiz, MainRoutes.COMPAT_QUIZ),
            FeatureHubItem("AI match insights", "Explain matching signals and profile patterns", Icons.Filled.AutoAwesome, MainRoutes.AI_INSIGHTS),
            FeatureHubItem("Advanced horoscope", "Astrology tools when applicable", Icons.Filled.AutoAwesome, MainRoutes.ADV_HOROSCOPE),
            FeatureHubItem("Muhurat calendar", "Astrology calendar when applicable", Icons.Filled.Event, MainRoutes.MUHURAT)
        )
    ),
    FeatureHubSection(
        "Profile tools",
        "Improve how your profile is presented and understood.",
        listOf(
            FeatureHubItem("Biodata", "Create a shareable matrimonial biodata", Icons.Filled.Badge, MainRoutes.BIODATA),
            FeatureHubItem("Bio generator", "Draft and save your profile introduction", Icons.Filled.Edit, MainRoutes.BIO_GENERATOR),
            FeatureHubItem("Photo editor", "Prepare profile photos before upload", Icons.Filled.PhotoCamera, MainRoutes.PHOTO_EDITOR),
            FeatureHubItem("Video profile", "Add a personal video introduction", Icons.Filled.Videocam, MainRoutes.VIDEO_PROFILE),
            FeatureHubItem("Profile analytics", "Understand your profile activity", Icons.Filled.Analytics, MainRoutes.PROFILE_ANALYTICS),
            FeatureHubItem("Profile boost", "Manage optional profile visibility boosts", Icons.Filled.BarChart, MainRoutes.PROFILE_BOOST),
            FeatureHubItem("Family details", "Review family information shown on your profile", Icons.Filled.Groups, MainRoutes.FAMILY)
        )
    ),
    FeatureHubSection(
        "Connect",
        "Tools for safer conversations and introductions.",
        listOf(
            FeatureHubItem("Virtual meet", "Private video-meeting workflow", Icons.Filled.VideoCall, MainRoutes.VIRTUAL_MEET),
            FeatureHubItem("Secure call", "Call without exposing a personal number", Icons.Filled.RecordVoiceOver, MainRoutes.SECURE_CALL),
            FeatureHubItem("Live events", "Join guided community and compatibility events", Icons.Filled.Event, MainRoutes.LIVE_EVENTS)
        )
    ),
    FeatureHubSection(
        "Guidance & safety",
        "Support and trust tools for higher-stakes decisions.",
        listOf(
            FeatureHubItem("Assisted matchmaking", "Relationship-manager assisted search", Icons.Filled.SupportAgent, MainRoutes.ASSISTED),
            FeatureHubItem("Counselling", "Relationship and pre-marriage guidance", Icons.Filled.Psychology, MainRoutes.COUNSELLING),
            FeatureHubItem("Guides", "Educational articles and checklists", Icons.Filled.VolunteerActivism, MainRoutes.GUIDES),
            FeatureHubItem("Safety center", "Safety guidance and controls", Icons.Filled.HealthAndSafety, MainRoutes.SAFETY_CENTER),
            FeatureHubItem("Background verification", "Review verification options and status", Icons.Filled.VerifiedUser, MainRoutes.BACKGROUND_CHECK)
        )
    ),
    FeatureHubSection(
        "Journey",
        "Optional tools for milestones after a connection develops.",
        listOf(
            FeatureHubItem("Relationship timeline", "Keep milestones in one place", Icons.Filled.Timeline, MainRoutes.TIMELINE),
            FeatureHubItem("Wedding planner", "Checklist and budget planning", Icons.Filled.Celebration, MainRoutes.WEDDING_PLANNER),
            FeatureHubItem("Success stories", "Community stories", Icons.Filled.Favorite, MainRoutes.STORIES),
            FeatureHubItem("User reviews", "Read member testimonials", Icons.Filled.WorkspacePremium, MainRoutes.TESTIMONIALS),
            FeatureHubItem("Refer & earn", "Referral tools and rewards", Icons.Filled.Redeem, MainRoutes.REFERRAL),
            FeatureHubItem("Rewards & badges", "Review engagement rewards", Icons.Filled.Redeem, MainRoutes.REWARDS)
        )
    )
)

@Composable
fun FeatureHubScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    MatreeScreen(title = "Explore Matree", onBack = onBack) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(MatreeDimens.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(MatreeDimens.Space12)
        ) {
            FEATURE_SECTIONS.forEach { section ->
                item(key = "header_${section.title}") {
                    MatreeSectionHeader(
                        title = section.title,
                        subtitle = section.subtitle,
                        modifier = Modifier.padding(top = MatreeDimens.Space8, bottom = MatreeDimens.Space4)
                    )
                }
                section.items.forEach { item ->
                    item(key = item.route) {
                        MatreeFeatureTile(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }
        }
    }
}
