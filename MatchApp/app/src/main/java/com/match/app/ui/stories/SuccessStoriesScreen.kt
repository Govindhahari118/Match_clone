package com.match.app.ui.stories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private data class Story(
    val names: String,
    val city: String,
    val state: String,
    val year: Int,
    val quote: String,
    val tag: String,
    val community: String,
    val matchScore: Int,
    val daysToMatch: Int,
    val bothVerified: Boolean = true
)

private val STORIES = listOf(
    Story("Rahul & Priya",     "Mumbai",     "Maharashtra", 2024, "We matched in early 2024 and built trust through daily conversations. Within six months, both families met and we got married in December.", "Love Story",     "Maratha",    94, 47),
    Story("Arjun & Sneha",     "Bangalore",  "Karnataka",   2024, "We started with shared values and career goals. The connection was natural from day one and we completed our engagement in three months.", "Family First",   "Brahmin",     88, 32),
    Story("Vikram & Kavya",    "Hyderabad",  "Telangana",   2023, "Compatibility was strong and communication was easy. Our first video call set the tone and we moved confidently towards marriage.",         "Quick Match",    "Reddy",       91, 19),
    Story("Siddharth & Isha",  "Chennai",    "Tamil Nadu",  2024, "The astrology match was 90%. We knew from the first chat that this was something real — the Kundli analysis made both families confident.", "Astro Match",    "Iyengar",     90, 38),
    Story("Karan & Meera",     "Pune",       "Maharashtra", 2023, "Our families connected instantly. Match's questionnaire helped us realise how aligned we were in values before we even met in person.",    "Deep Bond",      "Agarwal",     96, 55),
    Story("Rohan & Neha",      "Delhi",      "Delhi",       2024, "Being verified gave us both confidence. We met in person within two weeks and never looked back. The process was safe and easy.",         "Verified Trust", "Punjabi Khatri", 87, 14),
    Story("Aditya & Riya",     "Kolkata",    "West Bengal", 2024, "Matched on astrology — both Vrishchika Rasi. Our families were sceptical but seeing the 95% compatibility score convinced everyone.",     "Astro Match",    "Kayastha",    95, 28),
    Story("Suresh & Lakshmi",  "Coimbatore", "Tamil Nadu",  2023, "We were both looking for someone who valued family. Match's community filter helped us find each other among thousands of Tamil Brahmin profiles.", "Community", "Tamil Brahmin", 89, 42),
    Story("Mohit & Divya",     "Jaipur",     "Rajasthan",   2024, "We connected over shared hobbies — both musicians. The personality quiz matched us and it felt like the app already knew us.",           "Hobby Match",    "Maheshwari",  92, 61),
    Story("Sachin & Pooja",    "Ahmedabad",  "Gujarat",     2023, "NRI match — Sachin was in the US, Pooja in Ahmedabad. The video call feature on Match made the distance feel small.",                    "NRI Match",      "Patel",       85, 73),
    Story("Nikhil & Ananya",   "Gurgaon",    "Haryana",     2024, "We were both skeptical about matrimony apps. But the questionnaire revealed how perfectly aligned our life goals were. Never doubted again.", "Perfect Fit", "Baniya",       93, 29),
    Story("Deepak & Swati",    "Lucknow",    "Uttar Pradesh", 2023, "Traditional values but modern outlook. Match helped us find exactly that balance — compatible families, shared faith, and great conversation.", "Classic Match", "Kayastha", 88, 45),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessStoriesScreen(onBack: () -> Unit = {}) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("success_stories", "Success Stories")) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("stories_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).testTag("success_stories_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Stats header ─────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            t("lakhs_happy_marriages", "Lakhs of Happy Marriages"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            t("real_couples_found", "Real couples who found their perfect match on Match"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("50,000+",   "Happy Couples")
                            StatChip("12,000+",   "Weddings 2024")
                            StatChip("47 days",   "Avg Match Time")
                            StatChip("28",        "States")
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("200+",   "Communities")
                            StatChip("4.2★",   "App Rating")
                            StatChip("94%",    "Satisfaction")
                            StatChip("10L+",   "Members")
                        }
                    }
                }
            }

            // ── Story cards ──────────────────────────────────────────
            items(STORIES) { s -> StoryCard(s) }

            // ── CTA ──────────────────────────────────────────────────
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(8.dp))
                        Text(t("your_story_next", "Your story could be next"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(t("complete_profile_find_match", "Complete your profile and find your perfect match today."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StoryCard(s: Story) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            // ── Header row ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar pair
                Box(Modifier.size(52.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp).align(Alignment.TopStart)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(s.names.first().uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(36.dp).align(Alignment.BottomEnd)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(s.names.split("&")[1].trim().first().uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.names, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        if (s.bothVerified) Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Color(0xFF1976D2))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${s.city}, ${s.state} · ${s.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SuggestionChip(onClick = {}, label = { Text(s.tag, style = MaterialTheme.typography.labelSmall) })
            }

            // ── Match stats row ────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("${s.matchScore}% match", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("${s.daysToMatch} days", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(s.community, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            // ── Quote ─────────────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp).padding(top = 2.dp),
                        tint = Color(0xFFE91E63))
                    Spacer(Modifier.width(8.dp))
                    Text("\"${s.quote}\"", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
