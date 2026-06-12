package com.match.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.match.app.MainActivity
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.repo.NotificationRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.chat.ChatListScreen
import com.match.app.ui.chat.ChatScreen
import com.match.app.ui.circles.CirclesScreen
import com.match.app.ui.detail.MatchDetailScreen
import com.match.app.ui.family.FamilyScreen
import com.match.app.ui.help.HelpScreen
import com.match.app.ui.interests.InterestsScreen
import com.match.app.ui.kundli.KundliScreen
import com.match.app.ui.legal.LegalScreen
import com.match.app.ui.likes.LikesScreen
import com.match.app.ui.matches.MatchesScreen
import com.match.app.ui.notifications.NotificationsScreen
import com.match.app.ui.pricing.PricingScreen
import com.match.app.ui.profile.ProfileScreen
import com.match.app.ui.questionnaire.QuestionnaireScreen
import com.match.app.ui.regions.RegionsScreen
import com.match.app.ui.settings.SettingsScreen
import com.match.app.ui.shortlist.ShortlistScreen
import com.match.app.ui.stories.SuccessStoriesScreen
import com.match.app.ui.biodata.BiodataScreen
import com.match.app.ui.verification.VerificationScreen
import com.match.app.ui.whoviewed.WhoViewedScreen
import com.match.app.ui.quiz.CompatibilityQuizScreen
import com.match.app.ui.secondmarriage.SecondMarriageScreen
import com.match.app.ui.assisted.AssistedServiceScreen
import com.match.app.ui.meet.VirtualMeetScreen
import com.match.app.ui.biogen.BioGeneratorScreen
import com.match.app.ui.photoeditor.PhotoEditorScreen
import com.match.app.ui.counselling.CounsellingScreen
import com.match.app.ui.guides.GuidesScreen
import com.match.app.ui.boost.ProfileBoostScreen
import com.match.app.ui.videoprofile.VideoProfileScreen
import com.match.app.ui.recentlyjoined.RecentlyJoinedScreen
import com.match.app.ui.testimonials.TestimonialsScreen
import com.match.app.ui.discovery.SwipeDiscoveryScreen
import com.match.app.ui.community.CommunityBrowseScreen
import com.match.app.ui.events.LiveEventsScreen
import com.match.app.ui.bgcheck.BackgroundCheckScreen
import com.match.app.ui.securecall.SecureCallScreen
import com.match.app.ui.privacy.PrivacyDashboardScreen
import com.match.app.ui.aiinsights.AIMatchInsightsScreen
import com.match.app.ui.analytics.ProfileAnalyticsScreen
import com.match.app.ui.wedding.WeddingPlannerScreen
import com.match.app.ui.horoscope.AdvancedHoroscopeScreen
import com.match.app.ui.rewards.DailyRewardsScreen
import com.match.app.ui.nearby.NearbyMatchesScreen
import com.match.app.ui.nri.NRIMatchScreen
import com.match.app.ui.safety.SafetyCenterScreen
import com.match.app.ui.timeline.RelationshipTimelineScreen
import com.match.app.ui.referral.MatchmakerReferralScreen
import com.match.app.ui.muhurat.AstroCalendarScreen
import com.match.app.ui.deepcompat.CompatibilityDeepDiveScreen
import com.match.app.ui.phone.PhoneVerificationScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Routes ─────────────────────────────────────────────────────────────────
object MainRoutes {
    const val HOME          = "home"
    const val MATCHES       = "matches"
    const val LIKES         = "likes"
    const val INTERESTS     = "interests"
    const val SHORTLISTS    = "shortlists"
    const val CHAT_LIST     = "chat_list"
    const val QUIZ          = "quiz"
    const val PROFILE       = "profile"
    const val SETTINGS      = "settings"
    const val NOTIFICATIONS = "notifications"
    const val WHO_VIEWED    = "who_viewed"
    const val KUNDLI        = "kundli"
    const val STORIES       = "stories"
    const val PRICING       = "pricing"
    const val FAMILY        = "family"
    const val REGIONS       = "regions"
    const val CIRCLES       = "circles"
    const val VERIFICATION  = "verification"
    const val HELP          = "help"
    const val TERMS         = "terms"
    const val PRIVACY       = "privacy"
    const val GUIDELINES    = "guidelines"
    const val SECURITY_PAGE = "security"
    const val REFUNDS       = "refunds"
    const val BIODATA       = "biodata"
    const val COMPAT_QUIZ   = "compat_quiz"
    const val SECOND_MARRIAGE = "second_marriage"
    const val ASSISTED      = "assisted"
    const val VIRTUAL_MEET  = "virtual_meet"
    const val BIO_GEN       = "bio_gen"
    const val PHOTO_EDITOR  = "photo_editor"
    const val COUNSELLING   = "counselling"
    const val GUIDES        = "guides"
    const val DETAIL        = "detail/{userId}"
    const val CHAT          = "chat/{peerId}"
    const val PROFILE_BOOST = "profile_boost"
    const val VIDEO_PROFILE = "video_profile"
    const val RECENTLY_JOINED = "recently_joined"
    const val TESTIMONIALS = "testimonials"
    const val SWIPE_DISCOVER = "swipe_discover"
    const val COMMUNITY_BROWSE = "community_browse"
    const val LIVE_EVENTS = "live_events"
    const val BG_CHECK = "bg_check"
    const val SECURE_CALL = "secure_call"
    const val PRIVACY_DASH = "privacy_dash"
    const val AI_INSIGHTS = "ai_insights"
    const val ANALYTICS = "analytics"
    const val WEDDING_PLANNER = "wedding_planner"
    const val ADV_HOROSCOPE = "adv_horoscope"
    const val DAILY_REWARDS = "daily_rewards"
    const val NEARBY = "nearby"
    const val NRI_MATCH = "nri_match"
    const val SAFETY_CENTER = "safety_center"
    const val TIMELINE = "timeline"
    const val REFERRAL = "referral"
    const val MUHURAT = "muhurat"
    const val DEEP_COMPAT = "deep_compat"
    const val LANGUAGE_SELECT = "language_select"
    const val PHONE_VERIF = "phone_verif"
    const val PROFILE_WIZARD = "profile_wizard"
    fun detail(userId: Long) = "detail/$userId"
    fun chat(peerId: Long)   = "chat/$peerId"
}

// ── Bottom nav tabs ─────────────────────────────────────────────────────────
private sealed class Tab(val route: String, val label: String, val icon: ImageVector, val tag: String) {
    data object Home      : Tab(MainRoutes.HOME,      "Home",     Icons.Filled.Home,      "tab_home")
    data object Matches   : Tab(MainRoutes.MATCHES,   "Matches",  Icons.Filled.Favorite,  "tab_matches")
    data object Interests : Tab(MainRoutes.INTERESTS, "Interests",Icons.AutoMirrored.Filled.Send, "tab_interests")
    data object Messages  : Tab(MainRoutes.CHAT_LIST, "Messages", Icons.Filled.Forum,     "tab_messages")
    data object Profile   : Tab(MainRoutes.PROFILE,   "Profile",  Icons.Filled.Person,    "tab_profile")
}
private val TABS = listOf(Tab.Home, Tab.Matches, Tab.Interests, Tab.Messages, Tab.Profile)

// ── Drawer nav item model ────────────────────────────────────────────────────
private data class DrawerItem(
    val route: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: Int = 0
)

private data class DrawerSection(val title: String, val items: List<DrawerItem>)

// ── ViewModel ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val session: SessionStore,
    private val notifRepo: NotificationRepository,
    private val messageDao: MessageDao
) : ViewModel() {
    val unreadNotifications: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { notifRepo.observeUnreadCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val unreadMessages: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { messageDao.observeUnread(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}

// ── Drawer content ───────────────────────────────────────────────────────────
@Composable
private fun AppDrawer(
    currentRoute: String?,
    unreadNotif: Int,
    unreadMsg: Int,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val sections = listOf(
        DrawerSection("DISCOVER", listOf(
            DrawerItem(MainRoutes.HOME,      "Home",          "Activity snapshot",           Icons.Filled.Home),
            DrawerItem(MainRoutes.MATCHES,   "Matches",       "Daily recommendations",       Icons.Filled.Favorite),
            DrawerItem(MainRoutes.INTERESTS, "Interests",     "Sent & received interests",   Icons.AutoMirrored.Filled.Send, 0),
            DrawerItem(MainRoutes.SHORTLISTS,"Shortlists",    "Saved favourites",            Icons.Filled.Bookmark),
            DrawerItem(MainRoutes.WHO_VIEWED,"Who Viewed",    "Recent profile visitors",     Icons.Filled.RemoveRedEye),
            DrawerItem(MainRoutes.REGIONS,   "Regions",       "Regional & community presets",Icons.Filled.Map),
        )),
        DrawerSection("CONNECT", listOf(
            DrawerItem(MainRoutes.CHAT_LIST,  "Messages",      "Conversations with matches",  Icons.Filled.Forum, unreadMsg),
            DrawerItem(MainRoutes.NOTIFICATIONS,"Notifications","Realtime alerts & updates",  Icons.Filled.Notifications, unreadNotif),
            DrawerItem(MainRoutes.STORIES,    "Success Stories","Community wins & inspiration",Icons.Filled.AutoAwesome),
            DrawerItem(MainRoutes.CIRCLES,    "Circles",       "Community groups",            Icons.Filled.Groups),
        )),
        DrawerSection("ACCOUNT", listOf(
            DrawerItem(MainRoutes.PROFILE,    "Profile",       "Personal & partner prefs",    Icons.Filled.Person),
            DrawerItem(MainRoutes.SETTINGS,   "Settings",      "Privacy & account controls",  Icons.Filled.Settings),
            DrawerItem(MainRoutes.FAMILY,     "Family",        "Family approvals & guardians",Icons.Filled.FamilyRestroom),
            DrawerItem(MainRoutes.VERIFICATION,"Verification", "Trust & identity checks",     Icons.Filled.Verified),
            DrawerItem(MainRoutes.PRICING,    "Pricing",       "Membership & plans",          Icons.Filled.Star),
            DrawerItem(MainRoutes.HELP,       "Help",          "Support & guidance",          Icons.AutoMirrored.Filled.Help),
        )),
        DrawerSection("TOOLS", listOf(
            DrawerItem(MainRoutes.KUNDLI,         "Kundli",          "Astrology compatibility",      Icons.Filled.AutoAwesome),
            DrawerItem(MainRoutes.QUIZ,           "Questionnaire",   "Answer match questions",       Icons.AutoMirrored.Filled.ListAlt),
            DrawerItem(MainRoutes.BIODATA,        "Biodata Maker",   "Free matrimonial biodata",     Icons.Filled.Description),
            DrawerItem(MainRoutes.COMPAT_QUIZ,    "Compat. Quiz",    "Find your personality type",   Icons.Filled.Psychology),
            DrawerItem(MainRoutes.SECOND_MARRIAGE,"Second Marriage",  "Begin your new chapter",      Icons.Filled.Favorite),
            DrawerItem(MainRoutes.ASSISTED,       "Assisted RM",     "Expert matchmaking service",   Icons.Filled.SupportAgent),
            DrawerItem(MainRoutes.VIRTUAL_MEET,    "Virtual Meet",    "Private video call, no numbers",Icons.Filled.VideoCall),
            DrawerItem(MainRoutes.BIO_GEN,         "Bio Generator",   "AI-written matrimonial bio",    Icons.Filled.AutoAwesome),
            DrawerItem(MainRoutes.PHOTO_EDITOR,    "Photo Editor",    "Crop, filters & watermark",     Icons.Filled.PhotoCamera),
            DrawerItem(MainRoutes.COUNSELLING,     "Counselling",     "Talk to a certified counsellor",Icons.Filled.SupportAgent),
            DrawerItem(MainRoutes.GUIDES,          "Guides",          "Articles & expert advice",      Icons.AutoMirrored.Filled.MenuBook),
        )),
        DrawerSection("LEGAL", listOf(
            DrawerItem(MainRoutes.TERMS,      "Terms",         "Terms of service",            Icons.Filled.Gavel),
            DrawerItem(MainRoutes.PRIVACY,    "Privacy",       "Privacy policy",              Icons.Filled.PrivacyTip),
            DrawerItem(MainRoutes.GUIDELINES, "Guidelines",    "Community guidelines",        Icons.Filled.Shield),
            DrawerItem(MainRoutes.SECURITY_PAGE,"Security",    "Security practices",          Icons.Filled.Lock),
            DrawerItem(MainRoutes.REFUNDS,    "Refunds",       "Refund policy",               Icons.Filled.Receipt),
        )),
    )

    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)) {
        // ── Drawer header ──────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("MatchApp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("All pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // ── Sections ──────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            sections.forEach { section ->
                item {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                items(section.items) { item ->
                    val selected = currentRoute == item.route
                    DrawerNavItem(
                        item = item,
                        selected = selected,
                        onClick = {
                            onNavigate(item.route)
                            onClose()
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DrawerNavItem(item: DrawerItem, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("drawer_${item.route}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(badge = {
            if (item.badge > 0) Badge { Text("${item.badge}") }
        }) {
            Icon(item.icon, contentDescription = item.label,
                modifier = Modifier.size(22.dp), tint = fg)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = fg)
            Text(item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Shell ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(vm: MainShellViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val currentRoute = current?.route

    // ── Consume pending deep link from notification tap ─────────────────────
    val activity = LocalContext.current as? MainActivity
    LaunchedEffect(Unit) {
        activity?.consumeDeepLink()?.let { dest ->
            nav.navigate(dest) { launchSingleTop = true }
        }
    }
    val showBottomBar = currentRoute in setOf(
        MainRoutes.HOME, MainRoutes.MATCHES, MainRoutes.INTERESTS,
        MainRoutes.CHAT_LIST, MainRoutes.PROFILE
    )
    val unreadNotif by vm.unreadNotifications.collectAsState()
    val unreadMsg   by vm.unreadMessages.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun openDrawer()  = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute   = currentRoute,
                unreadNotif    = unreadNotif,
                unreadMsg      = unreadMsg,
                onNavigate     = { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onClose        = { closeDrawer() }
            )
        }
    ) {
        Scaffold(
            topBar = {
                // Show top bar only on the 5 main tabs
                if (showBottomBar) {
                    val pageTitle = when (currentRoute) {
                        MainRoutes.HOME      -> "MatchApp"
                        MainRoutes.MATCHES   -> "Browse Profiles"
                        MainRoutes.INTERESTS -> "Interests"
                        MainRoutes.CHAT_LIST -> "Messages"
                        MainRoutes.PROFILE   -> "My Profile"
                        else                 -> "MatchApp"
                    }
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF5A7D), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(pageTitle, fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { openDrawer() },
                                modifier = Modifier.testTag("hamburger_btn")
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "All pages")
                            }
                        },
                        actions = {
                            // Quick Help / Support
                            IconButton(onClick = { nav.navigate(MainRoutes.HELP) }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                            BadgedBox(badge = {
                                if (unreadNotif > 0) Badge { Text("$unreadNotif") }
                            }) {
                                IconButton(onClick = { nav.navigate(MainRoutes.NOTIFICATIONS) }) {
                                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (currentRoute == MainRoutes.MATCHES) {
                    ExtendedFloatingActionButton(
                        onClick = { nav.navigate(MainRoutes.DAILY_REWARDS) },
                        icon = { Icon(Icons.Filled.Stars, null) },
                        text = { Text("Earn Coins") },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.testTag("engagement_fab")
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(modifier = Modifier.testTag("bottom_bar")) {
                        TABS.forEach { t ->
                            val selected = current?.hierarchy?.any { it.route == t.route } == true
                            NavigationBarItem(
                                modifier = Modifier.testTag(t.tag),
                                selected = selected,
                                onClick = {
                                    nav.navigate(t.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true; restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(badge = {
                                        val badge = when (t) {
                                            Tab.Profile  -> if (unreadNotif > 0) unreadNotif else 0
                                            Tab.Messages -> unreadMsg
                                            else         -> 0
                                        }
                                        if (badge > 0) Badge { Text("$badge") }
                                    }) { Icon(t.icon, contentDescription = t.label) }
                                },
                                label = { Text(t.label) }
                            )
                        }
                    }
                }
            }
        ) { pad ->
            NavHost(
                navController = nav,
                startDestination = MainRoutes.HOME,
                modifier = Modifier.fillMaxSize().padding(pad)
            ) {
                composable(MainRoutes.HOME) {
                    HomeLauncherScreen(
                        onGoMatches       = { nav.navigate(MainRoutes.MATCHES) },
                        onGoQuiz          = { nav.navigate(MainRoutes.QUIZ) },
                        onGoStories       = { nav.navigate(MainRoutes.STORIES) },
                        onGoPricing       = { nav.navigate(MainRoutes.PRICING) },
                        onGoInterests     = { nav.navigate(MainRoutes.INTERESTS) },
                        onGoNotifications = { nav.navigate(MainRoutes.NOTIFICATIONS) },
                        onGoShortlists    = { nav.navigate(MainRoutes.SHORTLISTS) },
                        onGoRegions       = { nav.navigate(MainRoutes.REGIONS) },
                        onGoCircles       = { nav.navigate(MainRoutes.CIRCLES) },
                        onGoMessages      = { nav.navigate(MainRoutes.CHAT_LIST) },
                        onGoProfile       = { nav.navigate(MainRoutes.PROFILE) },
                        onGoVerification  = { nav.navigate(MainRoutes.VERIFICATION) },
                        onGoKundli        = { nav.navigate(MainRoutes.KUNDLI) },
                        onGoWhoViewed     = { nav.navigate(MainRoutes.WHO_VIEWED) },
                        onGoFamily        = { nav.navigate(MainRoutes.FAMILY) },
                        onGoHelp          = { nav.navigate(MainRoutes.HELP) },
                        onGoBiodata       = { nav.navigate(MainRoutes.BIODATA) },
                        onGoSecondMarriage    = { nav.navigate(MainRoutes.SECOND_MARRIAGE) },
                        onGoCompatibilityQuiz = { nav.navigate(MainRoutes.COMPAT_QUIZ) },
                        onGoAssisted          = { nav.navigate(MainRoutes.ASSISTED) },
                        onGoVirtualMeet       = { nav.navigate(MainRoutes.VIRTUAL_MEET) },
                        onGoBioGen            = { nav.navigate(MainRoutes.BIO_GEN) },
                        onGoPhotoEditor       = { nav.navigate(MainRoutes.PHOTO_EDITOR) },
                        onGoCounselling       = { nav.navigate(MainRoutes.COUNSELLING) },
                        onGoGuides            = { nav.navigate(MainRoutes.GUIDES) },
                        onGoBoost             = { nav.navigate(MainRoutes.PROFILE_BOOST) },
                        onGoVideoProfile      = { nav.navigate(MainRoutes.VIDEO_PROFILE) },
                        onGoRecentlyJoined    = { nav.navigate(MainRoutes.RECENTLY_JOINED) },
                        onGoTestimonials      = { nav.navigate(MainRoutes.TESTIMONIALS) },
                        onGoSwipeDiscover     = { nav.navigate(MainRoutes.SWIPE_DISCOVER) },
                        onGoCommunityBrowse   = { nav.navigate(MainRoutes.COMMUNITY_BROWSE) },
                        onGoLiveEvents        = { nav.navigate(MainRoutes.LIVE_EVENTS) },
                        onGoBgCheck           = { nav.navigate(MainRoutes.BG_CHECK) },
                        onGoSecureCall        = { nav.navigate(MainRoutes.SECURE_CALL) },
                        onGoPrivacyDash       = { nav.navigate(MainRoutes.PRIVACY_DASH) },
                        onGoAIInsights        = { nav.navigate(MainRoutes.AI_INSIGHTS) },
                        onGoAnalytics         = { nav.navigate(MainRoutes.ANALYTICS) },
                        onGoWeddingPlanner    = { nav.navigate(MainRoutes.WEDDING_PLANNER) },
                        onGoAdvHoroscope      = { nav.navigate(MainRoutes.ADV_HOROSCOPE) },
                        onGoDailyRewards      = { nav.navigate(MainRoutes.DAILY_REWARDS) },
                        onGoNearby            = { nav.navigate(MainRoutes.NEARBY) },
                        onGoNRIMatch          = { nav.navigate(MainRoutes.NRI_MATCH) },
                        onGoSafetyCenter      = { nav.navigate(MainRoutes.SAFETY_CENTER) },
                        onGoTimeline          = { nav.navigate(MainRoutes.TIMELINE) },
                        onGoReferral          = { nav.navigate(MainRoutes.REFERRAL) },
                        onGoMuhurat           = { nav.navigate(MainRoutes.MUHURAT) },
                        onGoDeepCompat        = { nav.navigate(MainRoutes.DEEP_COMPAT) },
                        onGoWizard            = { nav.navigate(MainRoutes.PROFILE_WIZARD) },
                        onOpenProfile         = { nav.navigate(MainRoutes.detail(it)) }
                    )
                }
                composable(MainRoutes.MATCHES) {
                    MatchesScreen(onOpen = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.LIKES) {
                    LikesScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat    = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.INTERESTS) {
                    InterestsScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat    = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.SHORTLISTS) {
                    ShortlistScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat    = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.CHAT_LIST) {
                    ChatListScreen(onOpenChat = { nav.navigate(MainRoutes.chat(it)) })
                }
                composable(MainRoutes.QUIZ) { QuestionnaireScreen() }
                composable(MainRoutes.PROFILE) {
                    ProfileScreen(
                        onGoSettings      = { nav.navigate(MainRoutes.SETTINGS) },
                        onGoNotifications = { nav.navigate(MainRoutes.NOTIFICATIONS) },
                        onGoWhoViewed     = { nav.navigate(MainRoutes.WHO_VIEWED) },
                        onGoShortlists    = { nav.navigate(MainRoutes.SHORTLISTS) },
                        onGoKundli        = { nav.navigate(MainRoutes.KUNDLI) },
                        onGoPricing       = { nav.navigate(MainRoutes.PRICING) },
                        onGoStories       = { nav.navigate(MainRoutes.STORIES) },
                        onGoFamily        = { nav.navigate(MainRoutes.FAMILY) },
                        onGoInterests     = { nav.navigate(MainRoutes.INTERESTS) },
                        onGoVerification  = { nav.navigate(MainRoutes.VERIFICATION) },
                        onGoHelp          = { nav.navigate(MainRoutes.HELP) },
                        onGoTerms         = { nav.navigate(MainRoutes.TERMS) },
                        onGoPrivacy       = { nav.navigate(MainRoutes.PRIVACY) },
                        onGoGuidelines    = { nav.navigate(MainRoutes.GUIDELINES) },
                        onGoBiodata       = { nav.navigate(MainRoutes.BIODATA) },
                        unreadNotif       = unreadNotif
                    )
                }
                composable(MainRoutes.SETTINGS)      { SettingsScreen(onBack = { nav.popBackStack() }, onGoLanguage = { nav.navigate(MainRoutes.LANGUAGE_SELECT) }, onUpgrade = { nav.navigate(MainRoutes.PRICING) }) }
                composable(MainRoutes.LANGUAGE_SELECT) { com.match.app.ui.language.LanguageSelectionScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.NOTIFICATIONS) {
                    NotificationsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenProfile = { uid -> nav.navigate(MainRoutes.detail(uid)) },
                        onOpenChat = { peerId -> nav.navigate(MainRoutes.chat(peerId)) }
                    )
                }
                composable(MainRoutes.WHO_VIEWED)    { WhoViewedScreen(onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }, onBack = { nav.popBackStack() }, onUpgrade = { nav.navigate(MainRoutes.PRICING) }) }
                composable(MainRoutes.KUNDLI)        { KundliScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.STORIES)       { SuccessStoriesScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRICING)       { PricingScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.FAMILY)        { FamilyScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REGIONS) {
                    RegionsScreen(
                        onBack      = { nav.popBackStack() },
                        onGoMatches = { nav.navigate(MainRoutes.MATCHES) },
                        onOpenProfile = { uid -> nav.navigate(MainRoutes.detail(uid)) }
                    )
                }
                composable(MainRoutes.CIRCLES) {
                    CirclesScreen(
                        onBack      = { nav.popBackStack() },
                        onGoMatches = { nav.navigate(MainRoutes.MATCHES) },
                        onOpenProfile = { uid -> nav.navigate(MainRoutes.detail(uid)) }
                    )
                }
                composable(MainRoutes.VERIFICATION)  { VerificationScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.HELP)          { HelpScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TERMS)         { LegalScreen(type = "terms",      onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY)       { LegalScreen(type = "privacy",    onBack = { nav.popBackStack() }) }
                composable(MainRoutes.GUIDELINES)    { LegalScreen(type = "guidelines", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECURITY_PAGE) { LegalScreen(type = "security",   onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REFUNDS)       { LegalScreen(type = "refunds",    onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BIODATA)         { BiodataScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.COMPAT_QUIZ)     { CompatibilityQuizScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECOND_MARRIAGE) {
                    SecondMarriageScreen(
                        onBack = { nav.popBackStack() },
                        onOpenProfile = { uid -> nav.navigate(MainRoutes.detail(uid)) }
                    )
                }
                composable(MainRoutes.ASSISTED)        { AssistedServiceScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.VIRTUAL_MEET)     { VirtualMeetScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BIO_GEN)          { BioGeneratorScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PHOTO_EDITOR)     { PhotoEditorScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.COUNSELLING)      { CounsellingScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.GUIDES)           { GuidesScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PROFILE_BOOST)    { ProfileBoostScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.VIDEO_PROFILE)   { VideoProfileScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.RECENTLY_JOINED) { RecentlyJoinedScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.TESTIMONIALS)    { TestimonialsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SWIPE_DISCOVER)  { SwipeDiscoveryScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.COMMUNITY_BROWSE) { CommunityBrowseScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.LIVE_EVENTS)     { LiveEventsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BG_CHECK)        { BackgroundCheckScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECURE_CALL)     { SecureCallScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY_DASH)    { PrivacyDashboardScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.AI_INSIGHTS)     { AIMatchInsightsScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.ANALYTICS)       { ProfileAnalyticsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.WEDDING_PLANNER) { WeddingPlannerScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.ADV_HOROSCOPE)   { AdvancedHoroscopeScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.DAILY_REWARDS)   { DailyRewardsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.NEARBY)          { NearbyMatchesScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.NRI_MATCH)       { NRIMatchScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SAFETY_CENTER)   { SafetyCenterScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TIMELINE)        { RelationshipTimelineScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REFERRAL)        { MatchmakerReferralScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.MUHURAT)         { AstroCalendarScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.DEEP_COMPAT)     { CompatibilityDeepDiveScreen(onBack = { nav.popBackStack() }, onUpgrade = { nav.navigate(MainRoutes.PRICING) }) }
                composable(MainRoutes.PHONE_VERIF)     { PhoneVerificationScreen(onBack = { nav.popBackStack() }, onVerified = { nav.popBackStack() }) }
                composable(MainRoutes.PROFILE_WIZARD)  { com.match.app.ui.onboarding.ProfileWizardScreen(onComplete = { nav.popBackStack() }) }
                composable(
                    MainRoutes.DETAIL,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
                ) { back ->
                    val userId = back.arguments?.getLong("userId") ?: return@composable
                    MatchDetailScreen(
                        userId = userId,
                        onBack = { nav.popBackStack() },
                        onChat = { nav.navigate(MainRoutes.chat(userId)) }
                    )
                }
                composable(
                    MainRoutes.CHAT,
                    arguments = listOf(navArgument("peerId") { type = NavType.LongType })
                ) { back ->
                    val peerId = back.arguments?.getLong("peerId") ?: return@composable
                    ChatScreen(peerId = peerId, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
