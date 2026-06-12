@file:Suppress("UNUSED_PARAMETER")
package com.match.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Today's Picks ──────────────────────────────────────────────────────────
@HiltViewModel
class TodayPicksViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val auth: AuthRepository
) : ViewModel() {
    private val _picks = MutableStateFlow<List<UserProfile>>(emptyList())
    val picks = _picks.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val entities = userDao.allExcluding(uid).shuffled().take(8)
            _picks.value = entities.mapNotNull { auth.currentProfile(it.id) }
        }
    }
}

/**
 * Multi-app launcher home â€” large coloured tiles that act as the main entry
 * points to every major surface in the app.  Shows a login prompt banner
 * when scrolled past a threshold (for guest preview scenarios).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLauncherScreen(
    onGoMatches:       () -> Unit,
    onGoQuiz:          () -> Unit,
    onGoStories:       () -> Unit,
    onGoPricing:       () -> Unit,
    onGoInterests:     () -> Unit,
    onGoNotifications: () -> Unit,
    onGoShortlists:    () -> Unit,
    onGoRegions:       () -> Unit,
    onGoCircles:       () -> Unit,
    onGoMessages:      () -> Unit,
    onGoProfile:       () -> Unit,
    onGoVerification:  () -> Unit,
    onGoKundli:        () -> Unit,
    onGoWhoViewed:     () -> Unit,
    onGoFamily:        () -> Unit,
    onGoHelp:          () -> Unit,
    onGoBiodata:            () -> Unit = {},
    onGoSecondMarriage:     () -> Unit = {},
    onGoCompatibilityQuiz:  () -> Unit = {},
    onGoAssisted:           () -> Unit = {},
    onGoVirtualMeet:        () -> Unit = {},
    onGoBioGen:             () -> Unit = {},
    onGoPhotoEditor:        () -> Unit = {},
    onGoCounselling:        () -> Unit = {},
    onGoGuides:             () -> Unit = {},
    onGoBoost:              () -> Unit = {},
    onGoVideoProfile:       () -> Unit = {},
    onGoRecentlyJoined:     () -> Unit = {},
    onGoTestimonials:       () -> Unit = {},
    onGoSwipeDiscover:      () -> Unit = {},
    onGoCommunityBrowse:    () -> Unit = {},
    onGoLiveEvents:         () -> Unit = {},
    onGoBgCheck:            () -> Unit = {},
    onGoSecureCall:         () -> Unit = {},
    onGoPrivacyDash:        () -> Unit = {},
    onGoAIInsights:         () -> Unit = {},
    onGoAnalytics:          () -> Unit = {},
    onGoWeddingPlanner:     () -> Unit = {},
    onGoAdvHoroscope:       () -> Unit = {},
    onGoDailyRewards:       () -> Unit = {},
    onGoNearby:             () -> Unit = {},
    onGoNRIMatch:           () -> Unit = {},
    onGoSafetyCenter:       () -> Unit = {},
    onGoTimeline:           () -> Unit = {},
    onGoReferral:           () -> Unit = {},
    onGoMuhurat:            () -> Unit = {},
    onGoDeepCompat:         () -> Unit = {},
    onGoWizard:             () -> Unit = {},
    onOpenProfile:          (Long) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
    picksVm: TodayPicksViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val todayPicks by picksVm.picks.collectAsState()
    val p = ui.profile
    val scrollState = rememberScrollState()
    var showLoginModal by remember { mutableStateOf(false) }
    var allExpanded by remember { mutableStateOf(false) }
    // Capture catalog so tile labels re-translate instantly when language changes
    val catalog = com.match.app.ui.i18n.LocalI18n.current

    LaunchedEffect(scrollState.value, p) {
        if (p == null && scrollState.value > 400 && !showLoginModal) {
            kotlinx.coroutines.delay(1500)
            showLoginModal = true
        }
    }

    // â”€â”€ Section tile lists â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val discoverTiles = remember(catalog) {
        listOf(
            LauncherTile(catalog.text("browse", "Browse"),        Icons.Filled.Search,       Color(0xFFE91E63), tag = "sec_browse",    onClick = onGoMatches),
            LauncherTile(catalog.text("swipe", "Swipe"),          Icons.Filled.SwipeRight,   Color(0xFF7B1FA2), tag = "sec_swipe",     onClick = onGoSwipeDiscover),
            LauncherTile(catalog.text("nearby", "Nearby"),        Icons.Filled.NearMe,       Color(0xFF2E7D32), tag = "sec_nearby",    onClick = onGoNearby),
            LauncherTile(catalog.text("nri_match", "NRI Match"),  Icons.Filled.Flight,       Color(0xFF1565C0), tag = "sec_nri",       onClick = onGoNRIMatch),
        )
    }
    val connectTiles = remember(catalog, ui) {
        listOf(
            LauncherTile(catalog.text("interests", "Interests"),    Icons.Filled.MoveToInbox,  Color(0xFFFF6F00), badge = ui.pendingInterests, tag = "sec_interests",  onClick = onGoInterests),
            LauncherTile(catalog.text("messages", "Messages"),      Icons.Filled.Forum,         Color(0xFF1976D2), tag = "sec_msgs",      onClick = onGoMessages),
            LauncherTile(catalog.text("who_viewed", "Who Viewed"),  Icons.Filled.RemoveRedEye, Color(0xFF455A64), tag = "sec_wv",        onClick = onGoWhoViewed),
            LauncherTile(catalog.text("live_events", "Live Events"), Icons.Filled.Celebration,  Color(0xFF6A1B9A), tag = "sec_events",    onClick = onGoLiveEvents),
        )
    }
    val compatTiles = remember(catalog) {
        listOf(
            LauncherTile(catalog.text("kundli", "Kundli"),               Icons.Filled.AutoAwesome,  Color(0xFF7A4E9C), tag = "sec_kundli",    onClick = onGoKundli),
            LauncherTile(catalog.text("ai_match", "AI Match"),           Icons.Filled.Psychology,   Color(0xFF6200EA), tag = "sec_ai",        onClick = onGoAIInsights),
            LauncherTile(catalog.text("horoscope_plus", "Horoscope+"),   Icons.Filled.Star,         Color(0xFFE65100), tag = "sec_horo",      onClick = onGoAdvHoroscope),
            LauncherTile(catalog.text("deep_compat", "Deep Compat"),     Icons.Filled.Favorite,     Color(0xFF880E4F), tag = "sec_compat",    onClick = onGoDeepCompat),
        )
    }
    val profileTiles = remember(catalog) {
        listOf(
            LauncherTile(catalog.text("my_profile", "My Profile"), Icons.Filled.Person,       Color(0xFF512DA8), tag = "sec_profile",   onClick = onGoProfile),
            LauncherTile(catalog.text("verify_me", "Verify Me"),   Icons.Filled.Verified,     Color(0xFF1565C0), tag = "sec_verify",    onClick = onGoVerification),
            LauncherTile(catalog.text("photos", "Photos"),         Icons.Filled.PhotoCamera,  Color(0xFFD81B60), tag = "sec_photos",    onClick = onGoPhotoEditor),
            LauncherTile(catalog.text("boost", "Boost"),           Icons.Filled.Bolt,         Color(0xFFFFB300), tag = "sec_boost",     onClick = onGoBoost),
        )
    }
    val weddingTiles = remember(catalog) {
        listOf(
            LauncherTile(catalog.text("wedding_plan", "Wedding Plan"), Icons.Filled.Cake,         Color(0xFFAD1457), tag = "sec_wedding",   onClick = onGoWeddingPlanner),
            LauncherTile(catalog.text("muhurat", "Muhurat"),           Icons.Filled.CalendarMonth,Color(0xFFBF360C), tag = "sec_muhurat",   onClick = onGoMuhurat),
            LauncherTile(catalog.text("timeline", "Timeline"),         Icons.Filled.Timeline,     Color(0xFF4A148C), tag = "sec_timeline",  onClick = onGoTimeline),
            LauncherTile(catalog.text("counselling", "Counselling"),   Icons.Filled.Psychology,   Color(0xFF00796B), tag = "sec_counsel",   onClick = onGoCounselling),
        )
    }
    val allTiles = remember(catalog, ui) {
        listOf(
            LauncherTile(catalog.text("shortlist", "Shortlist"),        Icons.Filled.Bookmark,        Color(0xFF2E7D32), badge = ui.shortlistCount, tag = "all_shortlists",      onClick = onGoShortlists),
            LauncherTile(catalog.text("notifications", "Notifications"),Icons.Filled.Notifications,   Color(0xFFD84315), badge = ui.unreadNotif,    tag = "all_notifications",   onClick = onGoNotifications),
            LauncherTile(catalog.text("regions", "Regions"),            Icons.Filled.Map,             Color(0xFF6A1B9A),                            tag = "all_regions",         onClick = onGoRegions),
            LauncherTile(catalog.text("circles", "Circles"),            Icons.Filled.Groups,          Color(0xFF00838F),                            tag = "all_circles",         onClick = onGoCircles),
            LauncherTile(catalog.text("stories", "Stories"),            Icons.Filled.EmojiEvents,     Color(0xFFC2185B),                            tag = "all_stories",         onClick = onGoStories),
            LauncherTile(catalog.text("pricing", "Pricing"),            Icons.Filled.WorkspacePremium,Color(0xFFFFB300),                            tag = "all_pricing",         onClick = onGoPricing),
            LauncherTile(catalog.text("quiz", "Quiz"),                  Icons.AutoMirrored.Filled.ListAlt,         Color(0xFF558B2F),                            tag = "all_quiz",            onClick = onGoQuiz),
            LauncherTile(catalog.text("family", "Family"),              Icons.Filled.FamilyRestroom,  Color(0xFF8D6E63),                            tag = "all_family",          onClick = onGoFamily),
            LauncherTile(catalog.text("help", "Help"),                  Icons.AutoMirrored.Filled.Help,            Color(0xFF607D8B),                            tag = "all_help",            onClick = onGoHelp),
            LauncherTile(catalog.text("biodata", "Biodata"),            Icons.Filled.Description,     Color(0xFF8B1A1A),                            tag = "all_biodata",         onClick = onGoBiodata),
            LauncherTile(catalog.text("compat_quiz", "Compat. Quiz"),   Icons.Filled.Psychology,      Color(0xFFE65100),                            tag = "all_compat_quiz",     onClick = onGoCompatibilityQuiz),
            LauncherTile(catalog.text("second_marriage", "2nd Marriage"),Icons.Filled.Favorite,       Color(0xFF4A148C),                            tag = "all_second_marriage", onClick = onGoSecondMarriage),
            LauncherTile(catalog.text("assisted_rm", "Assisted RM"),    Icons.Filled.SupportAgent,   Color(0xFF8B1A1A),                            tag = "all_assisted",        onClick = onGoAssisted),
            LauncherTile(catalog.text("virtual_meet", "Virtual Meet"),  Icons.Filled.VideoCall,       Color(0xFF1565C0),                            tag = "all_virtual_meet",    onClick = onGoVirtualMeet),
            LauncherTile(catalog.text("bio_gen", "Bio Gen"),            Icons.Filled.AutoAwesome,     Color(0xFFE65100),                            tag = "all_bio_gen",         onClick = onGoBioGen),
            LauncherTile(catalog.text("video_profile", "Video Profile"),Icons.Filled.Videocam,        Color(0xFF7B1FA2),                            tag = "all_video_profile",   onClick = onGoVideoProfile),
            LauncherTile(catalog.text("new_profiles", "New Profiles"),  Icons.Filled.FiberNew,        Color(0xFF0097A7),                            tag = "all_recently_joined", onClick = onGoRecentlyJoined),
            LauncherTile(catalog.text("reviews", "Reviews"),            Icons.Filled.Star,            Color(0xFFF57C00),                            tag = "all_testimonials",    onClick = onGoTestimonials),
            LauncherTile(catalog.text("communities", "Communities"),    Icons.Filled.Groups,          Color(0xFF283593),                            tag = "all_community",       onClick = onGoCommunityBrowse),
            LauncherTile(catalog.text("bg_check", "BG Check"),          Icons.Filled.Shield,          Color(0xFF1A237E),                            tag = "all_bgcheck",         onClick = onGoBgCheck),
            LauncherTile(catalog.text("secure_call", "Secure Call"),    Icons.Filled.PhoneInTalk,     Color(0xFF00695C),                            tag = "all_securecall",      onClick = onGoSecureCall),
            LauncherTile(catalog.text("privacy", "Privacy"),            Icons.Filled.Lock,            Color(0xFF37474F),                            tag = "all_privacy",         onClick = onGoPrivacyDash),
            LauncherTile(catalog.text("analytics", "Analytics"),        Icons.Filled.BarChart,        Color(0xFF00838F),                            tag = "all_analytics",       onClick = onGoAnalytics),
            LauncherTile(catalog.text("daily_rewards", "Daily Rewards"),Icons.Filled.CardGiftcard,    Color(0xFFFF6F00),                            tag = "all_rewards",         onClick = onGoDailyRewards),
            LauncherTile(catalog.text("safety", "Safety"),              Icons.Filled.HealthAndSafety, Color(0xFFB71C1C),                            tag = "all_safety",          onClick = onGoSafetyCenter),
            LauncherTile(catalog.text("refer_earn", "Refer & Earn"),    Icons.Filled.Share,           Color(0xFF6A1B9A),                            tag = "all_referral",        onClick = onGoReferral),
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).testTag("home_screen"),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // â”€â”€ 1. Gradient greeting header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )))
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val firstName = p?.displayName?.split(" ")?.firstOrNull() ?: "there"
                    Text("Hi $firstName \uD83D\uDC4B", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    val subMsg = when {
                        ui.pendingInterests > 0 -> t("people_sent_interest", "${ui.pendingInterests} people sent you interest!").replace("{n}", "${ui.pendingInterests}")
                        ui.mutualCount > 0      -> t("mutual_matches_waiting", "${ui.mutualCount} mutual matches waiting 🎉").replace("{n}", "${ui.mutualCount}")
                        else                    -> t("find_perfect_match_today", "Find your perfect match today")
                    }
                    Text(subMsg, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.88f))
                    if (p != null) {
                        val steps = listOf(p.bio.isNotBlank(), p.city.isNotBlank(), p.religion.isNotBlank(), p.education.isNotBlank(), p.profession.isNotBlank())
                        val pct = steps.count { it }.toFloat() / steps.size
                        if (pct < 1f) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color.White, trackColor = Color.White.copy(0.3f)
                                )
                                Text("${(pct * 100).toInt()}% ${t("complete", "complete")}",
                                    style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── Profile completeness nudge ─────────────────────────────────────────────
            if (p != null) {
                val completionSteps = listOf(
                    p.bio.isNotBlank()        to t("add_bio", "Add a bio"),
                    p.city.isNotBlank()       to t("add_city", "Add your city"),
                    p.religion.isNotBlank()   to t("add_religion", "Add religion"),
                    p.education.isNotBlank()  to t("add_education", "Add education"),
                    p.profession.isNotBlank() to t("add_profession", "Add profession")
                )
                val done = completionSteps.count { it.first }
                val total = completionSteps.size
                if (done < total) {
                    val nextStep = completionSteps.firstOrNull { !it.first }?.second ?: ""
                    ElevatedCard(
                        onClick = onGoWizard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("profile_nudge"),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.AccountCircle, null, Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    t("complete_profile_nudge", "Complete your profile ($done/$total)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(nextStep, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LinearProgressIndicator(
                                    progress = { done.toFloat() / total },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── 2. Getting Started Guide (New Users) ───────────────────────────────────
            if (p != null && (!p.isVerified || !ui.hasQuestionnaire)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        t("getting_started", "Getting Started"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GuideStepCard(
                            title = "Verify",
                            done = p.isVerified,
                            icon = Icons.Filled.Verified,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f),
                            onClick = onGoVerification
                        )
                        GuideStepCard(
                            title = "Compatibility",
                            done = ui.hasQuestionnaire,
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            color = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f),
                            onClick = onGoQuiz
                        )
                        GuideStepCard(
                            title = "Boost",
                            done = p.isPremium,
                            icon = Icons.Filled.Bolt,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f),
                            onClick = onGoBoost
                        )
                    }
                }
            }

            // ── 2. Stats row ───────────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(t("interests", "Interests"), ui.pendingInterests, Icons.Filled.MoveToInbox, Color(0xFFFF6F00), Modifier.weight(1f), onClick = onGoInterests)
                StatCard(t("mutual", "Mutual"),    ui.mutualCount,       Icons.Filled.Favorite,    Color(0xFFE91E63), Modifier.weight(1f), onClick = onGoMatches)
                StatCard(t("saved", "Saved"),     ui.shortlistCount,    Icons.Filled.Bookmark,    Color(0xFF2E7D32), Modifier.weight(1f), onClick = onGoShortlists)
                StatCard(t("alerts", "Alerts"),    ui.unreadNotif,       Icons.Filled.Notifications,Color(0xFFD84315), Modifier.weight(1f), onClick = onGoNotifications)
            }

            // â”€â”€ 3. Primary 2Ã—2 action buttons â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryActionBtn(t("browse_profiles", "Browse Profiles"), Icons.Filled.Search,     Color(0xFFE91E63), Modifier.weight(1f), onGoMatches)
                    PrimaryActionBtn(t("swipe_and_like", "Swipe & Like"),    Icons.Filled.SwipeRight, Color(0xFF7B1FA2), Modifier.weight(1f), onGoSwipeDiscover)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryActionBtn(t("interests", "Interests"),       Icons.Filled.FavoriteBorder, Color(0xFFFF6F00), Modifier.weight(1f), onGoInterests)
                    PrimaryActionBtn(t("messages", "Messages"),        Icons.Filled.Forum,          Color(0xFF1976D2), Modifier.weight(1f), onGoMessages)
                }
            }

            Spacer(Modifier.height(12.dp))

            // â”€â”€ 4. Community browse strip â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            CommunityQuickBrowse(onBrowse = onGoMatches, onCommunity = onGoCommunityBrowse)


            // -- 4b. Today's Picks strip -------------------------------------------------------
            if (todayPicks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TodayPicksStrip(picks = todayPicks, onOpenProfile = onOpenProfile, onSeeAll = onGoMatches)
            }
            Spacer(Modifier.height(12.dp))

            // â”€â”€ 5. Categorised feature sections â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FeatureSection("🔍 ${t("discover_matches", "Discover Matches")}",          discoverTiles)
                FeatureSection("💬 ${t("connect", "Connect")}",                            connectTiles)
                FeatureSection("⭐ ${t("compatibility_astrology", "Compatibility & Astrology")}", compatTiles)
                FeatureSection("👤 ${t("my_profile", "My Profile")}",                      profileTiles)
                FeatureSection("💍 ${t("wedding_lifestyle", "Wedding & Lifestyle")}",      weddingTiles)
            }

            // â”€â”€ 6. Premium CTA â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (p != null && !p.isPremium) {
                Spacer(Modifier.height(12.dp))
                ElevatedCard(
                    onClick = onGoPricing,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("home_premium_cta"),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t("upgrade_to_premium", "Upgrade to Premium"), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text(t("premium_subtitle", "Unlimited contacts · See who viewed · Priority listing"),
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E342E))
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFE65100))
                    }
                }
            }

            // â”€â”€ 7. Assisted RM card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Spacer(Modifier.height(10.dp))
            ElevatedCard(
                onClick = onGoAssisted,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("home_assisted"),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF8B1A1A).copy(alpha = 0.07f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SupportAgent, null, Modifier.size(36.dp), tint = Color(0xFF8B1A1A))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("assisted_matchmaking", "Assisted Matchmaking"), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF8B1A1A))
                        Text(t("assisted_subtitle", "Dedicated RM · Find a match 10× faster"),
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E342E))
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF8B1A1A))
                }
            }

            // â”€â”€ 8. All features (collapsed) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick = { allExpanded = !allExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (allExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (allExpanded) t("show_less", "Show less") else t("all_features_count", "All features (${allTiles.size} more)").replace("{n}", "${allTiles.size}"))
                }
                if (allExpanded) {
                    Spacer(Modifier.height(12.dp))
                    LauncherGrid(tiles = allTiles)
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // â”€â”€ Login modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (showLoginModal) {
            ModalBottomSheet(onDismissRequest = { showLoginModal = false }) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(t("sign_in_to_continue", "Sign in to continue"), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text(t("access_all_features", "Access all features – matches, interests, chat & more."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showLoginModal = false; onGoProfile() },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("home_login_cta")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("sign_in", "Sign in"))
                    }
                    TextButton(onClick = { showLoginModal = false }) { Text(t("continue_browsing", "Continue browsing")) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class LauncherTile(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val badge: Int = 0,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun GuideStepCard(
    title: String,
    done: Boolean,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(90.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (done) color.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(24.dp), tint = if (done) color else color.copy(alpha = 0.5f))
                if (done) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        Modifier.size(14.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun LauncherGrid(tiles: List<LauncherTile>) {
    val cols = 3
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile -> LauncherTileView(tile, modifier = Modifier.weight(1f)) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LauncherTileView(tile: LauncherTile, modifier: Modifier = Modifier) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(96.dp).clickable(onClick = tile.onClick).testTag(tile.tag)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Surface(shape = RoundedCornerShape(50), color = tile.accent.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(tile.icon, null, Modifier.size(22.dp), tint = tile.accent) }
                }
                Spacer(Modifier.height(6.dp))
                Text(tile.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            if (tile.badge > 0) {
                Badge(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Text("${tile.badge}") }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Text(if (count > 0) "$count" else "\u2014", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrimaryActionBtn(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.height(72.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color.copy(0.08f))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = color.copy(0.15f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = color) }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FeatureSection(title: String, tiles: List<LauncherTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.forEach { tile ->
                ElevatedCard(onClick = tile.onClick, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(80.dp).testTag(tile.tag)) {
                    Column(Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Surface(shape = RoundedCornerShape(10.dp), color = tile.accent.copy(0.13f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(tile.icon, null, Modifier.size(18.dp), tint = tile.accent) }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(tile.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
@Composable
private fun WeddingEcoTile(emoji: String, title: String, color: Color, modifier: Modifier) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// Today's Picks Strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TodayPicksStrip(
    picks: List<com.match.app.domain.model.UserProfile>,
    onOpenProfile: (Long) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "💝 ${t("todays_picks", "Today's Picks")}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSeeAll) { Text(t("see_all", "See all")) }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(picks, key = { it.id }) { profile ->
                PickCard(profile = profile, onClick = { onOpenProfile(profile.id) })
            }
        }
    }
}

@Composable
private fun PickCard(profile: com.match.app.domain.model.UserProfile, onClick: () -> Unit) {
    val avatarColors = remember(profile.id) {
        val palette = listOf(
            listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
            listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
            listOf(Color(0xFF009688), Color(0xFF4CAF50)),
            listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
            listOf(Color(0xFF795548), Color(0xFF607D8B)),
            listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
        )
        palette[(profile.id % palette.size).toInt()]
    }
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(avatarColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            Text(profile.displayName.split(" ").firstOrNull() ?: profile.displayName,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1)
            Text("${profile.age} \u2022 ${profile.city.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            if (profile.isVerified) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE3F2FD)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, Modifier.size(10.dp), tint = Color(0xFF1976D2))
                        Text("Verified", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Community Quick Browse Strip â€” like Telugu Matrimony / BrahminMatrimony
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private data class CommunityEntry(val emoji: String, val label: String, val sub: String, val color: Color)

@Composable
private fun CommunityQuickBrowse(onBrowse: () -> Unit, onCommunity: () -> Unit) {
    val communities = remember { listOf(
        CommunityEntry("\uD83D\uDD24", "Telugu",    "Andhra & Telangana",  Color(0xFF1565C0)),
        CommunityEntry("\uD83D\uDD24", "Tamil",     "Tamil Nadu",          Color(0xFF880E4F)),
        CommunityEntry("\uD83D\uDD24", "Kannada",   "Karnataka",           Color(0xFFE65100)),
        CommunityEntry("\uD83D\uDD24", "Malayalam", "Kerala",              Color(0xFF2E7D32)),
        CommunityEntry("\uD83D\uDD24", "Hindi",     "North India",         Color(0xFF6A1B9A)),
        CommunityEntry("\uD83D\uDD24", "Marathi",   "Maharashtra",         Color(0xFF0277BD)),
        CommunityEntry("\uD83D\uDECD", "Hindu",     "All Communities",     Color(0xFFBF360C)),
        CommunityEntry("\uD83C\uDFDB\uFE0F", "Brahmin",  "All Regions",         Color(0xFF4A148C)),
        CommunityEntry("\u2694\uFE0F", "Kshatriya","Rajput \u2022 Nair \u2022 etc", Color(0xFF37474F)),
        CommunityEntry("\u271D\uFE0F", "Christian", "All Denominations",   Color(0xFF1A237E)),
        CommunityEntry("\u262A\uFE0F", "Muslim",    "All Sects",           Color(0xFF1B5E20)),
        CommunityEntry("\uD83D\uDD4D", "Jain",      "All Sects",           Color(0xFF4E342E)),
        CommunityEntry("\u2708\uFE0F", "NRI",       "Abroad Profiles",     Color(0xFF006064)),
    )}

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t("browse_by_community", "Browse by Community"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onCommunity) { Text(t("see_all", "See all")) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(communities) { c ->
                ElevatedCard(
                    onClick = onBrowse,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(100.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(10.dp), color = c.color.copy(0.12f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(c.emoji, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            }
                        }
                        Text(c.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.color, textAlign = TextAlign.Center)
                        Text(c.sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
    }
}
