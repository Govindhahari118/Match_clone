package com.match.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.match.app.MainActivity
import com.match.app.R
import com.match.app.data.local.dao.NotificationDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.NotificationEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Firebase Cloud Messaging receiver. Sensitive payloads stay generic; app data is fetched after auth. */
@AndroidEntryPoint
class MatchFcmService : FirebaseMessagingService() {

    @Inject lateinit var notifDao: NotificationDao
    @Inject lateinit var userDao: UserDao
    @Inject lateinit var session: SessionStore
    @Inject lateinit var profileService: FirestoreProfileService

    companion object {
        const val CHANNEL_ID = "match_default_channel"
        const val CHANNEL_NAME = "MatrimonyConnect"
        private const val CH_MESSAGES = "match_messages"
        private const val CH_INTERESTS = "match_interests"
        private const val CH_MATCHES = "match_matches"
        private const val CH_SYSTEM = "match_system"
        private const val CH_SAFETY = "match_safety"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                listOf(
                    NotificationChannel(CH_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "New chat messages"; enableVibration(true) },
                    NotificationChannel(CH_INTERESTS, "Interests", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Interest requests and accepts" },
                    NotificationChannel(CH_MATCHES, "Matches", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "New and mutual matches" },
                    NotificationChannel(CH_SAFETY, "Safety Alerts", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Safety alerts and emergency notifications"; enableVibration(true) },
                    NotificationChannel(CH_SYSTEM, "System", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "App updates, verification and reminders" },
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "General notifications"; enableVibration(true) }
                ).forEach(mgr::createNotificationChannel)
            }
        }

        private fun channelFor(type: String) = when (type) {
            "message" -> CH_MESSAGES
            "interest_received", "like" -> CH_INTERESTS
            "new_match", "mutual_match", "daily_match" -> CH_MATCHES
            "safety_alert", "sos" -> CH_SAFETY
            "boost_expiring", "verification", "verification_update", "reward", "notification",
            "subscription_expiry", "profile_viewed", "profile_incomplete", "inactivity_nudge" -> CH_SYSTEM
            else -> CHANNEL_ID
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit().putString("pending_fcm_token", token).apply()
        serviceScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                profileService.saveFcmToken(uid, token)
                getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                    .edit().remove("pending_fcm_token").apply()
            } catch (e: Exception) {
                Log.w("MatchFcm", "FCM token sync failed; retained for retry", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.data["title"] ?: message.notification?.title ?: "MatrimonyConnect"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val type = message.data["type"] ?: "general"
        val fromFirebaseUid = message.data["peer_uid"] ?: message.data["user_id"]

        serviceScope.launch {
            val localPeer = fromFirebaseUid?.let { userDao.findByFirebaseUid(it)?.id }
            showNotification(title, body, type, localPeer, localPeer)
            persistNotification(type, title, body, localPeer)
        }
    }

    private suspend fun persistNotification(type: String, title: String, body: String, fromUserId: Long?) {
        try {
            val currentMyId = session.userId.firstOrNull() ?: return
            notifDao.insert(
                NotificationEntity(
                    userId = currentMyId,
                    type = type.uppercase(),
                    fromUserId = fromUserId,
                    title = title,
                    body = body,
                    isRead = false
                )
            )
        } catch (e: Exception) {
            Log.e("MatchFcm", "Failed to persist notification", e)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        userId: Long? = null,
        peerId: Long? = null
    ) {
        createNotificationChannel(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_type", type)
            userId?.let { putExtra("from_user_id", it) }
            peerId?.let { putExtra("peer_id", it) }
        }
        val requestCode = (peerId ?: userId ?: System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationId = when (type) {
            "message" -> peerId?.hashCode() ?: 1001
            "interest_received" -> userId?.hashCode() ?: 1002
            "new_match" -> userId?.hashCode() ?: 1003
            "mutual_match" -> userId?.hashCode() ?: 1005
            "profile_viewed" -> 1004
            else -> requestCode
        }
        val notification = NotificationCompat.Builder(this, channelFor(type))
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }
}
