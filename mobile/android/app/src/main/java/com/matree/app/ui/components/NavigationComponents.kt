package com.matree.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.matree.app.design.MatreeTheme

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val MatreeBottomDestinations = listOf(
    BottomDestination("search", "Search", Icons.Outlined.Search),
    BottomDestination("matches", "Matches", Icons.Outlined.FavoriteBorder),
    BottomDestination("messages", "Messages", Icons.Outlined.ChatBubbleOutline),
    BottomDestination("profile", "Profile", Icons.Outlined.PersonOutline),
)

@Composable
fun MatreeBottomNavigation(
    currentRoute: String?,
    onDestinationSelected: (BottomDestination) -> Unit,
) {
    val tokens = MatreeTheme.tokens
    NavigationBar(
        containerColor = tokens.colors.surfaceElevated.copy(alpha = 0.98f),
        contentColor = tokens.colors.textPrimary,
    ) {
        MatreeBottomDestinations.forEach { destination ->
            val selected = destination.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = tokens.typography.caption,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = tokens.colors.actionPrimary,
                    selectedTextColor = tokens.colors.actionPrimary,
                    indicatorColor = tokens.colors.actionSecondary,
                    unselectedIconColor = tokens.colors.textSecondary,
                    unselectedTextColor = tokens.colors.textSecondary,
                ),
            )
        }
    }
}
