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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
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
import com.match.app.ui.components.MatreeChoiceChip
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeLoadingState
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import com.match.app.ui.theme.colorSchemeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class BiodataShareOptions(
    val includeCommunity: Boolean = false,
    val includeIncome: Boolean = false,
    val includeAstrology: Boolean = false,
    val includeFamily: Boolean = false,
    val includeAboutMe: Boolean = false
)

data class BiodataPdfColors(
    val title: Int,
    val onTitle: Int,
    val header: Int,
    val body: Int,
    val divider: Int
)

@HiltViewModel
class BiodataViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = session.userId
        .map { id -> id?.let { auth.currentProfile(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun exportPdf(
        context: Context,
        p: UserProfile,
        templateName: String,
        options: BiodataShareOptions,
        colors: BiodataPdfColors
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val modernLayout = templateName.equals("Modern", ignoreCase = true)
        val minimalLayout = templateName.equals("Minimal", ignoreCase = true)
        val titlePaint = Paint().apply {
            textSize = if (minimalLayout) 18f else 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (modernLayout) colors.onTitle else colors.title
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = colors.header
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color = colors.body
        }
        val linePaint = Paint().apply {
            color = colors.divider
            strokeWidth = if (minimalLayout) 0.8f else 1.5f
        }

        var y = 60f
        if (modernLayout) {
            val headerBackground = Paint().apply { color = colors.title }
            canvas.drawRoundRect(30f, 24f, 565f, 96f, 16f, 16f, headerBackground)
            canvas.drawText("Matrimonial Biodata", 48f, 60f, titlePaint)
            val templatePaint = Paint(bodyPaint).apply {
                color = colors.onTitle
                textSize = 11f
            }
            canvas.drawText("Modern • $templateName", 48f, 82f, templatePaint)
            y = 122f
        } else {
            canvas.drawText("Matrimonial Biodata — $templateName", 40f, y, titlePaint)
            y += 6f
            if (!minimalLayout) {
                canvas.drawLine(40f, y, 555f, y, linePaint)
            }
            y += if (minimalLayout) 20f else 30f
        }

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
        if (options.includeCommunity) {
            section("Community",
                "Religion" to p.religion,
                "Mother Tongue" to p.motherTongue,
                "Caste" to p.caste,
                "Sub-Caste" to p.subCaste,
                "Gothra" to p.gothra
            )
        }
        section(
            "Education & Career",
            *buildList {
                add("Education" to p.education)
                add("Profession" to p.profession)
                if (options.includeIncome) add("Income" to p.incomeBand)
            }.toTypedArray()
        )
        section("Location",
            "City" to p.city,
            "State" to p.state
        )
        if (
            options.includeAstrology &&
            (p.rasi.isNotBlank() || p.nakshatra.isNotBlank() || p.manglik.isNotBlank())
        ) {
            section("Astrology",
                "Rasi" to p.rasi,
                "Nakshatra" to p.nakshatra,
                "Manglik" to p.manglik
            )
        }
        if (options.includeFamily) {
            section("Family Details",
                "Father's Occupation" to p.fatherOccupation,
                "Mother's Occupation" to p.motherOccupation,
                "Siblings" to p.siblings.takeIf { it > 0 }?.toString().orEmpty(),
                "Family Type" to p.familyType
            )
        }
        if (options.includeAboutMe && p.bio.isNotBlank()) {
            section("About Me", "Bio" to p.bio)
        }

        pdfDoc.finishPage(page)
        val file = File(context.cacheDir, "biodata_${p.displayName.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BiodataScreen(
    onBack: () -> Unit = {},
    vm: BiodataViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    var selectedTemplate by rememberSaveable { mutableStateOf(0) }
    var includeCommunity by rememberSaveable { mutableStateOf(false) }
    var includeIncome by rememberSaveable { mutableStateOf(false) }
    var includeAstrology by rememberSaveable { mutableStateOf(false) }
    var includeFamily by rememberSaveable { mutableStateOf(false) }
    var includeAboutMe by rememberSaveable { mutableStateOf(false) }
    val templates = listOf("Classic", "Modern", "Minimal")
    val context = LocalContext.current
    val shareOptions = BiodataShareOptions(
        includeCommunity = includeCommunity,
        includeIncome = includeIncome,
        includeAstrology = includeAstrology,
        includeFamily = includeFamily,
        includeAboutMe = includeAboutMe
    )
    // Export uses the selected visual family's light palette so print remains readable even
    // when the in-app display mode is Dark.
    val printScheme = colorSchemeFor(MatreeDesign.visual.palette, dark = false)
    val pdfColors = BiodataPdfColors(
        title = printScheme.primary.toArgb(),
        onTitle = printScheme.onPrimary.toArgb(),
        header = printScheme.primary.toArgb(),
        body = printScheme.onSurfaceVariant.toArgb(),
        divider = printScheme.secondary.toArgb()
    )

    Scaffold(
        topBar = {
            MatreeTopBar(title = t("biodata_maker", "Biodata Maker"), onBack = onBack)
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(MatreeDesign.spacing.md)
                .testTag("biodata_screen"),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
        ) {
            MatreeInlineNotice(
                message = "Free Biodata Maker — Generate a biodata PDF from the profile details you choose to share.",
                icon = Icons.Filled.AutoFixHigh,
                tone = MatreeStatusTone.SUCCESS
            )

            // ── Template selector ────────────────────────────────────────
            Text(t("choose_template", "Choose template"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                templates.forEachIndexed { idx, name ->
                    MatreeChoiceChip(
                        text = name,
                        selected = selectedTemplate == idx,
                        onClick = { selectedTemplate = idx },
                        icon = if (selectedTemplate == idx) Icons.Filled.CheckCircle else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Include in biodata", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Optional personal details are excluded by default. Include only what you want in the generated PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
            ) {
                MatreeChoiceChip("Community", includeCommunity, { includeCommunity = !includeCommunity })
                MatreeChoiceChip("Income", includeIncome, { includeIncome = !includeIncome })
                MatreeChoiceChip("Astrology", includeAstrology, { includeAstrology = !includeAstrology })
                MatreeChoiceChip("Family", includeFamily, { includeFamily = !includeFamily })
                MatreeChoiceChip("About me", includeAboutMe, { includeAboutMe = !includeAboutMe })
            }

            // ── Biodata preview ──────────────────────────────────────────
            Text(t("preview", "Preview"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            profile?.let { p ->
                when (selectedTemplate) {
                    0 -> ClassicBiodata(p, shareOptions)
                    1 -> ModernBiodata(p, shareOptions)
                    else -> MinimalBiodata(p, shareOptions)
                }
            } ?: run {
                MatreeLoadingState(message = "Loading biodata preview…", rows = 2)
            }

            // ── Export actions ───────────────────────────────────────────
            Text(t("export_share", "Export & Share"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        profile?.let { p ->
                            val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate], shareOptions, pdfColors)
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", pdfFile
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("biodata_export_pdf")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text("Open PDF")
                }
                Button(
                    onClick = {
                        profile?.let { p ->
                            val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate], shareOptions, pdfColors)
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
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text("WhatsApp")
                }
            }
            OutlinedButton(
                onClick = {
                    profile?.let { p ->
                        val pdfFile = vm.exportPdf(context, p, templates[selectedTemplate], shareOptions, pdfColors)
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
                Spacer(Modifier.width(MatreeDesign.spacing.xs))
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
                        "✅ Verification signals can help others understand which checks you completed",
                        "✅ Share biodata with family/relatives directly via WhatsApp"
                    ).forEach { tip ->
                        Text(tip, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(MatreeDesign.spacing.xl))
        }
    }
}

// ── Classic Biodata Template ─────────────────────────────────────────────────
@Composable
private fun ClassicBiodata(p: UserProfile, options: BiodataShareOptions) {
    val accent = MaterialTheme.colorScheme.primary
    Card(
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        border = BorderStroke(2.dp, accent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(accent)
                    .padding(MatreeDesign.spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "✦ Matrimonial Biodata ✦",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        p.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                Modifier.padding(MatreeDesign.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, accent),
                        modifier = Modifier.size(MatreeDesign.sizes.avatarStandard)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                p.displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }
                }

                BiodataSection(
                    "Personal Details",
                    accent,
                    listOf(
                        "Name" to p.displayName,
                        "Age" to "${p.age} years",
                        "Height" to if (p.heightCm > 0) "${p.heightCm} cm" else "Not provided",
                        "Marital Status" to p.maritalStatus.ifBlank { "Not provided" }
                    )
                )

                if (options.includeCommunity) {
                    BiodataSection(
                        "Community",
                        accent,
                        listOf(
                            "Religion" to p.religion.ifBlank { "Not provided" },
                            "Mother Tongue" to p.motherTongue.ifBlank { "Not provided" },
                            "Community / Caste" to p.caste.ifBlank { "Not provided" },
                            "Sub-Community" to p.subCaste.ifBlank { "Not provided" },
                            "Gotra / Gothra" to p.gothra.ifBlank { "Not provided" }
                        )
                    )
                }

                if (
                    options.includeAstrology &&
                    (p.rasi.isNotBlank() || p.nakshatra.isNotBlank() || p.manglik.isNotBlank())
                ) {
                    BiodataSection(
                        "Astrology",
                        accent,
                        listOf(
                            "Rasi (Moon Sign)" to p.rasi.ifBlank { "Not provided" },
                            "Nakshatra (Star)" to p.nakshatra.ifBlank { "Not provided" },
                            "Manglik" to p.manglik.ifBlank { "Not provided" }
                        )
                    )
                }

                BiodataSection(
                    "Education & Career",
                    accent,
                    buildList {
                        add("Education" to p.education.ifBlank { "Not provided" })
                        add("Profession" to p.profession.ifBlank { "Not provided" })
                        if (options.includeIncome) {
                            add("Annual Income" to p.incomeBand.ifBlank { "Not disclosed" })
                        }
                    }
                )

                BiodataSection(
                    "Location",
                    accent,
                    listOf(
                        "City" to p.city.ifBlank { "Not provided" },
                        "State" to p.state.ifBlank { "Not provided" }
                    )
                )

                if (options.includeFamily) {
                    BiodataSection(
                        "Family Details",
                        accent,
                        listOf(
                            "Father's Occupation" to p.fatherOccupation.ifBlank { "Not provided" },
                            "Mother's Occupation" to p.motherOccupation.ifBlank { "Not provided" },
                            "Siblings" to p.siblings.takeIf { it > 0 }?.toString().orEmpty().ifBlank { "Not provided" },
                            "Family Type" to p.familyType.ifBlank { "Not provided" }
                        )
                    )
                }

                if (options.includeAboutMe && p.bio.isNotBlank()) {
                    Text(
                        "About Me",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(MatreeDesign.radii.small),
                        color = accent.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            p.bio,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(MatreeDesign.spacing.sm)
                        )
                    }
                }

                HorizontalDivider(color = accent.copy(alpha = 0.2f))
                Text(
                    "Generated by Matree",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
private fun ModernBiodata(p: UserProfile, options: BiodataShareOptions) {
    Card(
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(MatreeDesign.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        modifier = Modifier.size(MatreeDesign.sizes.avatarCompact)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                p.displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(MatreeDesign.spacing.sm))
                    Column {
                        Text(
                            p.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "${p.age} yrs · ${p.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        if (p.isVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    modifier = Modifier.size(MatreeDesign.sizes.iconSmall),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(MatreeDesign.spacing.xxs))
                                Text(
                                    "Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.padding(MatreeDesign.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
            ) {
                ModernInfoRow(
                    Icons.Filled.Person,
                    "Personal",
                    "${p.age} yrs · ${p.maritalStatus.ifBlank { "Not provided" }} · ${if (p.heightCm > 0) "${p.heightCm} cm" else "Height not provided"}"
                )
                ModernInfoRow(
                    Icons.Filled.School,
                    "Education",
                    "${p.education.ifBlank { "Not provided" }} · ${p.profession.ifBlank { "Not provided" }}"
                )
                ModernInfoRow(
                    Icons.Filled.LocationOn,
                    "Location",
                    "${p.city.ifBlank { "Not provided" }}, ${p.state.ifBlank { "Not provided" }}"
                )

                if (options.includeCommunity) {
                    ModernInfoRow(
                        Icons.Filled.Diversity3,
                        "Community",
                        "${p.religion.ifBlank { "Not provided" }} · ${p.caste.ifBlank { "Not provided" }} · ${p.motherTongue.ifBlank { "Not provided" }}"
                    )
                }
                if (
                    options.includeAstrology &&
                    (p.rasi.isNotBlank() || p.nakshatra.isNotBlank() || p.manglik.isNotBlank())
                ) {
                    ModernInfoRow(
                        Icons.Filled.AutoAwesome,
                        "Astrology",
                        "${p.rasi.ifBlank { "Not provided" }} · ${p.nakshatra.ifBlank { "Not provided" }} · ${p.manglik.ifBlank { "Not provided" }}"
                    )
                }
                if (options.includeIncome) {
                    ModernInfoRow(
                        Icons.Filled.AttachMoney,
                        "Income",
                        p.incomeBand.ifBlank { "Not disclosed" }
                    )
                }
                if (options.includeFamily) {
                    ModernInfoRow(
                        Icons.Filled.Group,
                        "Family",
                        "${p.familyType.ifBlank { "Not provided" }} · Father: ${p.fatherOccupation.ifBlank { "Not provided" }}"
                    )
                }
                if (options.includeAboutMe && p.bio.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        p.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        Spacer(Modifier.width(MatreeDesign.spacing.sm))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Minimal Biodata Template ─────────────────────────────────────────────────
@Composable
private fun MinimalBiodata(p: UserProfile, options: BiodataShareOptions) {
    Card(
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(MatreeDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        p.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${p.age} · ${p.profession.ifBlank { "Not provided" }} · ${p.city.ifBlank { "Not provided" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MatreeDesign.sizes.touchTarget)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            p.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()
            val rows = buildList {
                add("Education" to p.education.ifBlank { "Not provided" })
                add("Location" to "${p.city.ifBlank { "Not provided" }}, ${p.state.ifBlank { "Not provided" }}")
                if (options.includeCommunity) {
                    add("Community" to "${p.religion.ifBlank { "Not provided" }} / ${p.caste.ifBlank { "Not provided" }}")
                }
                if (options.includeAstrology) {
                    val astrology = listOf(p.rasi, p.nakshatra, p.manglik).filter { it.isNotBlank() }
                    if (astrology.isNotEmpty()) add("Astrology" to astrology.joinToString(" / "))
                }
                if (options.includeIncome) {
                    add("Income" to p.incomeBand.ifBlank { "Not disclosed" })
                }
                if (options.includeFamily) {
                    add(
                        "Family",
                        buildString {
                            append(p.familyType.ifBlank { "Not provided" })
                            p.siblings.takeIf { it > 0 }?.let { append(" · ").append(it).append(" sibling(s)") }
                        }
                    )
                }
            }
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "$label:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(130.dp)
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (options.includeAboutMe && p.bio.isNotBlank()) {
                HorizontalDivider()
                Text(
                    p.bio,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

