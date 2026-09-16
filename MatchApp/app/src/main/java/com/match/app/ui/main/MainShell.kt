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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.match.app.ui.aiinsights.AIMatchInsightsScreen
import com.match.app.ui.analytics.ProfileAnalyticsScreen
import com.match.app.ui.assisted.AssistedServiceScreen
import com.match.app.ui.bgcheck.BackgroundCheckScreen
import com.match.app.ui.biodata.BiodataScreen
import com.match.app.ui.biogen.BioGeneratorScreen
import com.match.app.ui.boost.ProfileBoostScreen
import com.match.app.ui.chat.ChatListScreen
import com.match.app.ui.chat.ChatScreen
import com.match.app.ui.circles.CirclesScreen
import com.match.app.ui.community.CommunityBrowseScreen
import com.match.app.ui.counselling.CounsellingScreen
import com.match.app.ui.deepcompat.CompatibilityDeepDiveScreen
import com.match.app.ui.detail.MatchDetailScreen
import com.match.app.ui.discovery.SwipeDiscoveryScreen
import com.match.app.ui.events.LiveEventsScreen
import com.match.app.ui.family.FamilyScreen
import com.match.app.ui.guides.GuidesScreen
import com.match.app.ui.help.HelpScreen
import com.match.app.ui.horoscope.AdvancedHoroscopeScreen
import com.match.app.ui.interests.InterestsScreen
import com.match.app.ui.kundli.KundliScreen
import com.match.app.ui.legal.LegalScreen
import com.match.app.ui.likes.LikesScreen
import com.match.app.ui.matches.MatchesScreen
import com.match.app.ui.meet.VirtualMeetScreen
import com.match.app.ui.muhurat.AstroCalendarScreen
import com.match.app.ui.nearby.NearbyMatchesScreen
import com.match.app.ui.notifications.NotificationsScreen
import com.match.app.ui.nri.NRIMatchScreen
import com.match.app.ui.photoeditor.PhotoEditorScreen
import com.match.app.ui.pricing.PricingScreen
import com.match.app.ui.privacy.PrivacyDashboardScreen
import com.match.app.ui.profile.ProfileScreen
import com.match.app.ui.questionnaire.QuestionnaireScreen
import com.match.app.ui.quiz.CompatibilityQuizScreen
import com.match.app.ui.recentlyjoined.RecentlyJoinedScreen
import com.match.app.ui.referral.MatchmakerReferralScreen
import com.match.app.ui.regions.RegionsScreen
import com.match.app.ui.rewards.DailyRewardsScreen
import com.match.app.ui.safety.SafetyCenterScreen
import com.match.app.ui.secondmarriage.SecondMarriageScreen
import com.match.app.ui.securecall.SecureCallScreen
import com.match.app.ui.settings.SettingsScreen
import com.match.app.ui.shortlist.ShortlistScreen
import com.match.app.ui.stories.SuccessStoriesScreen
import com.match.app.ui.testimonials.TestimonialsScreen
import com.match.app.ui.timeline.RelationshipTimelineScreen
import com.match.app.ui.verification.VerificationScreen
import com.match.app.ui.videoprofile.VideoProfileScreen
import com.match.app.ui.wedding.WeddingPlannerScreen
import com.match.app.ui.whoviewed.WhoViewedScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Single source of truth for every production-reachable main-app destination. */
object MainRoutes {
    const val HOME = "home"
    const val MATCHES = "matches"
    const val NEARBY = "nearby"
    const val INTERESTS = "interests"
    const val SHORTLISTS = "shortlists"
    const val CHAT_LIST = "chat_list"
    const val QUIZ = "quiz"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val WHO_VIEWED = "who_viewed"
    const val KUNDLI = "kundli"
    const val KUNDLI_PAIR = "kundli/{targetId}"
    const val PRICING = "pricing"
    const val VERIFICATION = "verification"
    const val HELP = "help"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"
    const val GUIDELINES = "guidelines"
    const val SECURITY_PAGE = "security"
    const val REFUNDS = "refunds"
    const val BIODATA = "biodata"
    const val PRIVACY_DASH = "privacy_dash"
    const val LANGUAGE_SELECT = "language_select"
    const val DETAIL = "detail/{userId}"
    const val CHAT = "chat/{peerId}"

    const val FEATURE_HUB = "feature_hub"
    const val AI_INSIGHTS = "ai_insights"
    const val PROFILE_ANALYTICS = "profile_analytics"
    const val ASSISTED = "assisted"
    const val BACKGROUND_CHECK = "background_check"
    const val BIO_GENERATOR = "bio_generator"
    const val PROFILE_BOOST = "profile_boost"
    const val CIRCLES = "circles"
    const val COMMUNITY_BROWSE = "community_browse"
    const val COUNSELLING = "counselling"
    const val COMPAT_QUIZ = "compat_quiz"
    const val DEEP_COMPAT = "deep_compat/{candidateId}"
    const val SWIPE_DISCOVER = "swipe_discover"
    const val LIVE_EVENTS = "live_events"
    const val FAMILY = "family"
    const val GUIDES = "guides"
    const val ADV_HOROSCOPE = "advanced_horoscope"
    const val LIKES = "likes"
    const val VIRTUAL_MEET = "virtual_meet"
    const val MUHURAT = "muhurat"
    const val NRI_MATCH = "nri_match"
    const val PHOTO_EDITOR = "photo_editor"
    const val RECENTLY_JOINED = "recently_joined"
    const val REFERRAL = "referral"
    const val REGIONS = "regions"
    const val REWARDS = "rewards"
    const val SAFETY_CENTER = "safety_center"
    const val SECOND_MARRIAGE = "second_marriage"
    const val SECURE_CALL = "secure_call"
    const val STORIES = "stories"
    const val TESTIMONIALS = "testimonials"
    const val TIMELINE = "timeline"
    const val VIDEO_PROFILE = "video_profile"
    const val WEDDING_PLANNER = "wedding_planner"

    fun detail(userId: Long) = "detail/$userId"
    fun chat(peerId: Long) = "chat/$peerId"
    fun kundli(targetId: Long) = "kundli/$targetId"
    fun deepCompat(candidateId: Long) = "deep_compat/$candidateId"
}

private sealed class Tab(val route: String, val label: String, val icon: ImageVector, val tag: String) {
    data object Home : Tab(MainRoutes.HOME, "Home", Icons.Filled.Home, "tab_home")
    data object Matches : Tab(MainRoutes.MATCHES, "Matches", Icons.Filled.Favorite, "tab_matches")
    data object Interests : Tab(MainRoutes.INTERESTS, "Interests", Icons.AutoMirrored.Filled.Send, "tab_interests")
    data object Messages : Tab(MainRoutes.CHAT_LIST, "Messages", Icons.Filled.Forum, "tab_messages")
    data object Profile : Tab(MainRoutes.PROFILE, "Profile", Icons.Filled.Person, "tab_profile")
}

private val TABS = listOf(Tab.Home, Tab.Matches, Tab.Interests, Tab.Messages, Tab.Profile)

private data class DrawerItem(
    val route: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: Int = 0
)

private data class DrawerSection(val title: String, val items: List<DrawerItem>)

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

@Composable
private fun AppDrawer(
    currentRoute: String?,
    unreadNotif: Int,
    unreadMsg: Int,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val sections = listOf(
        DrawerSection("MATCH", listOf(
            DrawerItem(MainRoutes.HOME, "Home", "Your activity", Icons.Filled.Home),
            DrawerItem(MainRoutes.MATCHES, "Discover", "Browse compatible profiles", Icons.Filled.Search),
            DrawerItem(MainRoutes.NEARBY, "Nearby", "Profiles near your shared location", Icons.Filled.LocationOn),
            DrawerItem(MainRoutes.INTERESTS, "Interests", "Sent and received interests", Icons.AutoMirrored.Filled.Send),
            DrawerItem(MainRoutes.SHORTLISTS, "Shortlist", "Profiles you saved", Icons.Filled.Bookmark),
            DrawerItem(MainRoutes.WHO_VIEWED, "Who viewed", "Recent profile visitors", Icons.Filled.RemoveRedEye),
            DrawerItem(MainRoutes.LIKES, "Mutual matches", "People where interest is mutual", Icons.Filled.Favorite)
        )),
        DrawerSection("EXPLORE", listOf(
            DrawerItem(MainRoutes.FEATURE_HUB, "Explore Matree", "All discovery, profile and journey tools", Icons.Filled.Explore),
            DrawerItem(MainRoutes.REGIONS, "Regions & community", "Browse by place, language and community", Icons.Filled.Public),
            DrawerItem(MainRoutes.CIRCLES, "Circles", "Curated discovery circles", Icons.Filled.Groups),
            DrawerItem(MainRoutes.RECENTLY_JOINED, "Recently joined", "Newer member profiles", Icons.Filled.FiberNew)
        )),
        DrawerSection("CONNECT", listOf(
            DrawerItem(MainRoutes.CHAT_LIST, "Messages", "Mutual-match conversations", Icons.Filled.Forum, unreadMsg),
            DrawerItem(MainRoutes.NOTIFICATIONS, "Notifications", "Account and match updates", Icons.Filled.Notifications, unreadNotif),
            DrawerItem(MainRoutes.VIRTUAL_MEET, "Virtual meet", "Private video meeting tools", Icons.Filled.VideoCall),
            DrawerItem(MainRoutes.SECURE_CALL, "Secure calls", "Call without sharing your number", Icons.Filled.Phone)
        )),
        DrawerSection("ACCOUNT", listOf(
            DrawerItem(MainRoutes.PROFILE, "Profile", "Your matrimonial profile", Icons.Filled.Person),
            DrawerItem(MainRoutes.VERIFICATION, "Verification", "Identity verification", Icons.Filled.Verified),
            DrawerItem(MainRoutes.PRIVACY_DASH, "Privacy", "Visibility and account privacy", Icons.Filled.PrivacyTip),
            DrawerItem(MainRoutes.SAFETY_CENTER, "Safety center", "Safety guidance and controls", Icons.Filled.HealthAndSafety),
            DrawerItem(MainRoutes.PRICING, "Membership", "Google Play membership plans", Icons.Filled.WorkspacePremium),
            DrawerItem(MainRoutes.SETTINGS, "Settings", "Language, security and account", Icons.Filled.Settings),
            DrawerItem(MainRoutes.HELP, "Help", "Support and guidance", Icons.AutoMirrored.Filled.Help)
        )),
        DrawerSection("COMPATIBILITY", listOf(
            DrawerItem(MainRoutes.QUIZ, "Questionnaire", "Values and partner preferences", Icons.AutoMirrored.Filled.ListAlt),
            DrawerItem(MainRoutes.COMPAT_QUIZ, "Compatibility quiz", "Lifestyle and values reflection", Icons.Filled.Quiz),
            DrawerItem(MainRoutes.KUNDLI, "Kundali", "Astrology when applicable", Icons.Filled.AutoAwesome),
            DrawerItem(MainRoutes.AI_INSIGHTS, "Match insights", "Explain matching signals", Icons.Filled.Psychology)
        )),
        DrawerSection("LEGAL", listOf(
            DrawerItem(MainRoutes.TERMS, "Terms", "Terms of service", Icons.Filled.Gavel),
            DrawerItem(MainRoutes.PRIVACY, "Privacy policy", "How data is handled", Icons.Filled.Policy),
            DrawerItem(MainRoutes.GUIDELINES, "Guidelines", "Community standards", Icons.Filled.Shield),
            DrawerItem(MainRoutes.SECURITY_PAGE, "Security", "Security practices", Icons.Filled.Lock),
            DrawerItem(MainRoutes.REFUNDS, "Refunds", "Membership refund policy", Icons.Filled.Receipt)
        ))
    )

    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Matree", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Matrimony with clearer choices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close menu") }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
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
                    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val foreground = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier.fillMaxWidth().background(background).clickable {
                            onNavigate(item.route)
                            onClose()
                        }.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, null, tint = foreground, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = foreground)
                            Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.badge > 0) Badge { Text(item.badge.toString()) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(vm: MainShellViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val currentRoute = current?.route
    val unreadNotif by vm.unreadNotifications.collectAsState()
    val unreadMsg by vm.unreadMessages.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activity = LocalContext.current as? MainActivity
    LaunchedEffect(Unit) {
        activity?.consumeDeepLink()?.let { destination -> nav.navigate(destination) { launchSingleTop = true } }
    }

    val showBottomBar = currentRoute in setOf(
        MainRoutes.HOME, MainRoutes.MATCHES, MainRoutes.INTERESTS, MainRoutes.CHAT_LIST, MainRoutes.PROFILE
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                unreadNotif = unreadNotif,
                unreadMsg = unreadMsg,
                onNavigate = { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showBottomBar) {
                    val pageTitle = when (currentRoute) {
                        MainRoutes.HOME -> "Matree"
                        MainRoutes.MATCHES -> "Discover"
                        MainRoutes.INTERESTS -> "Interests"
                        MainRoutes.CHAT_LIST -> "Messages"
                        MainRoutes.PROFILE -> "My Profile"
                        else -> "Matree"
                    }
                    TopAppBar(
                        title = { Text(pageTitle, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.testTag("hamburger_btn")) {
                                Icon(Icons.Filled.Menu, "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { nav.navigate(MainRoutes.FEATURE_HUB) }) { Icon(Icons.Filled.Explore, "Explore Matree") }
                            IconButton(onClick = { nav.navigate(MainRoutes.HELP) }) { Icon(Icons.AutoMirrored.Filled.Help, "Help") }
                            BadgedBox(badge = { if (unreadNotif > 0) Badge { Text(unreadNotif.toString()) } }) {
                                IconButton(onClick = { nav.navigate(MainRoutes.NOTIFICATIONS) }) {
                                    Icon(Icons.Filled.Notifications, "Notifications")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(modifier = Modifier.testTag("bottom_bar")) {
                        TABS.forEach { tab ->
                            val selected = current?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                modifier = Modifier.testTag(tab.tag),
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(badge = {
                                        val badge = if (tab == Tab.Messages) unreadMsg else 0
                                        if (badge > 0) Badge { Text(badge.toString()) }
                                    }) { Icon(tab.icon, tab.label) }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = MainRoutes.HOME,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                composable(MainRoutes.HOME) {
                    HomeLauncherScreen(
                        onGoMatches = { nav.navigate(MainRoutes.MATCHES) },
                        onGoQuiz = { nav.navigate(MainRoutes.QUIZ) },
                        onGoStories = { nav.navigate(MainRoutes.STORIES) },
                        onGoPricing = { nav.navigate(MainRoutes.PRICING) },
                        onGoInterests = { nav.navigate(MainRoutes.INTERESTS) },
                        onGoNotifications = { nav.navigate(MainRoutes.NOTIFICATIONS) },
                        onGoShortlists = { nav.navigate(MainRoutes.SHORTLISTS) },
                        onGoRegions = { nav.navigate(MainRoutes.REGIONS) },
                        onGoCircles = { nav.navigate(MainRoutes.CIRCLES) },
                        onGoMessages = { nav.navigate(MainRoutes.CHAT_LIST) },
                        onGoProfile = { nav.navigate(MainRoutes.PROFILE) },
                        onGoVerification = { nav.navigate(MainRoutes.VERIFICATION) },
                        onGoKundli = { nav.navigate(MainRoutes.KUNDLI) },
                        onGoWhoViewed = { nav.navigate(MainRoutes.WHO_VIEWED) },
                        onGoFamily = { nav.navigate(MainRoutes.FAMILY) },
                        onGoHelp = { nav.navigate(MainRoutes.HELP) },
                        onGoFeatureHub = { nav.navigate(MainRoutes.FEATURE_HUB) },
                        onGoBiodata = { nav.navigate(MainRoutes.BIODATA) },
                        onGoSecondMarriage = { nav.navigate(MainRoutes.SECOND_MARRIAGE) },
                        onGoCompatibilityQuiz = { nav.navigate(MainRoutes.COMPAT_QUIZ) },
                        onGoAssisted = { nav.navigate(MainRoutes.ASSISTED) },
                        onGoVirtualMeet = { nav.navigate(MainRoutes.VIRTUAL_MEET) },
                        onGoBioGen = { nav.navigate(MainRoutes.BIO_GENERATOR) },
                        onGoPhotoEditor = { nav.navigate(MainRoutes.PHOTO_EDITOR) },
                        onGoCounselling = { nav.navigate(MainRoutes.COUNSELLING) },
                        onGoGuides = { nav.navigate(MainRoutes.GUIDES) },
                        onGoBoost = { nav.navigate(MainRoutes.PROFILE_BOOST) },
                        onGoVideoProfile = { nav.navigate(MainRoutes.VIDEO_PROFILE) },
                        onGoRecentlyJoined = { nav.navigate(MainRoutes.RECENTLY_JOINED) },
                        onGoTestimonials = { nav.navigate(MainRoutes.TESTIMONIALS) },
                        onGoSwipeDiscover = { nav.navigate(MainRoutes.SWIPE_DISCOVER) },
                        onGoCommunityBrowse = { nav.navigate(MainRoutes.COMMUNITY_BROWSE) },
                        onGoLiveEvents = { nav.navigate(MainRoutes.LIVE_EVENTS) },
                        onGoBgCheck = { nav.navigate(MainRoutes.BACKGROUND_CHECK) },
                        onGoSecureCall = { nav.navigate(MainRoutes.SECURE_CALL) },
                        onGoPrivacyDash = { nav.navigate(MainRoutes.PRIVACY_DASH) },
                        onGoAIInsights = { nav.navigate(MainRoutes.AI_INSIGHTS) },
                        onGoAnalytics = { nav.navigate(MainRoutes.PROFILE_ANALYTICS) },
                        onGoWeddingPlanner = { nav.navigate(MainRoutes.WEDDING_PLANNER) },
                        onGoAdvHoroscope = { nav.navigate(MainRoutes.ADV_HOROSCOPE) },
                        onGoDailyRewards = { nav.navigate(MainRoutes.REWARDS) },
                        onGoNearby = { nav.navigate(MainRoutes.NEARBY) },
                        onGoNRIMatch = { nav.navigate(MainRoutes.NRI_MATCH) },
                        onGoSafetyCenter = { nav.navigate(MainRoutes.SAFETY_CENTER) },
                        onGoTimeline = { nav.navigate(MainRoutes.TIMELINE) },
                        onGoReferral = { nav.navigate(MainRoutes.REFERRAL) },
                        onGoMuhurat = { nav.navigate(MainRoutes.MUHURAT) },
                        onGoDeepCompat = { nav.navigate(MainRoutes.MATCHES) },
                        onGoWizard = { nav.navigate(MainRoutes.PROFILE) },
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }
                    )
                }

                composable(MainRoutes.MATCHES) { MatchesScreen(onOpen = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.NEARBY) {
                    NearbyMatchesScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.INTERESTS) {
                    InterestsScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onCheckKundli = { nav.navigate(MainRoutes.kundli(it)) },
                        onOpenChat = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.SHORTLISTS) { ShortlistScreen(onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }) }
                composable(MainRoutes.CHAT_LIST) { ChatListScreen(onOpenChat = { nav.navigate(MainRoutes.chat(it)) }) }
                composable(MainRoutes.QUIZ) { QuestionnaireScreen() }
                composable(MainRoutes.PROFILE) {
                    ProfileScreen(
                        onGoSettings = { nav.navigate(MainRoutes.SETTINGS) },
                        onGoNotifications = { nav.navigate(MainRoutes.NOTIFICATIONS) },
                        onGoWhoViewed = { nav.navigate(MainRoutes.WHO_VIEWED) },
                        onGoShortlists = { nav.navigate(MainRoutes.SHORTLISTS) },
                        onGoKundli = { nav.navigate(MainRoutes.KUNDLI) },
                        onGoPricing = { nav.navigate(MainRoutes.PRICING) },
                        onGoInterests = { nav.navigate(MainRoutes.INTERESTS) },
                        onGoVerification = { nav.navigate(MainRoutes.VERIFICATION) },
                        onGoHelp = { nav.navigate(MainRoutes.HELP) },
                        onGoTerms = { nav.navigate(MainRoutes.TERMS) },
                        onGoPrivacy = { nav.navigate(MainRoutes.PRIVACY_DASH) },
                        onGoGuidelines = { nav.navigate(MainRoutes.GUIDELINES) },
                        onGoBiodata = { nav.navigate(MainRoutes.BIODATA) },
                        unreadNotif = unreadNotif
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    SettingsScreen(
                        onBack = { nav.popBackStack() },
                        onGoLanguage = { nav.navigate(MainRoutes.LANGUAGE_SELECT) },
                        onUpgrade = { nav.navigate(MainRoutes.PRICING) }
                    )
                }
                composable(MainRoutes.LANGUAGE_SELECT) {
                    com.match.app.ui.language.LanguageSelectionScreen(onBack = { nav.popBackStack() })
                }
                composable(MainRoutes.NOTIFICATIONS) {
                    NotificationsScreen(
                        onBack = { nav.popBackStack() },
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.WHO_VIEWED) {
                    WhoViewedScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onBack = { nav.popBackStack() },
                        onUpgrade = { nav.navigate(MainRoutes.PRICING) }
                    )
                }
                composable(MainRoutes.KUNDLI) { KundliScreen(onBack = { nav.popBackStack() }) }
                composable(
                    MainRoutes.KUNDLI_PAIR,
                    arguments = listOf(navArgument("targetId") { type = NavType.LongType })
                ) { backStack ->
                    val targetId = backStack.arguments?.getLong("targetId") ?: return@composable
                    KundliScreen(targetId = targetId, onBack = { nav.popBackStack() })
                }
                composable(MainRoutes.PRICING) { PricingScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.VERIFICATION) { VerificationScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY_DASH) {
                    PrivacyDashboardScreen(onBack = { nav.popBackStack() }, onGoSettings = { nav.navigate(MainRoutes.SETTINGS) })
                }
                composable(MainRoutes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TERMS) { LegalScreen(type = "terms", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY) { LegalScreen(type = "privacy", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.GUIDELINES) { LegalScreen(type = "guidelines", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECURITY_PAGE) { LegalScreen(type = "security", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REFUNDS) { LegalScreen(type = "refunds", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BIODATA) { BiodataScreen(onBack = { nav.popBackStack() }) }

                composable(MainRoutes.FEATURE_HUB) {
                    FeatureHubScreen(onBack = { nav.popBackStack() }, onNavigate = { nav.navigate(it) })
                }
                composable(MainRoutes.AI_INSIGHTS) {
                    AIMatchInsightsScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.PROFILE_ANALYTICS) { ProfileAnalyticsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.ASSISTED) { AssistedServiceScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BACKGROUND_CHECK) { BackgroundCheckScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BIO_GENERATOR) { BioGeneratorScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PROFILE_BOOST) { ProfileBoostScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.CIRCLES) {
                    CirclesScreen(
                        onBack = { nav.popBackStack() },
                        onGoMatches = { nav.navigate(MainRoutes.MATCHES) },
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }
                    )
                }
                composable(MainRoutes.COMMUNITY_BROWSE) {
                    CommunityBrowseScreen(
                        onBack = { nav.popBackStack() },
                        onBrowse = { _, _ -> nav.navigate(MainRoutes.MATCHES) }
                    )
                }
                composable(MainRoutes.COUNSELLING) { CounsellingScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.COMPAT_QUIZ) { CompatibilityQuizScreen(onBack = { nav.popBackStack() }) }
                composable(
                    MainRoutes.DEEP_COMPAT,
                    arguments = listOf(navArgument("candidateId") { type = NavType.LongType })
                ) { backStack ->
                    val candidateId = backStack.arguments?.getLong("candidateId") ?: return@composable
                    CompatibilityDeepDiveScreen(
                        candidateId = candidateId,
                        onBack = { nav.popBackStack() },
                        onUpgrade = { nav.navigate(MainRoutes.PRICING) }
                    )
                }
                composable(MainRoutes.SWIPE_DISCOVER) {
                    SwipeDiscoveryScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.LIVE_EVENTS) { LiveEventsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.FAMILY) { FamilyScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.GUIDES) { GuidesScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.ADV_HOROSCOPE) { AdvancedHoroscopeScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.LIKES) {
                    LikesScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.VIRTUAL_MEET) { VirtualMeetScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.MUHURAT) { AstroCalendarScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.NRI_MATCH) { NRIMatchScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PHOTO_EDITOR) { PhotoEditorScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.RECENTLY_JOINED) {
                    RecentlyJoinedScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.REFERRAL) { MatchmakerReferralScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REGIONS) {
                    RegionsScreen(
                        onBack = { nav.popBackStack() },
                        onGoMatches = { nav.navigate(MainRoutes.MATCHES) },
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }
                    )
                }
                composable(MainRoutes.REWARDS) { DailyRewardsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SAFETY_CENTER) { SafetyCenterScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECOND_MARRIAGE) {
                    SecondMarriageScreen(onBack = { nav.popBackStack() }, onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.SECURE_CALL) { SecureCallScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.STORIES) { SuccessStoriesScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TESTIMONIALS) { TestimonialsScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TIMELINE) { RelationshipTimelineScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.VIDEO_PROFILE) { VideoProfileScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.WEDDING_PLANNER) { WeddingPlannerScreen(onBack = { nav.popBackStack() }) }

                composable(MainRoutes.DETAIL, arguments = listOf(navArgument("userId") { type = NavType.LongType })) { backStack ->
                    val userId = backStack.arguments?.getLong("userId") ?: return@composable
                    MatchDetailScreen(
                        userId = userId,
                        onBack = { nav.popBackStack() },
                        onChat = { nav.navigate(MainRoutes.chat(userId)) },
                        onPricing = { nav.navigate(MainRoutes.PRICING) },
                        onKundli = { nav.navigate(MainRoutes.kundli(userId)) }
                    )
                }
                composable(MainRoutes.CHAT, arguments = listOf(navArgument("peerId") { type = NavType.LongType })) { backStack ->
                    val peerId = backStack.arguments?.getLong("peerId") ?: return@composable
                    ChatScreen(peerId = peerId, onBack = { nav.popBackStack() }, onUpgrade = { nav.navigate(MainRoutes.PRICING) })
                }
            }
        }
    }
}
