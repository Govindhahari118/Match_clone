package com.match.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.match.app.data.local.dao.BlockDao
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.NotificationDao
import com.match.app.data.local.dao.NoteDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.dao.PhotoDao
import com.match.app.data.local.dao.ProfileViewDao
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.SavedSearchDao
import com.match.app.data.local.dao.ShortlistDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.BlockEntity
import com.match.app.data.local.entity.LikeEntity
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.local.entity.NotificationEntity
import com.match.app.data.local.entity.NoteEntity
import com.match.app.data.local.entity.PendingMessageEntity
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.local.entity.ProfileViewEntity
import com.match.app.data.local.entity.QuestionnaireEntity
import com.match.app.data.local.entity.SavedSearchEntity
import com.match.app.data.local.entity.ShortlistEntity
import com.match.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        QuestionnaireEntity::class,
        PhotoEntity::class,
        MessageEntity::class,
        LikeEntity::class,
        BlockEntity::class,
        ShortlistEntity::class,
        NotificationEntity::class,
        ProfileViewEntity::class,
        SavedSearchEntity::class,
        PendingMessageEntity::class,
        NoteEntity::class
    ],
    version = 17,
    exportSchema = false
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
