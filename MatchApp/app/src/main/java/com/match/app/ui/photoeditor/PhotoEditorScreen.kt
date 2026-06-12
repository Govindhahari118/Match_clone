package com.match.app.ui.photoeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.match.app.ui.i18n.t
import androidx.hilt.navigation.compose.hiltViewModel

private val PINK = Color(0xFFD81B60)

private enum class FilterPreset(val label: String, val tint: Color) {
    Original("Original", Color.Transparent),
    Warm("Warm", Color(0x33FF6F00)),
    Cool("Cool", Color(0x331565C0)),
    Vintage("Vintage", Color(0x33795548)),
    BW("B & W", Color(0x66000000)),
    Sharp("Sharpen", Color(0x22FFFFFF))
}
private enum class CropAspect(val label: String) { Square("1:1"), Portrait("3:4"), Passport("2:3") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    onBack: () -> Unit = {},
    vm: PhotoEditorViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var selectedFilter by remember { mutableStateOf(FilterPreset.Original) }
    var crop by remember { mutableStateOf(CropAspect.Portrait) }
    var rotation by remember { mutableIntStateOf(0) }
    var watermark by remember { mutableStateOf(true) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var contrast by remember { mutableFloatStateOf(0.5f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setSourceUri(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("photo_editor", "Photo Editor")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("photoeditor_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("photo_editor_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier.fillMaxWidth().height(280.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (ui.sourceUri != null) {
                        AsyncImage(
                            model = ui.sourceUri,
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize()
                                .background(selectedFilter.tint),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFCDD2), Color(0xFFF8BBD0))))
                        ) {
                            Text("\uD83D\uDC70", Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.displayLarge)
                        }
                    }
                    if (watermark) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                            Text("Shared on Match",
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Text("Info: ${crop.label} • rotated $rotation° • ${selectedFilter.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            // Filters
            Text(t("filters", "Filters"), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                FilterPreset.entries.take(3).forEach { f ->
                    FilterChip(selected = selectedFilter == f, onClick = { selectedFilter = f },
                        label = { Text(f.label) }, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                FilterPreset.entries.drop(3).forEach { f ->
                    FilterChip(selected = selectedFilter == f, onClick = { selectedFilter = f },
                        label = { Text(f.label) }, modifier = Modifier.weight(1f))
                }
            }

            // Crop aspect
            Text("Crop aspect", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                CropAspect.entries.forEach { a ->
                    FilterChip(selected = crop == a, onClick = { crop = a },
                        label = { Text(a.label) }, modifier = Modifier.weight(1f))
                }
            }

            // Rotate
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { rotation = (rotation - 90 + 360) % 360 }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Rotate L")
                }
                OutlinedButton(onClick = { rotation = (rotation + 90) % 360 }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Rotate R")
                }
            }

            // Brightness
            Text(t("adjustments", "Adjustments"), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)

            AdjustmentSlider("Brightness", brightness) { brightness = it }
            AdjustmentSlider("Contrast", contrast) { contrast = it }
            AdjustmentSlider("Saturation", saturation) { saturation = it }

            // Watermark toggle
            ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, null, tint = PINK)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("watermark", "Watermark"), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        Text("Protects your photos from unauthorised use",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = watermark, onCheckedChange = { watermark = it })
                }
            }

            if (ui.error != null) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            Button(
                onClick = vm::saveToProfile,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("photo_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PINK),
                enabled = ui.sourceUri != null && !ui.saved && !ui.loading
            ) {
                Icon(Icons.Filled.CloudUpload, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text(if (ui.saved) "Saved!" else "Upload to Profile")
            }

            // Pick image button
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text("Choose Photo from Gallery")
            }

            if (ui.saved) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text("Photo uploaded successfully!",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32))
                    }
                }
            }

            // Reset button
            TextButton(onClick = {
                selectedFilter = FilterPreset.Original
                brightness = 0.5f; contrast = 0.5f; saturation = 0.5f
                rotation = 0; vm.resetSaved()
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Reset All")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
        Text("${((value - 0.5f) * 200).toInt()}",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
    }
}
