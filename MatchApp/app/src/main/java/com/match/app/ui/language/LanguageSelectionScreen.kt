package com.match.app.ui.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data ──────────────────────────────────────────────────────────────────
private data class LangEntry(val code: String, val label: String, val native: String, val dir: String = "ltr")
private data class LangGroup(val tier: String, val id: String, val entries: List<LangEntry>)

private val LANG_GROUPS = listOf(
    LangGroup("Global", "tier1", listOf(
        LangEntry("en", "English",    "English"),
        LangEntry("es", "Spanish",    "Español"),
        LangEntry("hi", "Hindi",      "हिन्दी"),
        LangEntry("pt", "Portuguese", "Português"),
        LangEntry("ru", "Russian",    "Pусский"),
        LangEntry("ar", "Arabic",     "العربية", "rtl"),
    )),
    LangGroup("European & Asian", "tier2", listOf(
        LangEntry("de", "German",   "Deutsch"),
        LangEntry("fr", "French",   "Français"),
        LangEntry("it", "Italian",  "Italiano"),
        LangEntry("ja", "Japanese", "日本語"),
        LangEntry("ko", "Korean",   "한국어"),
        LangEntry("zh", "Chinese",  "简体中文"),
    )),
    LangGroup("Regional & Emerging", "tier3", listOf(
        LangEntry("id", "Indonesian", "Bahasa Indonesia"),
        LangEntry("tr", "Turkish",    "Türkçe"),
        LangEntry("sw", "Swahili",    "Kiswahili"),
        LangEntry("vi", "Vietnamese", "Tiếng Việt"),
        LangEntry("th", "Thai",       "ไทย"),
        LangEntry("ur", "Urdu",       "اردو", "rtl"),
    )),
    LangGroup("Indian Languages", "indian", listOf(
        LangEntry("te", "Telugu",    "తెలుగు"),
        LangEntry("ta", "Tamil",     "தமிழ்"),
        LangEntry("kn", "Kannada",   "ಕನ್ನಡ"),
        LangEntry("mr", "Marathi",   "मराठी"),
        LangEntry("bn", "Bengali",   "বাংলা"),
        LangEntry("gu", "Gujarati",  "ગુજરાતી"),
        LangEntry("ml", "Malayalam", "മലയാളം"),
        LangEntry("pa", "Punjabi",   "ਪੰਜਾਬੀ"),
    )),
)

// ── ViewModel ─────────────────────────────────────────────────────────────
@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    fun setLanguage(code: String) = viewModelScope.launch { session.setUiLanguage(code) }
}

// ── Screen ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    vm: LanguageSelectionViewModel = hiltViewModel()
) {
    val current by vm.uiLanguage.collectAsState()
    val title = t("app_language", "App Language")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lang_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .testTag("lang_select_screen"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            LANG_GROUPS.forEach { group ->
                item(key = group.id) {
                    Text(
                        group.tier,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                items(group.entries, key = { it.code }) { lang ->
                    LangRow(
                        lang = lang,
                        selected = lang.code == current,
                        onClick = {
                            vm.setLanguage(lang.code)
                            onBack()
                        }
                    )
                }
                item(key = "${group.id}_divider") { HorizontalDivider(Modifier.padding(horizontal = 20.dp)) }
            }
        }
    }
}

@Composable
private fun LangRow(lang: LangEntry, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lang_row_${lang.code}"),
        color = if (selected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    lang.native,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    lang.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
