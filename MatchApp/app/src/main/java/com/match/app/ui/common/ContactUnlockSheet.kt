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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

/**
 * Contact details are never supplied from the public profile document. The caller
 * must obtain them through the server-authoritative reveal action after mutual-match,
 * membership and quota checks have succeeded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUnlockSheet(
    matchName: String,
    isPremium: Boolean,
    isMutual: Boolean,
    revealedPhone: String,
    contactsUsed: Int,
    contactsLimit: Int,
    isLoading: Boolean,
    requestLoading: Boolean,
    requestStatus: String,
    requestMessage: String?,
    errorMessage: String?,
    onReveal: () -> Unit,
    onRequestAccess: () -> Unit,
    onUpgrade: () -> Unit,
    onMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var copied by remember(revealedPhone) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(64.dp).background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (revealedPhone.isNotBlank()) Icons.Filled.Phone else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                if (revealedPhone.isNotBlank()) t("contact_details", "Contact details") else t("unlock_contact", "Unlock contact"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            when {
                revealedPhone.isNotBlank() -> {
                    Text(
t("contact_authorized_for_match", mapOf("name" to matchName), "Contact access was authorized for your mutual match with {name}."),
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
                            Text(revealedPhone, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(revealedPhone))
                                copied = true
                            }) {
                                Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, t("copy_phone", "Copy phone"))
                            }
                        }
                    }
                    if (contactsLimit > 0) {
                        Text(
t("contacts_unlocked_count", mapOf("used" to contactsUsed, "limit" to contactsLimit), "Contacts unlocked: {used} / {limit} for this membership"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("done", "Done")) }
                }

                !isMutual -> {
                    Text(
t("contact_after_mutual", "Contact details become eligible after both members accept each other's interest."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = { onDismiss(); onMessage() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Chat, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t("message", "Message"))
                    }
                }

                !isPremium -> {
                    Text(
t("contact_membership_required", "An active membership with contact access is required. The server checks your entitlement and quota before returning any phone number."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { onDismiss(); onUpgrade() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("view_membership_plans", "View membership plans"), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRequestAccess,
                        enabled = !requestLoading && requestStatus != "PENDING",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (requestLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                requestLoading -> t("sending_request", "Sending request…")
                                requestStatus == "PENDING" -> t("request_pending", "Request pending")
                                else -> t("request_contact_access", "Request contact access")
                            }
                        )
                    }
                }

                else -> {
                    Text(
t("reveal_shared_phone", mapOf("name" to matchName), "Reveal {name}'s shared phone number? This uses your membership contact allowance. Reopening the same contact does not consume another slot."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    requestMessage?.let { key ->
                        val text = when (key) {
                            "contact_request_pending" -> t("contact_request_pending", "Contact request sent. Waiting for approval.")
                            "contact_request_approved" -> t("contact_request_approved", "Contact sharing is approved. You can reveal the shared number if your membership allows it.")
                            "contact_request_sent" -> t("contact_request_sent", "Contact request sent.")
                            "contact_request_failed" -> t("contact_request_failed", "Contact request could not be sent.")
                            else -> key
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                    errorMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = onReveal,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Phone, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isLoading) t("checking_eligibility", "Checking eligibility…") else t("reveal_contact", "Reveal contact"))
                    }
                    OutlinedButton(
                        onClick = onRequestAccess,
                        enabled = !isLoading && !requestLoading && requestStatus != "PENDING",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (requestLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                requestLoading -> t("sending_request", "Sending request…")
                                requestStatus == "PENDING" -> t("request_pending", "Request pending")
                                else -> t("request_contact_access", "Request contact access")
                            }
                        )
                    }
                    TextButton(onClick = onDismiss, enabled = !isLoading && !requestLoading) { Text(t("cancel", "Cancel")) }
                }
            }
        }
    }
}
