package com.match.app.ui.wedding

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val ROSE = Color(0xFFC62828)

private data class ChecklistItem(val id: Int, val text: String, val category: String, val done: Boolean = false)
private data class VendorCategory(val name: String, val icon: ImageVector, val count: Int, val color: Color)
private data class BudgetItem(val category: String, val estimated: Int, val actual: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingPlannerScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wedding_planner", Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Checklist", "Budget", "Vendors")

    // Checklist state — persist completed IDs to SharedPreferences
    val completedIds = remember {
        prefs.getStringSet("completed_items", emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
    }
    val checklist = remember { mutableStateListOf(
        ChecklistItem(1, "Finalise wedding date", "Planning"),
        ChecklistItem(2, "Book venue / banquet hall", "Venue"),
        ChecklistItem(3, "Hire wedding photographer", "Photography"),
        ChecklistItem(4, "Order wedding invitations", "Stationery"),
        ChecklistItem(5, "Book caterer & finalise menu", "Catering"),
        ChecklistItem(6, "Select wedding outfit / lehenga", "Attire"),
        ChecklistItem(7, "Book priest / pandit", "Ceremony"),
        ChecklistItem(8, "Arrange music / DJ / band", "Entertainment"),
        ChecklistItem(9, "Book mehendi artist", "Beauty"),
        ChecklistItem(10, "Arrange transportation", "Logistics"),
        ChecklistItem(11, "Order flowers & decorations", "Decor"),
        ChecklistItem(12, "Book makeup artist", "Beauty"),
        ChecklistItem(13, "Finalise guest list", "Planning"),
        ChecklistItem(14, "Purchase wedding rings", "Jewellery"),
        ChecklistItem(15, "Book honeymoon travel", "Travel"),
        ChecklistItem(16, "Legal: marriage registration", "Legal"),
        ChecklistItem(17, "Gift registry setup", "Gifts"),
        ChecklistItem(18, "Pre-wedding shoot", "Photography"),
        ChecklistItem(19, "Sangeet / haldi ceremony plan", "Events"),
        ChecklistItem(20, "Reception party arrangements", "Events")
    ).map { it.copy(done = it.id in completedIds) }.toMutableStateList()
    }

    val vendors = remember { listOf(
        VendorCategory("Venues", Icons.Filled.LocationCity, 280, Color(0xFF6A1B9A)),
        VendorCategory("Photographers", Icons.Filled.CameraAlt, 190, Color(0xFF00838F)),
        VendorCategory("Caterers", Icons.Filled.Restaurant, 145, Color(0xFFD84315)),
        VendorCategory("Decorators", Icons.Filled.Palette, 120, Color(0xFF2E7D32)),
        VendorCategory("Makeup Artists", Icons.Filled.Face, 95, Color(0xFFC2185B)),
        VendorCategory("DJs & Bands", Icons.Filled.MusicNote, 80, Color(0xFF1565C0)),
        VendorCategory("Mehendi Artists", Icons.Filled.Brush, 65, Color(0xFFFF6F00)),
        VendorCategory("Pandits", Icons.Filled.Star, 50, Color(0xFF4E342E)),
        VendorCategory("Jewellers", Icons.Filled.Diamond, 110, Color(0xFF827717)),
        VendorCategory("Travel Agents", Icons.Filled.Flight, 70, Color(0xFF0D47A1))
    )}

    val budget = remember { listOf(
        BudgetItem("Venue & Decor", 500000, 0),
        BudgetItem("Catering", 300000, 0),
        BudgetItem("Photography & Video", 150000, 0),
        BudgetItem("Attire & Jewellery", 200000, 0),
        BudgetItem("Makeup & Mehendi", 50000, 0),
        BudgetItem("Entertainment", 75000, 0),
        BudgetItem("Travel & Honeymoon", 200000, 0),
        BudgetItem("Invitations & Gifts", 50000, 0),
        BudgetItem("Misc & Buffer", 100000, 0)
    )}
    val totalBudget = budget.sumOf { it.estimated }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("wedding_planner", "Wedding Planner")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("wedding_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("wedding_planner_screen")) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                        text = { Text(t) }, icon = {
                            Icon(when (i) { 0 -> Icons.Filled.Checklist; 1 -> Icons.Filled.AccountBalance; else -> Icons.Filled.Store }, null, Modifier.size(18.dp))
                        })
                }
            }
            when (selectedTab) {
                0 -> ChecklistTab(checklist) { idx ->
                    val item = checklist[idx]
                    val newDone = !item.done
                    checklist[idx] = item.copy(done = newDone)
                    if (newDone) completedIds.add(item.id) else completedIds.remove(item.id)
                    prefs.edit().putStringSet("completed_items", completedIds.map { it.toString() }.toSet()).apply()
                }
                1 -> BudgetTab(budget, totalBudget)
                2 -> VendorsTab(vendors)
            }
        }
    }
}

@Composable
private fun ChecklistTab(items: List<ChecklistItem>, onToggle: (Int) -> Unit) {
    val done = items.count { it.done }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = ROSE.copy(0.06f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$done of ${items.size} tasks done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { done.toFloat() / items.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (done > items.size / 2) Color(0xFF2E7D32) else ROSE)
                }
            }
        }
        items(items.size) { idx ->
            val item = items[idx]
            ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.done, onCheckedChange = { onToggle(idx) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32)))
                    Column(Modifier.weight(1f)) {
                        Text(item.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None)
                        Text(item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetTab(items: List<BudgetItem>, total: Int) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1B5E20).copy(0.08f))) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t("total_estimated_budget", "Total Estimated Budget"), style = MaterialTheme.typography.labelMedium)
                    Text("₹${"%,d".format(total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Text(t("customise_amounts", "Customise amounts below"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(items) { b ->
            ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.category, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("${(b.estimated.toFloat() / total * 100).toInt()}% of budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("₹${"%,d".format(b.estimated)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF1B5E20))
                }
            }
        }
    }
}

@Composable
private fun VendorsTab(vendors: List<VendorCategory>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Browse ${vendors.sumOf { it.count }}+ verified vendors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(vendors) { v ->
            ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = v.color.copy(0.12f), modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(v.icon, null, Modifier.size(24.dp), tint = v.color) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text("${v.count} verified vendors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
