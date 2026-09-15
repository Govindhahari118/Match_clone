package com.match.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_13_14 = object : Migration(13, 14) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN lastActiveAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN isIncognito INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE likes ADD COLUMN isSuperLike INTEGER NOT NULL DEFAULT 0")
    }}
    val MIGRATION_14_15 = object : Migration(14, 15) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN ageBucket TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE users SET ageBucket = CASE WHEN age < 18 THEN '<18' WHEN age >= 63 THEN '63+' ELSE printf('%d-%d', 18 + ((age - 18) / 5) * 5, 18 + ((age - 18) / 5) * 5 + 4) END")
    }}
    val MIGRATION_15_16 = object : Migration(15, 16) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN familyValues TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN aboutFamily TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN manglik TEXT NOT NULL DEFAULT ''")
    }}
    val MIGRATION_16_17 = object : Migration(16, 17) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN weight REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN complexion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN physicalStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN birthTime TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN birthPlace TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN familyStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN educationField TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN institution TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN graduationYear INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN occupationCategory TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN employer TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN employerType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN citizenship TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN isNRI INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN fitnessActivities TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN matrimonyId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN photoUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN voiceBioUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN profileCompleteness REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN verificationLevel INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN stealthMode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN showLastActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE users ADD COLUMN showHoroscope INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE users ADD COLUMN incomeDisclosure TEXT NOT NULL DEFAULT 'range'")
        db.execSQL("ALTER TABLE users ADD COLUMN subscriptionPlan TEXT NOT NULL DEFAULT 'FREE'")
        db.execSQL("ALTER TABLE users ADD COLUMN subscriptionExpiry INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN matchScore REAL NOT NULL DEFAULT 0")
    }}
    val MIGRATION_17_18 = object : Migration(17, 18) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pending_messages ADD COLUMN localMessageId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pending_messages ADD COLUMN clientMessageId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE pending_messages ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
        db.execSQL("ALTER TABLE pending_messages ADD COLUMN mediaUri TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE pending_messages ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
    }}
    val MIGRATION_18_19 = object : Migration(18, 19) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN username TEXT NOT NULL DEFAULT ''")
    }}
    val MIGRATION_19_20 = object : Migration(19, 20) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN subCaste TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN minScore REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN verifiedOnly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN incomeMax TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN residentialStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN hasChildren TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN keyword TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN gothra TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN nativeState TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN countryOfResidence TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN nriOnly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN willingToRelocate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN recentlyJoinedDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN familyType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN familyStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN physicalStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN hasChildrenFilter TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN citizenship TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN nriStatus TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN educationField TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN occupationCategory TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN employerType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN nakshatra TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN rasi TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN manglik TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN hobbies TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN withPhotoOnly INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN verifiedLevel INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN premiumOnly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN lastActiveWithinDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN minPoruthamScore INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_searches ADD COLUMN hasHoroscope TEXT NOT NULL DEFAULT ''")
    }}
}
