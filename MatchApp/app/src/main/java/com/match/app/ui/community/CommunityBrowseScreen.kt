package com.match.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

data class CommunityGroup(
    val name: String,
    val communities: List<CommunityItem>
)

data class CommunityItem(
    val name: String,
    val religion: String
)

private val communityGroups = listOf(
    CommunityGroup("Hindu Communities", listOf(
        CommunityItem("Brahmin", "Hindu"), CommunityItem("Rajput", "Hindu"),
        CommunityItem("Kshatriya", "Hindu"), CommunityItem("Vaishya", "Hindu"),
        CommunityItem("Nadar", "Hindu"), CommunityItem("Mudaliar", "Hindu"),
        CommunityItem("Pillai", "Hindu"), CommunityItem("Naidu", "Hindu"),
        CommunityItem("Reddy", "Hindu"), CommunityItem("Nair", "Hindu"),
        CommunityItem("Iyer", "Hindu"), CommunityItem("Iyengar", "Hindu"),
        CommunityItem("Aggarwal", "Hindu"), CommunityItem("Jat", "Hindu"),
        CommunityItem("Patel", "Hindu"), CommunityItem("Maratha", "Hindu"),
        CommunityItem("Kamma", "Hindu"), CommunityItem("Kapu", "Hindu"),
        CommunityItem("Velama", "Hindu")
    )),
    CommunityGroup("Muslim Communities", listOf(
        CommunityItem("Sunni", "Muslim"), CommunityItem("Shia", "Muslim"),
        CommunityItem("Bohra", "Muslim"), CommunityItem("Memon", "Muslim"),
        CommunityItem("Ansari", "Muslim"), CommunityItem("Sheikh", "Muslim"),
        CommunityItem("Syed", "Muslim"), CommunityItem("Pathan", "Muslim"),
        CommunityItem("Mappila", "Muslim")
    )),
    CommunityGroup("Christian Communities", listOf(
        CommunityItem("Catholic", "Christian"), CommunityItem("Protestant", "Christian"),
        CommunityItem("Church of South India", "Christian"),
        CommunityItem("Jacobite", "Christian"), CommunityItem("Pentecostal", "Christian")
    )),
    CommunityGroup("Sikh Communities", listOf(
        CommunityItem("Jat Sikh", "Sikh"), CommunityItem("Khatri Sikh", "Sikh"),
        CommunityItem("Arora Sikh", "Sikh"), CommunityItem("Ramgharia", "Sikh"),
        CommunityItem("Saini", "Sikh")
    )),
    CommunityGroup("Jain Communities", listOf(
        CommunityItem("Digambara", "Jain"), CommunityItem("Shwetambar", "Jain"),
        CommunityItem("Oswal", "Jain"), CommunityItem("Porwal", "Jain")
    )),
    CommunityGroup("Other Communities", listOf(
        CommunityItem("Buddhist", "Buddhist"), CommunityItem("Parsi", "Parsi"),
        CommunityItem("Jewish", "Jewish"), CommunityItem("Bahai", "Bahai")
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityBrowseScreen(
    onBack: () -> Unit = {},
    onBrowse: (String, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    val filteredGroups = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            communityGroups
        } else {
            communityGroups.mapNotNull { group ->
                val filtered = group.communities.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.religion.contains(searchQuery, ignoreCase = true)
                }
                if (filtered.isEmpty()) null else group.copy(communities = filtered)
            }
        }
    }
    val communityCount = communityGroups.sumOf { it.communities.size }
    val religionCount = communityGroups.flatMap { group ->
        group.communities.map { it.religion }
    }.distinct().size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("browse_by_community", "Browse by Community")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(t("search_community", "Search community...")) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "$religionCount religions • $communityCount community labels",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "These are browse filters, not claims about how many members belong to a community. Profile counts are shown only when backed by live data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            filteredGroups.forEach { group ->
                item(key = group.name) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGroup = if (expandedGroup == group.name) null else group.name
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${group.communities.size} browse options",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (expandedGroup == group.name) {
                                        Icons.Filled.ExpandLess
                                    } else {
                                        Icons.Filled.ExpandMore
                                    },
                                    contentDescription = null
                                )
                            }

                            if (expandedGroup == group.name || searchQuery.isNotBlank()) {
                                HorizontalDivider()
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    group.communities.forEach { community ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onBrowse(community.religion, community.name)
                                                }
                                        ) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        community.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        community.religion,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Icon(
                                                    Icons.Filled.ChevronRight,
                                                    "Browse ${community.name}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredGroups.isEmpty()) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No community label matches your search.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
