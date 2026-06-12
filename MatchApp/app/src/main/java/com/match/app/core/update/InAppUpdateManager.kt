package com.match.app.core.update

import android.app.Activity
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play In-App Updates.
 *
 * Supports:
 * - IMMEDIATE updates (force update — blocks the app until updated)
 * - FLEXIBLE updates (background download with optional install prompt)
 *
 * Use Remote Config `force_update_version_code` to control which version
 * triggers immediate vs. flexible updates.
 */
@Singleton
class InAppUpdateManager @Inject constructor() {

    companion object {
        private const val TAG = "InAppUpdate"
        const val UPDATE_REQUEST_CODE = 9001
    }

    fun checkForUpdate(
        activity: Activity,
        forceUpdateVersionCode: Int,
        onUpdateAvailable: (Boolean) -> Unit = {}
    ) {
        val updateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = updateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { updateInfo ->
            when {
                updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    val currentVersionCode = try {
                        val pi = activity.packageManager.getPackageInfo(activity.packageName, 0)
                        PackageInfoCompat.getLongVersionCode(pi).toInt()
                    } catch (_: Exception) { 1 }

                    val isForceUpdate = currentVersionCode < forceUpdateVersionCode

                    if (isForceUpdate && updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        startImmediateUpdate(activity, updateManager, updateInfo)
                    } else if (updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startFlexibleUpdate(activity, updateManager, updateInfo)
                    }
                    onUpdateAvailable(true)
                }
                updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Resume the in-progress update
                    startImmediateUpdate(activity, updateManager, updateInfo)
                    onUpdateAvailable(true)
                }
                else -> {
                    onUpdateAvailable(false)
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Update check failed", e)
            onUpdateAvailable(false)
        }
    }

    @Suppress("DEPRECATION")
    private fun startImmediateUpdate(
        activity: Activity,
        manager: com.google.android.play.core.appupdate.AppUpdateManager,
        info: AppUpdateInfo
    ) {
        manager.startUpdateFlowForResult(
            info,
            activity,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            UPDATE_REQUEST_CODE
        )
    }

    @Suppress("DEPRECATION")
    private fun startFlexibleUpdate(
        activity: Activity,
        manager: com.google.android.play.core.appupdate.AppUpdateManager,
        info: AppUpdateInfo
    ) {
        manager.startUpdateFlowForResult(
            info,
            activity,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            UPDATE_REQUEST_CODE
        )
    }
}
