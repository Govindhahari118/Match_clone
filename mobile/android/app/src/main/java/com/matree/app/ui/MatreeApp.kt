package com.matree.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.matree.app.design.AppearanceTheme
import com.matree.app.design.MatreeBackground
import com.matree.app.ui.components.MatreeBottomNavigation
import com.matree.app.ui.screens.MatchesScreen
import com.matree.app.ui.screens.MessagesScreen
import com.matree.app.ui.screens.ProfileScreen
import com.matree.app.ui.screens.SearchScreen

@Composable
fun MatreeApp(
    currentAppearance: AppearanceTheme,
    onAppearanceSelected: suspend (AppearanceTheme) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    MatreeBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                MatreeBottomNavigation(
                    currentRoute = currentRoute,
                    onDestinationSelected = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo("search") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "search",
            ) {
                composable("search") {
                    SearchScreen(contentPadding = padding)
                }
                composable("matches") {
                    MatchesScreen(contentPadding = padding)
                }
                composable("messages") {
                    MessagesScreen(contentPadding = padding)
                }
                composable("profile") {
                    ProfileScreen(
                        contentPadding = padding,
                        currentAppearance = currentAppearance,
                        onAppearanceSelected = onAppearanceSelected,
                    )
                }
            }
        }
    }
}
