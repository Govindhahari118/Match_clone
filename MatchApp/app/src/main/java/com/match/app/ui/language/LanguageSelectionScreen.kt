package com.match.app.ui.language

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.match.app.ui.i18n.SupportedUiLocales
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class LangEntry(val code: String, val label: String, val native: String)

/**
 * Only language packs that have real, independently translated production assets are exposed.
 * Placeholder/duplicated packs remain hidden until translation QA is complete.
 */
private val SUPPORTED_LANGUAGES = listOf(
    LangEntry("en", "English", "English"),
    LangEntry("te", "Telugu", "తెలుగు"),
    LangEntry("hi", "Hindi", "हिन्दी")
)

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun setLanguage(code: String) = viewModelScope.launch {
        if (code in SupportedUiLocales.codes) session.setUiLanguage(code)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    vm: LanguageSelectionViewModel = hiltViewModel()
) {
    val current by vm.uiLanguage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("app_language", "App Language")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lang_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().testTag("lang_select_screen"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    t("available_languages", "Available languages"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            items(SUPPORTED_LANGUAGES, key = { it.code }) { lang ->
                LangRow(
                    lang = lang,
                    selected = lang.code == current,
                    onClick = {
                        vm.setLanguage(lang.code)
                        onBack()
                    }
                )
            }
            item {
                HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Text(
                    t(
                        "more_languages_after_qa",
                        "More languages will appear only after translation and layout QA is complete."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LangRow(lang: LangEntry, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("lang_row_${lang.code}"),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(lang.native, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                Text(lang.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
