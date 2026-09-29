package com.match.app.ui.photoeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.match.app.ui.i18n.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    onBack: () -> Unit = {},
    vm: PhotoEditorViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(vm::setSourceUri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_photo", "Profile Photo")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("photoeditor_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
                .testTag("photo_editor_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(320.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (ui.sourceUri != null) {
                        AsyncImage(
                            model = ui.sourceUri,
                            contentDescription = "Selected profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Choose a photo to submit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Uploading records the current Media processing notice. The image stays protected and owner-only while pending moderation. It becomes peer-readable only after an authorized review publishes it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            ui.error?.let { message ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (ui.submittedForReview) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Submitted for review",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Submission does not mean the photo is published. Your current approved profile photo remains unchanged until review succeeds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Button(
                onClick = vm::submitForReview,
                enabled = ui.sourceUri != null && !ui.submittedForReview && !ui.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .testTag("photo_save_btn")
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ui.loading) "Submitting…" else "Submit photo for review")
            }

            OutlinedButton(
                onClick = {
                    vm.chooseAnother()
                    imagePickerLauncher.launch("image/*")
                },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ui.sourceUri == null) "Choose photo from gallery" else "Choose a different photo")
            }

            Text(
                "Crop, filters and watermark controls are intentionally not shown until Matree applies those edits to the actual uploaded bytes. The preview above reflects only the selected source image.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}
