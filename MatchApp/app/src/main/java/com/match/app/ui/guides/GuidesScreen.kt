package com.match.app.ui.guides

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val INDIGO = Color(0xFF3F51B5)

private data class Article(val title: String, val category: String, val readMin: Int,
                           val excerpt: String, val emoji: String)

private val CATEGORIES = listOf("All", "Second Marriage", "Astrology", "Family",
    "Legal", "Wedding", "NRI", "Communication")

private val ARTICLES = listOf(
    Article("How to handle family pressure for second marriage", "Family", 6,
        "Boundaries, timing, and gentle conversations with parents.", "👨‍👩‍👧"),
    Article("Widower remarriage: Legal checklist", "Legal", 9,
        "Property, guardianship, and registration essentials.", "⚖️"),
    Article("Rebuilding trust after divorce", "Second Marriage", 8,
        "A therapist's guide to opening your heart again.", "💜"),
    Article("36-Guna compatibility explained", "Astrology", 7,
        "What each Koota measures and what a good score looks like.", "🕉"),
    Article("Mangal dosha — myths vs. science", "Astrology", 5,
        "When it matters, when it doesn't — a balanced take.", "🪐"),
    Article("Planning an inter-caste wedding", "Wedding", 10,
        "Rituals blending, family conversations, venue picks.", "💍"),
    Article("NRI marriage: PR, visas, and paperwork", "NRI", 12,
        "Everything you need for spousal immigration.", "✈️"),
    Article("First video call — what to say", "Communication", 4,
        "Ice-breakers that actually work on a Virtual Meet.", "💬"),
    Article("Managing step-children in a blended family", "Second Marriage", 11,
        "Research-backed tips from family therapists.", "👨‍👧‍👦"),
    Article("Kundli matching vs. personality test", "Astrology", 6,
        "Why smart couples do both.", "🔮"),
    Article("Red flags in matrimonial profiles", "Communication", 5,
        "Spot inconsistencies before investing time.", "🚩"),
    Article("Pre-wedding finance talk — 8 topics", "Wedding", 7,
        "Money issues cause 40% of divorces. Talk early.", "💰")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesScreen(onBack: () -> Unit = {}) {
    var selectedCat by remember { mutableStateOf("All") }
    var saved by remember { mutableStateOf(setOf<String>()) }

    val filtered = if (selectedCat == "All") ARTICLES
        else ARTICLES.filter { it.category == selectedCat }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("guides_articles", "Guides & Articles")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("guides_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Category chips
            Row(
                Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CATEGORIES.forEach { c ->
                    FilterChip(
                        selected = selectedCat == c,
                        onClick = { selectedCat = c },
                        label = { Text(c) }
                    )
                }
            }
            HorizontalDivider()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(16.dp).testTag("guides_screen"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = INDIGO.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = INDIGO, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${filtered.size} article${if (filtered.size != 1) "s" else ""}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall)
                            Text(t("curated_by_experts", "Curated by counsellors, astrologers & lawyers"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                filtered.forEach { art ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text(art.emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(art.title, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(art.excerpt, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(4.dp),
                                        color = INDIGO.copy(alpha = 0.15f)) {
                                        Text(art.category, style = MaterialTheme.typography.labelSmall,
                                            color = INDIGO, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.Schedule, null, Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(" ${art.readMin} min read",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                saved = if (art.title in saved) saved - art.title else saved + art.title
                            }) {
                                Icon(
                                    if (art.title in saved) Icons.Filled.Bookmark
                                    else Icons.Filled.BookmarkBorder, null,
                                    tint = if (art.title in saved) INDIGO
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
