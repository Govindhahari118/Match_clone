package com.match.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.match.app.core.config.RemoteConfigManager
import com.match.app.service.MatchFcmService
import com.match.app.worker.DailyMatchDigestWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppEntry : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteConfig: RemoteConfigManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Enable Crashlytics crash reporting
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        // Register FCM notification channel at app startup
        MatchFcmService.createNotificationChannel(this)
        // Schedule daily match digest notification
        DailyMatchDigestWorker.schedule(this)
        // Fetch remote config values
        remoteConfig.fetchAndActivate()
    }
}

