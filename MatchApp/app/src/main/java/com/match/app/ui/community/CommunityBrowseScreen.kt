package com.match.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

data class CommunityGroup(
    val name: String,
    val emoji: String,
    val color: Color,
    val communities: List<CommunityItem>
)

data class CommunityItem(
    val name: String,
    val count: String,
    val religion: String
)

private val communityGroups = listOf(
    CommunityGroup("Hindu Communities", "🕉️", Color(0xFFFF6F00), listOf(
        CommunityItem("Brahmin", "45K+", "Hindu"),
        CommunityItem("Rajput", "32K+", "Hindu"),
        CommunityItem("Kshatriya", "28K+", "Hindu"),
        CommunityItem("Vaishya", "41K+", "Hindu"),
        CommunityItem("Nadar", "19K+", "Hindu"),
        CommunityItem("Mudaliar", "15K+", "Hindu"),
        CommunityItem("Pillai", "22K+", "Hindu"),
        CommunityItem("Naidu", "27K+", "Hindu"),
        CommunityItem("Reddy", "36K+", "Hindu"),
        CommunityItem("Nair", "18K+", "Hindu"),
        CommunityItem("Iyer", "25K+", "Hindu"),
        CommunityItem("Iyengar", "21K+", "Hindu"),
        CommunityItem("Aggarwal", "38K+", "Hindu"),
        CommunityItem("Jat", "17K+", "Hindu"),
        CommunityItem("Patel", "44K+", "Hindu"),
        CommunityItem("Maratha", "30K+", "Hindu"),
        CommunityItem("Kamma", "23K+", "Hindu"),
        CommunityItem("Kapu", "14K+", "Hindu"),
        CommunityItem("Velama", "11K+", "Hindu"),
        CommunityItem("SC/ST", "20K+", "Hindu"),
    )),
    CommunityGroup("Muslim Communities", "☪️", Color(0xFF1B5E20), listOf(
        CommunityItem("Sunni", "55K+", "Muslim"),
        CommunityItem("Shia", "18K+", "Muslim"),
        CommunityItem("Bohra", "12K+", "Muslim"),
        CommunityItem("Memon", "16K+", "Muslim"),
        CommunityItem("Ansari", "14K+", "Muslim"),
        CommunityItem("Sheikh", "21K+", "Muslim"),
        CommunityItem("Syed", "24K+", "Muslim"),
        CommunityItem("Pathan", "19K+", "Muslim"),
        CommunityItem("Mappila", "10K+", "Muslim"),
    )),
    CommunityGroup("Christian Communities", "✝️", Color(0xFF1565C0), listOf(
        CommunityItem("Catholic", "22K+", "Christian"),
        CommunityItem("Protestant", "15K+", "Christian"),
        CommunityItem("Church of South India", "9K+", "Christian"),
        CommunityItem("Jacobite", "7K+", "Christian"),
        CommunityItem("Pentecostal", "11K+", "Christian"),
    )),
    CommunityGroup("Sikh Communities", "☬", Color(0xFF4A148C), listOf(
        CommunityItem("Jat Sikh", "28K+", "Sikh"),
        CommunityItem("Khatri Sikh", "15K+", "Sikh"),
        CommunityItem("Arora Sikh", "17K+", "Sikh"),
        CommunityItem("Ramgharia", "8K+", "Sikh"),
        CommunityItem("Saini", "6K+", "Sikh"),
    )),
    CommunityGroup("Jain Communities", "🔱", Color(0xFF006064), listOf(
        CommunityItem("Digambara", "11K+", "Jain"),
        CommunityItem("Shwetambar", "14K+", "Jain"),
        CommunityItem("Oswal", "19K+", "Jain"),
        CommunityItem("Porwal", "8K+", "Jain"),
    )),
    CommunityGroup("Other Religions", "🌟", Color(0xFF37474F), listOf(
        CommunityItem("Buddhist", "7K+", "Buddhist"),
        CommunityItem("Parsi", "4K+", "Parsi"),
        CommunityItem("Jewish", "2K+", "Jewish"),
        CommunityItem("Bahai", "1K+", "Bahai"),
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityBrowseScreen(
    onBack: () -> Unit = {},
    onBrowse: (String, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedGroup by remember { mutableStateOf<String?>("Hindu Communities") }

    val filteredGroups = if (searchQuery.isBlank()) communityGroups else {
        communityGroups.mapNotNull { group ->
            val filtered = group.communities.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) group.copy(communities = filtered) else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("browse_by_community", "Browse by Community")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(t("search_community", "Search community...")) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Stats row
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("6 Religions", "50+ Communities", "500K+ Profiles").forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Community groups
            filteredGroups.forEach { group ->
                item(key = group.name) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Group header
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGroup = if (expandedGroup == group.name) null else group.name
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = group.color.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(group.emoji, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("${group.communities.size} communities",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(
                                    if (expandedGroup == group.name) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Communities grid
                            if (expandedGroup == group.name) {
                                HorizontalDivider()
                                Column(Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    group.communities.chunked(2).forEach { row ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { community ->
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = group.color.copy(alpha = 0.06f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { onBrowse(community.religion, community.name) }
                                                ) {
                                                    Row(
                                                        Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(community.name,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold)
                                                            Text(community.count,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = group.color)
                                                        }
                                                        Icon(Icons.Filled.ChevronRight, null,
                                                            Modifier.size(16.dp),
                                                            tint = group.color)
                                                    }
                                                }
                                            }
                                            // Pad if odd number
                                            if (row.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
