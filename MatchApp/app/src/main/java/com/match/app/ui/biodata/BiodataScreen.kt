package com.match.app.ui.biodata

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class BiodataViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = session.userId
        .map { id -> id?.let { auth.currentProfile(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun exportPdf(context: Context, p: UserProfile, templateName: String): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(139, 26, 26)
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(50, 50, 50)
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.rgb(80, 80, 80)
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(184, 134, 11)
            strokeWidth = 1.5f
        }

        var y = 60f
        canvas.drawText("Matrimonial Biodata — $templateName", 40f, y, titlePaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        fun section(title: String, vararg fields: Pair<String, String>) {
            canvas.drawText(title, 40f, y, headerPaint)
            y += 4f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 18f
            for ((label, value) in fields) {
                if (value.isNotBlank()) {
                    canvas.drawText("$label: $value", 50f, y, bodyPaint)
                    y += 18f
                }
            }
            y += 8f
        }

        section("Personal Details",
            "Name" to p.displayName,
            "Age" to "${p.age} years",
            "Gender" to p.gender.name,
            "Marital Status" to p.maritalStatus,
            "Height" to "${p.heightCm} cm"
        )
        section("Community",
            "Religion" to p.religion,
            "Mother Tongue" to p.motherTongue,
            "Caste" to p.caste,
            "Sub-Caste" to p.subCaste,
            "Gothra" to p.gothra
        )
        section("Education & Career",
            "Education" to p.education,
            "Profession" to p.profession,
            "Income" to p.incomeBand
        )
        section("Location",
            "City" to p.city,
            "State" to p.state
        )
        section("Astrology",
            "Rasi" to p.rasi,
            "Nakshatra" to p.nakshatra
        )
        if (p.bio.isNotBlank()) {
            section("About Me", "Bio" to p.bio)
        }

        pdfDoc.finishPage(page)
        val file = File(context.cacheDir, "biodata_${p.displayName.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }
}

private val MAROON = Color(0xFF8B1A1A)
private val GOLD   = Color(0xFFB8860B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiodataScreen(
    onBack: () -> Unit = {},
    vm: BiodataViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    var selectedTemplate by remember { mutableStateOf(0) }
    val templates = listOf("Classic", "Modern", "Minimal")
    val context = LocalContext.current
    var pdfExported by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("biodata_maker", "Biodata Maker")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("biodata_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text("Share") },
                        leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(16.dp)) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("biodata_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Free tool banner ─────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(28.dp), tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("free_biodata_maker", "Free Biodata Maker"), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        Text("Create a professional matrimonial biodata instantly — 100% free",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    }
                }
            }

            // ── Template selector ────────────────────────────────────────
            Text(t("choose_template", "Choose template"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                templates.forEachIndexed { idx, name ->
                    FilterChip(
                        selected = selectedTemplate == idx,
                        onClick = { selectedTemplate = idx },
                        label = { Text(name) },
                        leadingIcon = if (selectedTemplate == idx) {
                            { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Biodata preview ──────────────────────────────────────────
            Text(t("preview", "Preview"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            profile?.let { p ->
                when (selectedTemplate) {
                    0 -> ClassicBiodata(p)
                    1 -> ModernBiodata(p)
                    else -> MinimalBiodata(p)
                }
            } ?: run {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // ── Export actions ───────────────────────────────────────────
            Text(t("export_share", "Export & Share"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        profile?.let { p ->
                            val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate])
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", pdfFile
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                            pdfExported = true
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("biodata_export_pdf")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (pdfExported) "PDF Saved!" else "Export PDF")
                }
                Button(
                    onClick = {
                        profile?.let { p ->
                            val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate])
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", pdfFile
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                setPackage("com.whatsapp")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // WhatsApp not installed — fall back to generic share
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share via"))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("biodata_share_whatsapp")
                ) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }
            OutlinedButton(
                onClick = {
                    profile?.let { p ->
                        val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate])
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", pdfFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Matrimonial Biodata — ${p.displayName}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Send via email"))
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("biodata_send_email")
            ) {
                Icon(Icons.Filled.Email, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send via Email")
            }

            // ── Tips card ────────────────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Biodata tips", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    listOf(
                        "✅ Add a recent, smiling profile photo",
                        "✅ Complete your family details for credibility",
                        "✅ Mention hobbies and interests to stand out",
                        "✅ Get verified — 3× more responses with ✓ badge",
                        "✅ Share biodata with family/relatives directly via WhatsApp"
                    ).forEach { tip ->
                        Text(tip, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Classic Biodata Template ─────────────────────────────────────────────────
@Composable
private fun ClassicBiodata(p: UserProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MAROON.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.background(Color(0xFFFFFBF0))) {
            // Header bar
            Box(
                Modifier.fillMaxWidth().background(MAROON).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦ Matrimonial Biodata ✦",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = GOLD)
                    Text(p.displayName,
                        style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Photo placeholder
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Surface(
                        shape = CircleShape,
                        color = MAROON.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, MAROON),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(p.displayName.first().uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MAROON)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Sections
                BiodataSection("Personal Details", MAROON, listOf(
                    "Name" to p.displayName,
                    "Age" to "${p.age} years",
                    "Date of Birth" to "—",
                    "Height" to if (p.heightCm > 0) "${p.heightCm} cm" else "—",
                    "Complexion" to "—",
                    "Marital Status" to p.maritalStatus.ifBlank { "Never Married" }
                ))
                Spacer(Modifier.height(8.dp))

                BiodataSection("Religious Background", MAROON, listOf(
                    "Religion" to p.religion.ifBlank { "—" },
                    "Caste" to p.caste.ifBlank { "—" },
                    "Sub-Caste" to p.subCaste.ifBlank { "—" },
                    "Gotra / Gothra" to p.gothra.ifBlank { "—" },
                    "Rasi (Moon Sign)" to p.rasi.ifBlank { "—" },
                    "Nakshatra (Star)" to p.nakshatra.ifBlank { "—" },
                    "Mangal Dosh" to "—"
                ))
                Spacer(Modifier.height(8.dp))

                BiodataSection("Educational & Professional", MAROON, listOf(
                    "Education" to p.education.ifBlank { "—" },
                    "Profession" to p.profession.ifBlank { "—" },
                    "Annual Income" to p.incomeBand.ifBlank { "—" },
                    "Mother Tongue" to p.motherTongue.ifBlank { "—" }
                ))
                Spacer(Modifier.height(8.dp))

                BiodataSection("Location Details", MAROON, listOf(
                    "City" to p.city.ifBlank { "—" },
                    "State" to p.state.ifBlank { "—" },
                    "Native Place" to "—",
                    "Residential Status" to p.residentialStatus.ifBlank { "Citizen" }
                ))
                Spacer(Modifier.height(8.dp))

                BiodataSection("Family Details", MAROON, listOf(
                    "Father's Occupation" to p.fatherOccupation.ifBlank { "—" },
                    "Mother's Occupation" to p.motherOccupation.ifBlank { "—" },
                    "Siblings" to if (p.siblings > 0) "${p.siblings}" else "—",
                    "Family Type" to p.familyType.ifBlank { "—" }
                ))
                Spacer(Modifier.height(8.dp))

                if (p.bio.isNotBlank()) {
                    Text("About Me", style = MaterialTheme.typography.labelLarge,
                        color = MAROON, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(8.dp), color = MAROON.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(p.bio, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MAROON.copy(alpha = 0.2f))
                Text("Generated by Match App · matchapp.in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MAROON.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun BiodataSection(title: String, accentColor: Color, fields: List<Pair<String, String>>) {
    Text(title, style = MaterialTheme.typography.labelLarge,
        color = accentColor, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    fields.forEach { (label, value) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium, modifier = Modifier.width(140.dp))
            Text(": $value", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Modern Biodata Template ──────────────────────────────────────────────────
@Composable
private fun ModernBiodata(p: UserProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Gradient header
            Box(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(p.displayName.first().uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(p.displayName, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${p.age} yrs · ${p.city}", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f))
                        if (p.isVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Color(0xFF80CBC4))
                                Spacer(Modifier.width(4.dp))
                                Text("Verified Profile", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF80CBC4))
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernInfoRow(Icons.Filled.Person,    "Personal",  "${p.age} yrs · ${p.maritalStatus.ifBlank { "Never Married" }} · ${if (p.heightCm > 0) "${p.heightCm}cm" else ""}")
                ModernInfoRow(Icons.Filled.TempleHindu,"Religion", "${p.religion.ifBlank { "—" }} · ${p.caste.ifBlank { "—" }} · ${p.rasi.ifBlank { "—" }}")
                ModernInfoRow(Icons.Filled.School,    "Education", "${p.education.ifBlank { "—" }} · ${p.profession.ifBlank { "—" }}")
                ModernInfoRow(Icons.Filled.AttachMoney,"Income",   p.incomeBand.ifBlank { "Not disclosed" })
                ModernInfoRow(Icons.Filled.LocationOn, "Location", "${p.city.ifBlank { "—" }}, ${p.state.ifBlank { "India" }}")
                ModernInfoRow(Icons.Filled.Group,     "Family",   "${p.familyType.ifBlank { "Nuclear" }} · Father: ${p.fatherOccupation.ifBlank { "—" }}")
                if (p.bio.isNotBlank()) {
                    HorizontalDivider()
                    Text(p.bio, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ModernInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(18.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Minimal Biodata Template ─────────────────────────────────────────────────
@Composable
private fun MinimalBiodata(p: UserProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.displayName, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("${p.age} · ${p.profession.ifBlank { "Professional" }} · ${p.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(p.displayName.first().uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider()
            listOf(
                "Religion / Caste" to "${p.religion.ifBlank { "—" }} / ${p.caste.ifBlank { "—" }}",
                "Rasi / Nakshatra" to "${p.rasi.ifBlank { "—" }} / ${p.nakshatra.ifBlank { "—" }}",
                "Education" to p.education.ifBlank { "—" },
                "Income" to p.incomeBand.ifBlank { "Not disclosed" },
                "Family" to "${p.familyType.ifBlank { "Nuclear" }} family · ${p.siblings} sibling(s)"
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth()) {
                    Text("$k:", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium, modifier = Modifier.width(130.dp))
                    Text(v, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (p.bio.isNotBlank()) {
                HorizontalDivider()
                Text(p.bio, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
