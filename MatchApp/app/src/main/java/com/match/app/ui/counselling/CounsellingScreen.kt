package com.match.app.ui.counselling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private val COUNSELLING_TOPICS = listOf(
    "Pre-marriage expectations",
    "Communication",
    "Family pressure",
    "Second-marriage support",
    "Post-loss support",
    "Legal guidance request"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounsellingScreen(
    onBack: () -> Unit = {},
    vm: CounsellingViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("Video") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("counselling", "Counselling")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("counsel_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        if (ui.submitted) {
            Box(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Request submitted",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "This is a counselling request, not a confirmed booking. Availability, assigned provider, schedule and any applicable price must be confirmed by the real service workflow before a session is booked.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { vm.resetBooking(); onBack() }) {
                            Text(t("back_home", "Back to Home"))
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(16.dp).testTag("counselling_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupportAgent, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Request counselling support",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Choose the type of support and preferred session mode. No counsellor identity, rating, availability, free-session claim or price is shown until backed by configured provider data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text("What would you like help with?", fontWeight = FontWeight.SemiBold)
                COUNSELLING_TOPICS.forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { selectedTopic = topic },
                        label = { Text(topic) }
                    )
                }

                Text("Preferred mode", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Video", "Phone", "Chat").forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { mode = option },
                            label = { Text(option) }
                        )
                    }
                }

                ui.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        vm.bookSession(
                            counsellor = "Unassigned",
                            sessionType = selectedTopic ?: "General",
                            mode = mode
                        )
                    },
                    enabled = selectedTopic != null && !ui.loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("book_session_btn")
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit counselling request")
                    }
                }
            }
        }
    }
}
