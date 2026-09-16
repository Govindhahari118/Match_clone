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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityGroup(
    val name: String,
    val religion: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val communities: List<String>
)

private val COMMUNITY_GROUPS = listOf(
    CommunityGroup(
        "Hindu communities", "Hindu", Icons.Filled.TempleHindu,
        listOf("Brahmin", "Rajput", "Kshatriya", "Vaishya", "Nadar", "Mudaliar", "Pillai", "Naidu", "Reddy", "Nair", "Iyer", "Iyengar", "Aggarwal", "Jat", "Patel", "Maratha", "Kamma", "Kapu", "Velama")
    ),
    CommunityGroup(
        "Muslim communities", "Muslim", Icons.Filled.Mosque,
        listOf("Sunni", "Shia", "Bohra", "Memon", "Ansari", "Sheikh", "Syed", "Pathan", "Mappila")
    ),
    CommunityGroup(
        "Christian communities", "Christian", Icons.Filled.Church,
        listOf("Catholic", "Protestant", "Church of South India", "Jacobite", "Pentecostal")
    ),
    CommunityGroup(
        "Sikh communities", "Sikh", Icons.Filled.Star,
        listOf("Jat Sikh", "Khatri Sikh", "Arora Sikh", "Ramgharia", "Saini")
    ),
    CommunityGroup(
        "Jain communities", "Jain", Icons.Filled.Diamond,
        listOf("Digambara", "Shwetambar", "Oswal", "Porwal")
    ),
    CommunityGroup(
        "Buddhist communities", "Buddhist", Icons.Filled.SelfImprovement,
        emptyList()
    ),
    CommunityGroup(
        "Parsi / Zoroastrian communities", "Parsi/Zoroastrian", Icons.Filled.AutoAwesome,
        emptyList()
    ),
    CommunityGroup(
        "Other communities", "Other", Icons.Filled.Groups,
        emptyList()
    )
)

@HiltViewModel
class CommunityBrowseViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    fun apply(religion: String, community: String?, onApplied: () -> Unit) = viewModelScope.launch {
        val current = session.filter.first()
        session.setFilter(
            current.copy(
                religion = religion,
                caste = community.orEmpty()
            )
        )
        onApplied()
    }
}

/**
 * Community browsing is a discovery-filter launcher. Bundled labels are taxonomy options only;
 * Matree does not attach fabricated member counts or popularity claims to them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityBrowseScreen(
    onBack: () -> Unit = {},
    onBrowse: (String, String) -> Unit = { _, _ -> },
    vm: CommunityBrowseViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    val filteredGroups = remember(query) {
        val needle = query.trim()
        if (needle.isBlank()) COMMUNITY_GROUPS
        else COMMUNITY_GROUPS.mapNotNull { group ->
            val matchingCommunities = group.communities.filter { it.contains(needle, ignoreCase = true) }
            when {
                group.name.contains(needle, ignoreCase = true) || group.religion.contains(needle, ignoreCase = true) -> group
                matchingCommunities.isNotEmpty() -> group.copy(communities = matchingCommunities)
                else -> null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse by Community", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("community_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("community_browse_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Choose a religion or community to apply it to your discovery filters. Counts are shown only when Matree has live, privacy-reviewed data.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("community_search"),
                    label = { Text("Search religion or community") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            filteredGroups.forEach { group ->
                item(key = group.name) {
                    val expanded = expandedGroup == group.name || query.isNotBlank()
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    if (group.communities.isEmpty()) {
                                        vm.apply(group.religion, null) { onBrowse(group.religion, "") }
                                    } else {
                                        expandedGroup = if (expandedGroup == group.name) null else group.name
                                    }
                                }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(group.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (group.communities.isEmpty()) "Apply religion filter" else "${group.communities.size} filter options",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (group.communities.isEmpty()) Icons.Filled.ArrowForward
                                    else if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (expanded && group.communities.isNotEmpty()) {
                                HorizontalDivider()
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            vm.apply(group.religion, null) { onBrowse(group.religion, "") }
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Public, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("All ${group.religion} profiles")
                                    }

                                    group.communities.forEach { community ->
                                        OutlinedButton(
                                            onClick = {
                                                vm.apply(group.religion, community) {
                                                    onBrowse(group.religion, community)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(community, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                                            Icon(Icons.Filled.ArrowForward, contentDescription = "Browse $community")
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
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No matching community option", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Try a broader search or use the main Matches filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
