package com.match.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    const val OLDEST_SUPPORTED_VERSION = 13
    const val CURRENT_VERSION = 25
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
    val MIGRATION_20_21 = object : Migration(20, 21) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN faithTradition TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN faithSubTradition TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN faithInstitution TEXT NOT NULL DEFAULT ''")
    }}
    val MIGRATION_21_22 = object : Migration(21, 22) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN clientMessageId TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_clientMessageId ON messages(clientMessageId)")
    }}
    val MIGRATION_22_23 = object : Migration(22, 23) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN profileRevision INTEGER NOT NULL DEFAULT 0")
    }}
    val MIGRATION_23_24 = object : Migration(23, 24) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN profileCreatedFor TEXT NOT NULL DEFAULT 'SELF'")
    }}

    val MIGRATION_24_25 = object : Migration(24, 25) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE users_v25 (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firebaseUid` TEXT NOT NULL, `email` TEXT NOT NULL, `displayName` TEXT NOT NULL, `age` INTEGER NOT NULL, `gender` TEXT NOT NULL, `lookingFor` TEXT NOT NULL, `city` TEXT NOT NULL, `bio` TEXT NOT NULL, `rasi` TEXT NOT NULL, `nakshatra` TEXT NOT NULL, `religion` TEXT NOT NULL, `motherTongue` TEXT NOT NULL, `education` TEXT NOT NULL, `profession` TEXT NOT NULL, `maritalStatus` TEXT NOT NULL, `heightCm` INTEGER NOT NULL, `isVerified` INTEGER NOT NULL, `isPremium` INTEGER NOT NULL, `isShortlisted` INTEGER NOT NULL, `profileViewCount` INTEGER NOT NULL, `caste` TEXT NOT NULL, `state` TEXT NOT NULL, `subCaste` TEXT NOT NULL, `gothra` TEXT NOT NULL, `faithTradition` TEXT NOT NULL, `faithSubTradition` TEXT NOT NULL, `faithInstitution` TEXT NOT NULL, `incomeBand` TEXT NOT NULL, `diet` TEXT NOT NULL, `familyType` TEXT NOT NULL, `fatherOccupation` TEXT NOT NULL, `motherOccupation` TEXT NOT NULL, `siblings` INTEGER NOT NULL, `smoking` TEXT NOT NULL, `drinking` TEXT NOT NULL, `personalityType` TEXT NOT NULL, `hobbies` TEXT NOT NULL, `spokenLanguages` TEXT NOT NULL, `videoUrl` TEXT NOT NULL, `residentialStatus` TEXT NOT NULL, `hasChildren` INTEGER NOT NULL, `boostActiveUntil` INTEGER NOT NULL, `nativeState` TEXT NOT NULL, `countryOfResidence` TEXT NOT NULL, `visaStatus` TEXT NOT NULL, `willingToRelocate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastActiveAt` INTEGER NOT NULL, `isIncognito` INTEGER NOT NULL, `phoneNumber` TEXT NOT NULL, `ageBucket` TEXT NOT NULL, `familyValues` TEXT NOT NULL, `aboutFamily` TEXT NOT NULL, `manglik` TEXT NOT NULL, `dateOfBirth` TEXT NOT NULL, `weight` REAL NOT NULL, `complexion` TEXT NOT NULL, `physicalStatus` TEXT NOT NULL, `birthTime` TEXT NOT NULL, `birthPlace` TEXT NOT NULL, `familyStatus` TEXT NOT NULL, `educationField` TEXT NOT NULL, `institution` TEXT NOT NULL, `graduationYear` INTEGER NOT NULL, `occupationCategory` TEXT NOT NULL, `employer` TEXT NOT NULL, `employerType` TEXT NOT NULL, `citizenship` TEXT NOT NULL, `isNRI` INTEGER NOT NULL, `fitnessActivities` TEXT NOT NULL, `matrimonyId` TEXT NOT NULL, `photoUrl` TEXT NOT NULL, `voiceBioUrl` TEXT NOT NULL, `profileCompleteness` REAL NOT NULL, `verificationLevel` INTEGER NOT NULL, `stealthMode` INTEGER NOT NULL, `showLastActive` INTEGER NOT NULL, `showHoroscope` INTEGER NOT NULL, `incomeDisclosure` TEXT NOT NULL, `subscriptionPlan` TEXT NOT NULL, `subscriptionExpiry` INTEGER NOT NULL, `matchScore` REAL NOT NULL, `username` TEXT NOT NULL, `profileCreatedFor` TEXT NOT NULL, `profileRevision` INTEGER NOT NULL)")
        db.execSQL("INSERT INTO users_v25 (`id`, `firebaseUid`, `email`, `displayName`, `age`, `gender`, `lookingFor`, `city`, `bio`, `rasi`, `nakshatra`, `religion`, `motherTongue`, `education`, `profession`, `maritalStatus`, `heightCm`, `isVerified`, `isPremium`, `isShortlisted`, `profileViewCount`, `caste`, `state`, `subCaste`, `gothra`, `faithTradition`, `faithSubTradition`, `faithInstitution`, `incomeBand`, `diet`, `familyType`, `fatherOccupation`, `motherOccupation`, `siblings`, `smoking`, `drinking`, `personalityType`, `hobbies`, `spokenLanguages`, `videoUrl`, `residentialStatus`, `hasChildren`, `boostActiveUntil`, `nativeState`, `countryOfResidence`, `visaStatus`, `willingToRelocate`, `createdAt`, `lastActiveAt`, `isIncognito`, `phoneNumber`, `ageBucket`, `familyValues`, `aboutFamily`, `manglik`, `dateOfBirth`, `weight`, `complexion`, `physicalStatus`, `birthTime`, `birthPlace`, `familyStatus`, `educationField`, `institution`, `graduationYear`, `occupationCategory`, `employer`, `employerType`, `citizenship`, `isNRI`, `fitnessActivities`, `matrimonyId`, `photoUrl`, `voiceBioUrl`, `profileCompleteness`, `verificationLevel`, `stealthMode`, `showLastActive`, `showHoroscope`, `incomeDisclosure`, `subscriptionPlan`, `subscriptionExpiry`, `matchScore`, `username`, `profileCreatedFor`, `profileRevision`) SELECT `id`, `firebaseUid`, `email`, `displayName`, `age`, `gender`, `lookingFor`, `city`, `bio`, `rasi`, `nakshatra`, `religion`, `motherTongue`, `education`, `profession`, `maritalStatus`, `heightCm`, `isVerified`, `isPremium`, `isShortlisted`, `profileViewCount`, `caste`, `state`, `subCaste`, `gothra`, `faithTradition`, `faithSubTradition`, `faithInstitution`, `incomeBand`, `diet`, `familyType`, `fatherOccupation`, `motherOccupation`, `siblings`, `smoking`, `drinking`, `personalityType`, `hobbies`, `spokenLanguages`, `videoUrl`, `residentialStatus`, `hasChildren`, `boostActiveUntil`, `nativeState`, `countryOfResidence`, `visaStatus`, `willingToRelocate`, `createdAt`, `lastActiveAt`, `isIncognito`, `phoneNumber`, `ageBucket`, `familyValues`, `aboutFamily`, `manglik`, `dateOfBirth`, `weight`, `complexion`, `physicalStatus`, `birthTime`, `birthPlace`, `familyStatus`, `educationField`, `institution`, `graduationYear`, `occupationCategory`, `employer`, `employerType`, `citizenship`, `isNRI`, `fitnessActivities`, `matrimonyId`, `photoUrl`, `voiceBioUrl`, `profileCompleteness`, `verificationLevel`, `stealthMode`, `showLastActive`, `showHoroscope`, `incomeDisclosure`, `subscriptionPlan`, `subscriptionExpiry`, `matchScore`, `username`, `profileCreatedFor`, `profileRevision` FROM users")
        db.execSQL("DROP TABLE users")
        db.execSQL("ALTER TABLE users_v25 RENAME TO users")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_firebaseUid ON users(firebaseUid)")
    }}
    val ALL: List<Migration> = listOf(
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24,
        MIGRATION_24_25
    )

}
