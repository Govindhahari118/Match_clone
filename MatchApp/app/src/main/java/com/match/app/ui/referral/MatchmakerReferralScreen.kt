package com.match.app.ui.referral

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val PURPLE = Color(0xFF6A1B9A)

private data class ReferralReward(val milestone: String, val reward: String, val emoji: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchmakerReferralScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("referral", Context.MODE_PRIVATE) }
    val storedCode = prefs.getString("code", null)
    var referralCode by remember {
        // Generate a persistent unique referral code
        val code = storedCode ?: ("MATCH" + (100000..999999).random())
        if (storedCode == null) prefs.edit().putString("code", code).apply()
        mutableStateOf(code)
    }
    var copiedCode by remember { mutableStateOf(false) }
    val totalReferred = prefs.getInt("totalReferred", 0)
    val activeReferred = prefs.getInt("activeReferred", 0)
    val coinsEarned = prefs.getInt("coinsEarned", 0)

    val rewards = remember { listOf(
        ReferralReward("1 referral", "100 coins + 1 Profile Boost", "🎁"),
        ReferralReward("3 referrals", "500 coins + Incognito Mode (7 days)", "🎯"),
        ReferralReward("5 referrals", "1 Month Premium Lite FREE", "⭐"),
        ReferralReward("10 referrals", "3 Months Premium + ₹500 cashback", "👑"),
        ReferralReward("25 referrals", "Lifetime Premium + Matchmaker Badge", "💎"),
        ReferralReward("Success Story", "₹5,000 gift + Featured Story", "💍")
    )}

    val leaderboard = remember { listOf(
        Triple("Sunita A.", 42, "Hyderabad"),
        Triple("Rajesh K.", 38, "Mumbai"),
        Triple("Priya M.", 31, "Bangalore"),
        Triple("Amit S.", 28, "Delhi"),
        Triple("Kavya R.", 25, "Chennai")
    )}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("refer_earn", "Refer & Earn")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("referral_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("referral_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(PURPLE, Color(0xFFAD1457)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💝", style = MaterialTheme.typography.displayMedium)
                        Text(t("be_a_matchmaker", "Be a Matchmaker!"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Refer friends & family. Earn coins, premium features, and real cashback for every successful referral.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalReferred", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(t("referred", "Referred"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$activeReferred", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(t("active", "Active"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$coinsEarned 🪙", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Earned", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }
            }

            // Referral code
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("your_referral_code", "Your Referral Code"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(12.dp), color = PURPLE.copy(0.08f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(referralCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PURPLE, letterSpacing = androidx.compose.ui.unit.TextUnit(3f, androidx.compose.ui.unit.TextUnitType.Sp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            clipboardManager.setText(AnnotatedString(referralCode))
                            copiedCode = true
                        }, colors = ButtonDefaults.buttonColors(containerColor = PURPLE), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (copiedCode) "Copied!" else "Copy Code")
                        }
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }

            // Reward tiers
            Text(t("reward_tiers", "Reward Tiers"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            rewards.forEachIndexed { idx, r ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = if (idx < 2) CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9)) else CardDefaults.elevatedCardColors()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.milestone, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(r.reward, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (idx < 2) Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp), tint = Color(0xFF2E7D32))
                        else Icon(Icons.Filled.Lock, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Leaderboard
            Text(t("top_matchmakers", "Top Matchmakers This Month"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            leaderboard.forEachIndexed { idx, (name, count, city) ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = when (idx) { 0 -> Color(0xFFFFD600); 1 -> Color(0xFFBDBDBD); 2 -> Color(0xFFA1887F); else -> MaterialTheme.colorScheme.surfaceVariant }, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("#${idx + 1}", fontWeight = FontWeight.Bold, color = if (idx < 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text(city, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$count referrals", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PURPLE)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
