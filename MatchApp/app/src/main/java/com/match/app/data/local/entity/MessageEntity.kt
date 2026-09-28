package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["fromUserId", "toUserId"]), Index("toUserId"), Index("clientMessageId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val body: String,
    val sentAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    /** Local file URI for a recorded voice note (null = text message). */
    val voiceUri: String? = null,
    /** Cloud or local URI for an image attachment. */
    val imageUri: String? = null,
    /** Duration in milliseconds for voice note playback display. */
    val voiceDurationMs: Long? = null,
    /** ID of the message this is a reply to (null = top-level message). */
    val replyToId: Long? = null,
    /** Stable Firestore/client id used to reconcile retries and recipient acknowledgements. */
    val clientMessageId: String = "",
    /** Delivery status: "sending", "failed", "sent", "delivered", "read". */
    val status: String = "sent"
)
