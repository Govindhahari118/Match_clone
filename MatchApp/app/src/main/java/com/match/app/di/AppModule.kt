package com.match.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import com.match.app.BuildConfig
import com.match.app.data.local.MatchDatabase
import com.match.app.data.local.Migrations
import com.match.app.data.local.dao.*
import com.match.app.data.session.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun provideDb(@ApplicationContext ctx: Context): MatchDatabase {
        val builder = Room.databaseBuilder(ctx, MatchDatabase::class.java, "match.db").addMigrations(
            Migrations.MIGRATION_13_14, Migrations.MIGRATION_14_15, Migrations.MIGRATION_15_16,
            Migrations.MIGRATION_16_17, Migrations.MIGRATION_17_18, Migrations.MIGRATION_18_19,
            Migrations.MIGRATION_19_20)
        // Never destructively migrate production data. Debug builds may opt into a clean reset so
        // local schema work does not block developer iteration, using Room's explicit modern API.
        if (BuildConfig.DEBUG) builder.fallbackToDestructiveMigration(dropAllTables = true)
        return builder.build()
    }
    @Provides @Singleton fun provideFirebaseAnalytics(@ApplicationContext ctx: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
    @Provides fun userDao(db: MatchDatabase): UserDao = db.userDao()
    @Provides fun qDao(db: MatchDatabase): QuestionnaireDao = db.questionnaireDao()
    @Provides fun photoDao(db: MatchDatabase): PhotoDao = db.photoDao()
    @Provides fun msgDao(db: MatchDatabase): MessageDao = db.messageDao()
    @Provides fun likeDao(db: MatchDatabase): LikeDao = db.likeDao()
    @Provides fun blockDao(db: MatchDatabase): BlockDao = db.blockDao()
    @Provides fun shortlistDao(db: MatchDatabase): ShortlistDao = db.shortlistDao()
    @Provides fun notificationDao(db: MatchDatabase): NotificationDao = db.notificationDao()
    @Provides fun profileViewDao(db: MatchDatabase): ProfileViewDao = db.profileViewDao()
    @Provides fun savedSearchDao(db: MatchDatabase): SavedSearchDao = db.savedSearchDao()
    @Provides fun pendingMsgDao(db: MatchDatabase): PendingMessageDao = db.pendingMessageDao()
    @Provides fun noteDao(db: MatchDatabase): NoteDao = db.noteDao()
    @Provides @Singleton fun session(@ApplicationContext ctx: Context): SessionStore = SessionStore(ctx)
}
