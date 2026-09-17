package com.match.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.match.app.core.config.RemoteConfigManager
import com.match.app.service.MatchFcmService
import com.match.app.ui.common.ProtectedFirebaseStorageFetcher
import com.match.app.ui.common.ProtectedFirebaseStorageKeyer
import com.match.app.ui.common.ProtectedFirebaseStorageMapper
import com.match.app.worker.DailyMatchDigestWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppEntry : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteConfig: RemoteConfigManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            // These custom components are registered before Coil's defaults. Only `gs://` Strings
            // are mapped to the protected model; ordinary HTTPS/content/file requests keep Coil's
            // normal behavior. The protected model deliberately has no cache key so each visual
            // request re-enters Firebase Storage authorization before cached bytes can be reused.
            add(ProtectedFirebaseStorageMapper())
            add(ProtectedFirebaseStorageKeyer())
            add(ProtectedFirebaseStorageFetcher.Factory(this@AppEntry))
        }
        .build()

    override fun onCreate() {
        super.onCreate()

        // Concrete App Check providers are build-type specific. Debug installs the Firebase
        // debug provider; release installs Play Integrity. Common code cannot reference or package
        // the debug-only provider by accident.
        AppCheckProviderInstaller.install()

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        MatchFcmService.createNotificationChannel(this)
        DailyMatchDigestWorker.schedule(this)
        remoteConfig.fetchAndActivate()
    }
}
