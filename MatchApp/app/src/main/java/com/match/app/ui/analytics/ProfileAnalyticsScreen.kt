package com.match.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val BLUE = Color(0xFF1565C0)

private data class MetricCard(val icon: ImageVector, val label: String, val value: String, val delta: String, val up: Boolean)
private data class DayViews(val day: String, val views: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAnalyticsScreen(onBack: () -> Unit = {}) {
    val metrics = remember { listOf(
        MetricCard(Icons.Filled.Visibility, "Profile Views", "1,247", "+18% this week", true),
        MetricCard(Icons.Filled.Favorite, "Interests Received", "86", "+12 this week", true),
        MetricCard(Icons.AutoMirrored.Filled.Send, "Interests Sent", "34", "24 accepted", true),
        MetricCard(Icons.AutoMirrored.Filled.Chat, "Conversations", "12", "4 active", true),
        MetricCard(Icons.Filled.Search, "Search Appearances", "3,420", "+32% this week", true),
        MetricCard(Icons.Filled.Star, "Shortlisted By", "156", "+8 this week", true)
    )}
    val weekViews = remember { listOf(
        DayViews("Mon", 42), DayViews("Tue", 65), DayViews("Wed", 38),
        DayViews("Thu", 71), DayViews("Fri", 89), DayViews("Sat", 120), DayViews("Sun", 95)
    )}
    val maxViews = weekViews.maxOf { it.views }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_analytics", "Profile Analytics")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("analytics_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("analytics_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(BLUE, Color(0xFF0D47A1)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.BarChart, null, Modifier.size(36.dp), tint = Color.White)
                        Text(t("your_profile_performance", "Your Profile Performance"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("See how your profile performs compared to similar users", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Top 15%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(t("profile_rank", "Profile Rank"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("78%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(t("response_rate", "Response Rate"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("4.2×", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("vs Average", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }
            }

            // Key metrics grid
            Text(t("key_metrics", "Key Metrics"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            metrics.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { m ->
                        ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(m.icon, null, Modifier.size(18.dp), tint = BLUE)
                                    Spacer(Modifier.width(6.dp))
                                    Text(m.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(m.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(m.delta, style = MaterialTheme.typography.labelSmall, color = if (m.up) Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Weekly views chart
            Text(t("views_this_week", "Views This Week"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
                        weekViews.forEach { dv ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                Text("${dv.views}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.width(28.dp).height((dv.views.toFloat() / maxViews * 80).dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (dv.views == maxViews) Color(0xFF2E7D32) else BLUE))
                                Spacer(Modifier.height(4.dp))
                                Text(dv.day, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Who's viewing you
            Text(t("viewer_demographics", "Viewer Demographics"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                Triple("Age Group", "25-30 years (62%)", Icons.Filled.Person),
                Triple("Top City", "Hyderabad (28%)", Icons.Filled.LocationOn),
                Triple("Education", "Postgraduate (45%)", Icons.Filled.School),
                Triple("Peak Time", "Sun 7-9 PM (highest)", Icons.Filled.Schedule),
                Triple("Religion", "Hindu (78%)", Icons.Filled.Star),
                Triple("Income", "₹10-20 LPA (52%)", Icons.Filled.AccountBalance)
            ).forEach { (label, value, icon) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp), tint = BLUE)
                    Spacer(Modifier.width(10.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp))
                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tips
            Text(t("optimisation_tips", "Optimisation Tips"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Add a video introduction to get 3.2× more views",
                "Complete your family details section (+45% interest)",
                "Upload 5+ photos for maximum visibility",
                "Update your bio monthly to stay fresh in searches",
                "Verify your profile for a 2× trust boost"
            ).forEachIndexed { i, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = BLUE, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
