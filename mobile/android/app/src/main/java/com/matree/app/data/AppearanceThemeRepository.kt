package com.matree.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.matree.app.design.AppearanceTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "matree_appearance")

class AppearanceThemeRepository(
    private val context: Context,
) {
    private val key = stringPreferencesKey("appearance_theme")

    val theme: Flow<AppearanceTheme> = context.appearanceDataStore.data.map { preferences ->
        preferences[key]
            ?.let { stored -> AppearanceTheme.entries.firstOrNull { it.name == stored } }
            ?: AppearanceTheme.UNIVERSAL
    }

    suspend fun setTheme(theme: AppearanceTheme) {
        context.appearanceDataStore.edit { preferences ->
            preferences[key] = theme.name
        }
    }
}
