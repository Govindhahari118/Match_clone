package com.match.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.match.app.data.local.dao.*
import com.match.app.data.local.entity.*

@Database(
    entities = [UserEntity::class, QuestionnaireEntity::class, PhotoEntity::class, MessageEntity::class,
        LikeEntity::class, BlockEntity::class, ShortlistEntity::class, NotificationEntity::class,
        ProfileViewEntity::class, SavedSearchEntity::class, PendingMessageEntity::class, NoteEntity::class],
    version = Migrations.CURRENT_VERSION,
    exportSchema = true
)
abstract class MatchDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun questionnaireDao(): QuestionnaireDao
    abstract fun photoDao(): PhotoDao
    abstract fun messageDao(): MessageDao
    abstract fun likeDao(): LikeDao
    abstract fun blockDao(): BlockDao
    abstract fun shortlistDao(): ShortlistDao
    abstract fun notificationDao(): NotificationDao
    abstract fun profileViewDao(): ProfileViewDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun noteDao(): NoteDao
}
