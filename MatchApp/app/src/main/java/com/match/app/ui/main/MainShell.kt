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
import com.match.app.ui.biodata.BiodataScreen
import com.match.app.ui.chat.ChatListScreen
import com.match.app.ui.chat.ChatScreen
import com.match.app.ui.detail.MatchDetailScreen
import com.match.app.ui.help.HelpScreen
import com.match.app.ui.interests.InterestsScreen
import com.match.app.ui.kundli.KundliScreen
import com.match.app.ui.legal.LegalScreen
import com.match.app.ui.matches.MatchesScreen
import com.match.app.ui.notifications.NotificationsScreen
import com.match.app.ui.pricing.PricingScreen
import com.match.app.ui.privacy.PrivacyDashboardScreen
import com.match.app.ui.profile.ProfileScreen
import com.match.app.ui.questionnaire.QuestionnaireScreen
import com.match.app.ui.settings.SettingsScreen
import com.match.app.ui.shortlist.ShortlistScreen
import com.match.app.ui.verification.VerificationScreen
import com.match.app.ui.whoviewed.WhoViewedScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Routes intentionally exposed by the production shell.
 * Experimental source files can remain in the repository without becoming user-visible promises.
 */
object MainRoutes {
    const val HOME = "home"
    const val MATCHES = "matches"
    const val INTERESTS = "interests"
    const val SHORTLISTS = "shortlists"
    const val CHAT_LIST = "chat_list"
    const val QUIZ = "quiz"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val WHO_VIEWED = "who_viewed"
    const val KUNDLI = "kundli"
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

    fun detail(userId: Long) = "detail/$userId"
    fun chat(peerId: Long) = "chat/$peerId"
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
            DrawerItem(MainRoutes.INTERESTS, "Interests", "Sent and received interests", Icons.AutoMirrored.Filled.Send),
            DrawerItem(MainRoutes.SHORTLISTS, "Shortlist", "Profiles you saved", Icons.Filled.Bookmark),
            DrawerItem(MainRoutes.WHO_VIEWED, "Who Viewed", "Recent profile visitors", Icons.Filled.RemoveRedEye)
        )),
        DrawerSection("CONNECT", listOf(
            DrawerItem(MainRoutes.CHAT_LIST, "Messages", "Mutual-match conversations", Icons.Filled.Forum, unreadMsg),
            DrawerItem(MainRoutes.NOTIFICATIONS, "Notifications", "Account and match updates", Icons.Filled.Notifications, unreadNotif)
        )),
        DrawerSection("ACCOUNT", listOf(
            DrawerItem(MainRoutes.PROFILE, "Profile", "Your matrimonial profile", Icons.Filled.Person),
            DrawerItem(MainRoutes.VERIFICATION, "Verification", "Identity verification", Icons.Filled.Verified),
            DrawerItem(MainRoutes.PRIVACY_DASH, "Privacy", "Visibility and account privacy", Icons.Filled.PrivacyTip),
            DrawerItem(MainRoutes.PRICING, "Membership", "Google Play membership plans", Icons.Filled.WorkspacePremium),
            DrawerItem(MainRoutes.SETTINGS, "Settings", "Language, security and account", Icons.Filled.Settings),
            DrawerItem(MainRoutes.HELP, "Help", "Support and guidance", Icons.AutoMirrored.Filled.Help)
        )),
        DrawerSection("COMPATIBILITY", listOf(
            DrawerItem(MainRoutes.QUIZ, "Questionnaire", "Values and partner preferences", Icons.AutoMirrored.Filled.ListAlt),
            DrawerItem(MainRoutes.KUNDLI, "Kundali", "Astrology when applicable", Icons.Filled.AutoAwesome)
        )),
        DrawerSection("LEGAL", listOf(
            DrawerItem(MainRoutes.TERMS, "Terms", "Terms of service", Icons.Filled.Gavel),
            DrawerItem(MainRoutes.PRIVACY, "Privacy Policy", "How data is handled", Icons.Filled.Policy),
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
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("MatrimonyConnect", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Menu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        activity?.consumeDeepLink()?.let { destination ->
            nav.navigate(destination) { launchSingleTop = true }
        }
    }

    val showBottomBar = currentRoute in setOf(
        MainRoutes.HOME, MainRoutes.MATCHES, MainRoutes.INTERESTS,
        MainRoutes.CHAT_LIST, MainRoutes.PROFILE
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
                        MainRoutes.HOME -> "MatrimonyConnect"
                        MainRoutes.MATCHES -> "Discover"
                        MainRoutes.INTERESTS -> "Interests"
                        MainRoutes.CHAT_LIST -> "Messages"
                        MainRoutes.PROFILE -> "My Profile"
                        else -> "MatrimonyConnect"
                    }
                    TopAppBar(
                        title = { Text(pageTitle, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.testTag("hamburger_btn")) {
                                Icon(Icons.Filled.Menu, "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { nav.navigate(MainRoutes.HELP) }) {
                                Icon(Icons.AutoMirrored.Filled.Help, "Help")
                            }
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
                        onGoStories = {},
                        onGoPricing = { nav.navigate(MainRoutes.PRICING) },
                        onGoInterests = { nav.navigate(MainRoutes.INTERESTS) },
                        onGoNotifications = { nav.navigate(MainRoutes.NOTIFICATIONS) },
                        onGoShortlists = { nav.navigate(MainRoutes.SHORTLISTS) },
                        onGoRegions = { nav.navigate(MainRoutes.MATCHES) },
                        onGoCircles = {},
                        onGoMessages = { nav.navigate(MainRoutes.CHAT_LIST) },
                        onGoProfile = { nav.navigate(MainRoutes.PROFILE) },
                        onGoVerification = { nav.navigate(MainRoutes.VERIFICATION) },
                        onGoKundli = { nav.navigate(MainRoutes.KUNDLI) },
                        onGoWhoViewed = { nav.navigate(MainRoutes.WHO_VIEWED) },
                        onGoFamily = { nav.navigate(MainRoutes.PROFILE) },
                        onGoHelp = { nav.navigate(MainRoutes.HELP) },
                        onGoPrivacyDash = { nav.navigate(MainRoutes.PRIVACY_DASH) },
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) }
                    )
                }
                composable(MainRoutes.MATCHES) {
                    MatchesScreen(onOpen = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.INTERESTS) {
                    InterestsScreen(
                        onOpenProfile = { nav.navigate(MainRoutes.detail(it)) },
                        onOpenChat = { nav.navigate(MainRoutes.chat(it)) }
                    )
                }
                composable(MainRoutes.SHORTLISTS) {
                    ShortlistScreen(onOpenProfile = { nav.navigate(MainRoutes.detail(it)) })
                }
                composable(MainRoutes.CHAT_LIST) {
                    ChatListScreen(onOpenChat = { nav.navigate(MainRoutes.chat(it)) })
                }
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
                composable(MainRoutes.PRICING) { PricingScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.VERIFICATION) { VerificationScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY_DASH) {
                    PrivacyDashboardScreen(
                        onBack = { nav.popBackStack() },
                        onGoSettings = { nav.navigate(MainRoutes.SETTINGS) }
                    )
                }
                composable(MainRoutes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
                composable(MainRoutes.TERMS) { LegalScreen(type = "terms", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.PRIVACY) { LegalScreen(type = "privacy", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.GUIDELINES) { LegalScreen(type = "guidelines", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.SECURITY_PAGE) { LegalScreen(type = "security", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.REFUNDS) { LegalScreen(type = "refunds", onBack = { nav.popBackStack() }) }
                composable(MainRoutes.BIODATA) { BiodataScreen(onBack = { nav.popBackStack() }) }

                composable(
                    MainRoutes.DETAIL,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
                ) { backStack ->
                    val userId = backStack.arguments?.getLong("userId") ?: return@composable
                    MatchDetailScreen(
                        userId = userId,
                        onBack = { nav.popBackStack() },
                        onChat = { nav.navigate(MainRoutes.chat(userId)) },
                        onPricing = { nav.navigate(MainRoutes.PRICING) }
                    )
                }
                composable(
                    MainRoutes.CHAT,
                    arguments = listOf(navArgument("peerId") { type = NavType.LongType })
                ) { backStack ->
                    val peerId = backStack.arguments?.getLong("peerId") ?: return@composable
                    ChatScreen(
                        peerId = peerId,
                        onBack = { nav.popBackStack() },
                        onUpgrade = { nav.navigate(MainRoutes.PRICING) }
                    )
                }
            }
        }
    }
}
