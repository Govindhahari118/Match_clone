package com.matree.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matree.app.data.AppearanceThemeRepository
import com.matree.app.design.AppearanceTheme
import com.matree.app.design.MatreeTheme
import com.matree.app.ui.MatreeApp

class MainActivity : ComponentActivity() {
    private val appearanceRepository by lazy {
        AppearanceThemeRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val appearance = appearanceRepository.theme.collectAsStateWithLifecycle(
                initialValue = AppearanceTheme.UNIVERSAL,
            )

            MatreeTheme(appearanceTheme = appearance.value) {
                MatreeApp(
                    currentAppearance = appearance.value,
                    onAppearanceSelected = appearanceRepository::setTheme,
                )
            }
        }
    }
}
