package com.match.app.ui.wedding

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private data class PlannerTask(val id: String, val title: String, val category: String)
private data class BudgetCategory(val id: String, val title: String)
private data class VendorTask(val id: String, val title: String, val detail: String)

private val PLANNER_TASKS = listOf(
    PlannerTask("date", "Agree on the ceremony / wedding date", "Planning"),
    PlannerTask("guest_list", "Prepare the first guest-list estimate", "Planning"),
    PlannerTask("venue", "Choose and confirm the venue", "Venue"),
    PlannerTask("ceremony", "Plan religious, cultural or civil ceremony requirements", "Ceremony"),
    PlannerTask("registration", "Review marriage-registration requirements", "Legal"),
    PlannerTask("catering", "Choose catering and menu preferences", "Food"),
    PlannerTask("photo", "Arrange photography / videography", "Media"),
    PlannerTask("attire", "Plan attire and accessories", "Attire"),
    PlannerTask("decor", "Plan decor, flowers and stage requirements", "Decor"),
    PlannerTask("invites", "Prepare invitations and communication plan", "Invitations"),
    PlannerTask("music", "Arrange music / entertainment if needed", "Entertainment"),
    PlannerTask("transport", "Plan guest and family transportation", "Logistics"),
    PlannerTask("stay", "Arrange accommodation for travelling guests", "Logistics"),
    PlannerTask("schedule", "Create the event-day schedule", "Planning"),
    PlannerTask("emergency", "Keep emergency contacts and essential documents ready", "Safety"),
    PlannerTask("travel", "Plan post-wedding travel only if relevant", "Travel")
)

private val BUDGET_CATEGORIES = listOf(
    BudgetCategory("venue", "Venue & setup"),
    BudgetCategory("food", "Food & catering"),
    BudgetCategory("media", "Photography & video"),
    BudgetCategory("attire", "Attire & accessories"),
    BudgetCategory("decor", "Decor & flowers"),
    BudgetCategory("beauty", "Grooming / beauty"),
    BudgetCategory("entertainment", "Music & entertainment"),
    BudgetCategory("travel", "Travel & accommodation"),
    BudgetCategory("invites", "Invitations & gifts"),
    BudgetCategory("legal", "Legal / registration"),
    BudgetCategory("other", "Other / contingency")
)

private val VENDOR_TASKS = listOf(
    VendorTask("venue", "Venue", "Shortlist, compare terms and confirm the booking."),
    VendorTask("caterer", "Catering", "Confirm menu, headcount policy and service details."),
    VendorTask("photo", "Photography / video", "Confirm deliverables, hours, storage and usage rights."),
    VendorTask("decor", "Decor / flowers", "Confirm scope, setup time and venue restrictions."),
    VendorTask("attire", "Attire / tailoring", "Track fittings, delivery dates and alterations."),
    VendorTask("grooming", "Grooming / beauty", "Confirm timing, trial needs and travel charges."),
    VendorTask("music", "Music / entertainment", "Check venue rules, equipment and timing."),
    VendorTask("ceremony", "Ceremony support", "Arrange the officiant or ceremony support relevant to your chosen tradition or civil format."),
    VendorTask("transport", "Transport", "Confirm vehicles, routes, pickup points and contingency."),
    VendorTask("stay", "Accommodation", "Confirm room blocks, check-in rules and guest list."),
    VendorTask("legal", "Registration support", "Use official/local legal guidance where professional help is required.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingPlannerScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wedding_planner_v2", Context.MODE_PRIVATE) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Checklist", "Budget", "Vendors")

    val completedTasks = remember {
        mutableStateOf(prefs.getStringSet("completed_tasks", emptySet())?.toSet() ?: emptySet())
    }
    val completedVendors = remember {
        mutableStateOf(prefs.getStringSet("completed_vendors", emptySet())?.toSet() ?: emptySet())
    }
    val budgetValues = remember {
        mutableStateMapOf<String, String>().apply {
            BUDGET_CATEGORIES.forEach { category ->
                val stored = prefs.getLong("budget_${category.id}", 0L)
                put(category.id, if (stored > 0L) stored.toString() else "")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wedding Planner", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wedding_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("wedding_planner_screen")) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Filled.Checklist
                                    1 -> Icons.Filled.AccountBalanceWallet
                                    else -> Icons.Filled.Handyman
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ChecklistTab(
                    completed = completedTasks.value,
                    onToggle = { id ->
                        completedTasks.value = completedTasks.value.toMutableSet().apply {
                            if (!add(id)) remove(id)
                        }
                        prefs.edit().putStringSet("completed_tasks", completedTasks.value).apply()
                    }
                )

                1 -> BudgetTab(
                    values = budgetValues,
                    onValueChange = { id, raw ->
                        val digits = raw.filter(Char::isDigit).take(10)
                        budgetValues[id] = digits
                        prefs.edit().putLong("budget_$id", digits.toLongOrNull() ?: 0L).apply()
                    }
                )

                else -> VendorChecklistTab(
                    completed = completedVendors.value,
                    onToggle = { id ->
                        completedVendors.value = completedVendors.value.toMutableSet().apply {
                            if (!add(id)) remove(id)
                        }
                        prefs.edit().putStringSet("completed_vendors", completedVendors.value).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistTab(completed: Set<String>, onToggle: (String) -> Unit) {
    val done = PLANNER_TASKS.count { it.id in completed }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PlannerIntro(
                icon = Icons.Filled.Checklist,
                title = "$done of ${PLANNER_TASKS.size} planning tasks complete",
                body = "This is a neutral planning checklist. Keep only the tasks relevant to your ceremony, family and local legal requirements."
            )
        }

        items(PLANNER_TASKS, key = { it.id }) { task ->
            val checked = task.id in completed
            ElevatedCard(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(task.id) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(task.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetTab(values: Map<String, String>, onValueChange: (String, String) -> Unit) {
    val total = BUDGET_CATEGORIES.sumOf { values[it.id]?.toLongOrNull() ?: 0L }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PlannerIntro(
                icon = Icons.Filled.AccountBalanceWallet,
                title = if (total > 0L) "Current planned total: ₹${"%,d".format(total)}" else "Build your own budget",
                body = "Matree does not insert assumed wedding costs. Enter only the amounts you want to plan for; values are stored on this device."
            )
        }

        items(BUDGET_CATEGORIES, key = { it.id }) { category ->
            OutlinedTextField(
                value = values[category.id].orEmpty(),
                onValueChange = { onValueChange(category.id, it) },
                modifier = Modifier.fillMaxWidth().testTag("wedding_budget_${category.id}"),
                label = { Text(category.title) },
                leadingIcon = { Text("₹") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Your planned amount") }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun VendorChecklistTab(completed: Set<String>, onToggle: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PlannerIntro(
                icon = Icons.Filled.Handyman,
                title = "Vendor planning checklist",
                body = "This is not a vendor marketplace and Matree is not claiming any provider is verified here. Use these categories only to track what you still need to arrange."
            )
        }

        items(VENDOR_TASKS, key = { it.id }) { vendor ->
            val checked = vendor.id in completed
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(vendor.id) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            vendor.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(vendor.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlannerIntro(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
