package com.match.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.match.app.R
import com.match.app.MainActivity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyMatchDigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val session: SessionStore,
    private val matchingRepo: MatchingRepository,
    private val authRepo: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uid = session.userId.first() ?: return Result.success()

        // Count new recommendations
        val profile = authRepo.currentProfile(uid) ?: return Result.success()
        val filter = session.filter.first()
        val matches = matchingRepo.recommendations(uid, MatchMode.ADVANCED, filter)
        val count = matches.size.coerceAtMost(99)

        if (count > 0) {
            sendDigestNotification(profile.displayName, count)
        }
        return Result.success()
    }

    private fun sendDigestNotification(userName: String, count: Int) {
        val context = applicationContext
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$count new matches for you today 💕")
            .setContentText("Hi $userName! Open the app to discover your daily recommendations.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "match_default_channel"
        const val NOTIF_ID   = 9001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyMatchDigestWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_match_digest",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
