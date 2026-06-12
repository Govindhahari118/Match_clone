package com.match.app.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private val GOLD = Color(0xFFFF8F00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRewardsScreen(
    onBack: () -> Unit = {},
    vm: DailyRewardsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("rewards_badges", "Rewards & Badges")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("rewards_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    Surface(shape = RoundedCornerShape(20.dp), color = GOLD.copy(0.15f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(4.dp))
                            Text("${ui.totalCoins}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = GOLD)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { pad ->
        if (ui.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.padding(pad).fillMaxSize().verticalScroll(scrollState).padding(16.dp).testTag("rewards_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Streak + claim
                Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(GOLD, Color(0xFFFF6F00)))).padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🔥 ${ui.streak} Day Streak!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Login daily to maintain your streak and earn bonus coins", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..7).forEach { day ->
                                    val active = day <= ui.streak
                                    Surface(shape = CircleShape, color = if (active) Color.White else Color.White.copy(0.3f), modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(if (active) "✓" else "$day", fontWeight = FontWeight.Bold, color = if (active) GOLD else Color.White)
                                        }
                                    }
                                }
                            }
                            if (!ui.claimedToday) {
                                Button(
                                    onClick = vm::claimDaily,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Claim ${10 + ui.streak * 5} Coins 🪙", color = GOLD, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(0.3f), modifier = Modifier.fillMaxWidth()) {
                                    Text("Claimed today ✓", Modifier.padding(14.dp), color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // Daily tasks
                Text(t("daily_tasks", "Daily Tasks"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ui.tasks.forEach { task ->
                    ElevatedCard(
                        onClick = { if (!task.done) vm.completeTask(task.id, task.coins) },
                        shape = RoundedCornerShape(16.dp), 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(task.icon, null, Modifier.size(24.dp), tint = if (task.done) Color(0xFF2E7D32) else GOLD)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (task.done) FontWeight.Normal else FontWeight.SemiBold)
                                if (!task.done) Text("Earn ${task.coins} coins", style = MaterialTheme.typography.labelSmall, color = GOLD)
                            }
                            if (task.done) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Badges
                Text("Badges (${ui.badges.count { it.earned }}/${ui.badges.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(ui.badges.size) { i ->
                        val b = ui.badges[i]
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(140.dp),
                            colors = if (b.earned) CardDefaults.elevatedCardColors(containerColor = GOLD.copy(0.08f)) else CardDefaults.elevatedCardColors()
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(b.emoji, style = MaterialTheme.typography.headlineLarge)
                                Text(b.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                                Text(b.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                                if (!b.earned) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text("LOCKED", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Redeem
                Text(t("redeem_coins", "Redeem Coins"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ui.rewards.forEach { r ->
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(r.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { vm.redeemReward(r.id, r.coins) },
                                enabled = ui.totalCoins >= r.coins,
                                colors = ButtonDefaults.buttonColors(containerColor = GOLD),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("${r.coins} 🪙", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
