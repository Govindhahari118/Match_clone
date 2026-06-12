package com.match.app.ui.questionnaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.QuestionnaireRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.questionnaire.Questionnaire
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.match.app.ui.i18n.t

data class QuizState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val selfLikert: Map<Int, Int> = emptyMap(),
    val selfInterests: Map<Int, Set<String>> = emptyMap(),
    val partnerLikert: Map<Int, Int> = emptyMap(),
    val partnerInterests: Map<Int, Set<String>> = emptyMap(),
    val message: String? = null
)

@HiltViewModel
class QuestionnaireViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: QuestionnaireRepository
) : ViewModel() {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first()
            val has = uid?.let { repo.hasQuestionnaire(it) } == true
            _state.update { it.copy(loading = false, saved = has) }
        }
    }

    fun setSelfLikert(id: Int, v: Int)       = _state.update { it.copy(selfLikert = it.selfLikert + (id to v)) }
    fun setPartnerLikert(id: Int, v: Int)    = _state.update { it.copy(partnerLikert = it.partnerLikert + (id to v)) }
    fun toggleSelfInterest(id: Int, opt: String) = _state.update {
        val cur = it.selfInterests[id].orEmpty()
        it.copy(selfInterests = it.selfInterests + (id to if (opt in cur) cur - opt else cur + opt))
    }
    fun togglePartnerInterest(id: Int, opt: String) = _state.update {
        val cur = it.partnerInterests[id].orEmpty()
        it.copy(partnerInterests = it.partnerInterests + (id to if (opt in cur) cur - opt else cur + opt))
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(saving = true, message = null) }
        val uid = session.userId.first()
        if (uid == null) {
            _state.update { it.copy(saving = false, message = "Not signed in") }; return@launch
        }
        repo.save(
            userId = uid,
            selfLikert = s.selfLikert, selfInterests = s.selfInterests,
            partnerLikert = s.partnerLikert, partnerInterests = s.partnerInterests
        )
        _state.update { it.copy(saving = false, saved = true, message = "Saved. Your matches will refresh.") }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(vm: QuestionnaireViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(s.message) { s.message?.let { snack.showSnackbar(it); vm.clearMessage() } }

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { pad ->
        if (s.loading) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(pad).fillMaxSize().testTag("quiz_screen")
        ) {
            item {
                Text(t("tell_us_about_you", "Tell us about you"), style = MaterialTheme.typography.headlineSmall)
                Text("All users answer the same 15 questions — about themselves AND their ideal partner.",
                    style = MaterialTheme.typography.bodySmall)
            }

            item { SectionHeader("About yourself — Personality") }
            items(Questionnaire.LIKERT.size, key = { "sl-${Questionnaire.LIKERT[it].id}" }) { i ->
                val q = Questionnaire.LIKERT[i]
                LikertRow(q.prompt, s.selfLikert[q.id]) { vm.setSelfLikert(q.id, it) }
            }
            item { SectionHeader("About yourself — Interests") }
            items(Questionnaire.INTERESTS.size, key = { "si-${Questionnaire.INTERESTS[it].id}" }) { i ->
                val q = Questionnaire.INTERESTS[i]
                InterestRow(q.prompt, q.options, s.selfInterests[q.id].orEmpty()) { vm.toggleSelfInterest(q.id, it) }
            }

            item { SectionHeader("Your ideal partner — Personality") }
            items(Questionnaire.LIKERT.size, key = { "pl-${Questionnaire.LIKERT[it].id}" }) { i ->
                val q = Questionnaire.LIKERT[i]
                LikertRow(q.prompt, s.partnerLikert[q.id]) { vm.setPartnerLikert(q.id, it) }
            }
            item { SectionHeader("Your ideal partner — Interests") }
            items(Questionnaire.INTERESTS.size, key = { "pi-${Questionnaire.INTERESTS[it].id}" }) { i ->
                val q = Questionnaire.INTERESTS[i]
                InterestRow(q.prompt, q.options, s.partnerInterests[q.id].orEmpty()) { vm.togglePartnerInterest(q.id, it) }
            }

            item {
                Button(
                    onClick = vm::save,
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("quiz_save")
                ) {
                    if (s.saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (s.saved) t("update_answers", "Update answers") else t("save_answers", "Save answers"))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun LikertRow(prompt: String, value: Int?, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(prompt, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { v ->
                    FilterChip(selected = value == v, onClick = { onChange(v) }, label = { Text(v.toString()) })
                }
                Spacer(Modifier.weight(1f))
                Text("1 = Low · 5 = High", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun InterestRow(prompt: String, options: List<String>, chosen: Set<String>, onToggle: (String) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(prompt, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            FlowRowPolyfill(options) { opt ->
                FilterChip(selected = opt in chosen, onClick = { onToggle(opt) }, label = { Text(opt) })
            }
        }
    }
}

/** Small polyfill — wraps chips across multiple rows using a simple column of rows. */
@Composable
private fun <T> FlowRowPolyfill(items: List<T>, itemsPerRow: Int = 3, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(itemsPerRow).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { content(it) }
            }
        }
    }
}
