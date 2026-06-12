package com.match.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.match.app.MainActivity
import com.match.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_MATCHES   = "ch_matches"
        const val CHANNEL_MESSAGES  = "ch_messages"
        const val CHANNEL_INTERESTS = "ch_interests"

        private var nextId = 1000
        private fun nextNotifId() = nextId++
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_MATCHES, "Matches", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Mutual match alerts"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "New message notifications"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_INTERESTS, "Interests", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Someone liked your profile"
                }
            )
        }
    }

    private fun launchIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Fire a local notification when both users have liked each other. */
    fun notifyMutualMatch(theirName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("It's a Match! \uD83C\uDF89")
            .setContentText("You and $theirName have liked each other. Say hello!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()
        nm.notify(nextNotifId(), notif)
    }

    /** Fire a local notification when someone sends a like/interest. */
    fun notifyInterestReceived(theirName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_INTERESTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Interest \u2764\uFE0F")
            .setContentText("$theirName is interested in your profile!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()
        nm.notify(nextNotifId(), notif)
    }

    /** Fire a local notification when a new chat message arrives. */
    fun notifyNewMessage(senderName: String, preview: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText(preview)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()
        nm.notify(nextNotifId(), notif)
    }
}
