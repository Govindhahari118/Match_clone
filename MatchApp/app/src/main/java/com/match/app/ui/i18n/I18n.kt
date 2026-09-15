package com.match.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

@Stable
class I18nCatalog(
    private val current: Map<String, String>,
    private val english: Map<String, String>
) {
    /** Pure lookup with English and caller-provided fallback; safe in local JVM tests. */
    fun text(key: String, fallback: String): String = current[key] ?: english[key] ?: fallback

    /** Lookup with named-argument interpolation: replaces `{varName}` placeholders. */
    fun text(key: String, args: Map<String, Any>, fallback: String): String {
        var s = text(key, fallback)
        args.forEach { (k, v) -> s = s.replace("{$k}", v.toString()) }
        return s
    }

    /** Plural-aware lookup. Appends `_plural` suffix when count != 1. */
    fun plural(key: String, count: Int, fallback: String): String {
        val pluralKey = if (count == 1) key else "${key}_plural"
        return text(pluralKey, fallback).replace("{count}", count.toString())
    }
}

val LocalI18n = staticCompositionLocalOf { I18nCatalog(emptyMap(), emptyMap()) }

private val catalogCache = ConcurrentHashMap<String, Map<String, String>>()

@Composable
fun rememberI18nCatalog(languageCode: String): I18nCatalog {
    val context = LocalContext.current
    var english by remember { mutableStateOf(emptyMap<String, String>()) }
    var selected by remember { mutableStateOf(emptyMap<String, String>()) }

    androidx.compose.runtime.LaunchedEffect(languageCode) {
        val en = loadCatalog(context, "en")
        english = en
        selected = if (languageCode == "en") en else loadCatalog(context, languageCode)
    }

    return remember(english, selected) { I18nCatalog(selected, english) }
}

@Composable
fun t(key: String, fallback: String): String = LocalI18n.current.text(key, fallback)

@Composable
fun t(key: String, args: Map<String, Any>, fallback: String): String =
    LocalI18n.current.text(key, args, fallback)

@Composable
fun tp(key: String, count: Int, fallback: String): String =
    LocalI18n.current.plural(key, count, fallback)

private suspend fun loadCatalog(context: Context, code: String): Map<String, String> =
    withContext(Dispatchers.IO) {
        catalogCache.getOrPut(code) {
            val path = "i18n/$code.json"
            runCatching {
                context.assets.open(path).bufferedReader().use { reader ->
                    val json = JSONObject(reader.readText())
                    json.keys().asSequence().associateWith { key -> json.optString(key, "") }
                }
            }.getOrDefault(emptyMap())
        }
    }
