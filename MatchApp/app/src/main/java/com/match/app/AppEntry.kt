package com.match.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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

        // Install App Check before app-owned Firebase traffic starts. Release builds always use
        // Play Integrity. Debug builds try the official Firebase debug provider reflectively so
        // production compilation is never coupled to a debug-only implementation class.
        val appCheck = FirebaseAppCheck.getInstance()
        val provider = if (BuildConfig.DEBUG) {
            debugAppCheckProviderOrNull() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        appCheck.installAppCheckProviderFactory(provider)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        MatchFcmService.createNotificationChannel(this)
        DailyMatchDigestWorker.schedule(this)
        remoteConfig.fetchAndActivate()
    }

    private fun debugAppCheckProviderOrNull(): AppCheckProviderFactory? = runCatching {
        val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
        clazz.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
    }.getOrNull()
}
