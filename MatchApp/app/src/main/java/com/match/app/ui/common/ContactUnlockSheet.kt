package com.match.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet that reveals a match's phone/contact details.
 *
 * Business rules (matching BharatMatrimony / Shaadi.com model):
 *  - Premium members can reveal contact for free (up to their plan limit).
 *  - Free members see a teaser and are redirected to upgrade.
 *  - Contact is only shown when BOTH sides have consented (mutual match).
 *    For one-way interests, show a "Awaiting acceptance" state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUnlockSheet(
    matchName: String,
    matchPhone: String,        // blank = not yet shared by the match
    isPremium: Boolean,
    isMutual: Boolean,
    contactsUsed: Int,         // how many contacts unlocked this month
    contactsLimit: Int,        // plan limit (-1 = unlimited)
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard  = LocalClipboardManager.current
    var copied     by remember { mutableStateOf(false) }

    val canReveal = isPremium && isMutual && matchPhone.isNotBlank() &&
            (contactsLimit == -1 || contactsUsed < contactsLimit)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header icon
            Box(
                Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFE91E63), Color(0xFFFF5722))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (canReveal) Icons.Filled.Phone else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                if (canReveal) "Contact Details" else "Unlock Contact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            when {
                canReveal -> {
                    // ── Show the phone number ──────────────────────────────────
                    Text(
                        "You and $matchName are a mutual match.\nContact details are now available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                matchPhone,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(matchPhone))
                                    copied = true
                                }
                            ) {
                                Icon(
                                    if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (contactsLimit != -1) {
                        Text(
                            "Contacts unlocked: $contactsUsed / $contactsLimit this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }

                !isMutual -> {
                    // ── Waiting for mutual ─────────────────────────────────────
                    Text(
                        "Waiting for $matchName to accept your interest.\nContact details will be available once they accept.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("OK, I'll wait")
                    }
                }

                !isPremium -> {
                    // ── Upgrade prompt ─────────────────────────────────────────
                    Text(
                        "Upgrade to Premium to view contact details and connect directly with $matchName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    // Feature list
                    listOf(
                        "📞 View phone numbers",
                        "💬 Unlimited messaging",
                        "⭐ Send Super Interests",
                        "👁 See who viewed you",
                        "🔒 Incognito browse"
                    ).forEach { feature ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Button(
                        onClick = { onUpgrade(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63)
                        )
                    ) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Upgrade to Premium", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text("Maybe later") }
                }

                else -> {
                    // Phone not shared yet
                    Text(
                        "$matchName hasn't added a phone number yet. Send them a message instead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Send Message")
                    }
                }
            }
        }
    }
}
