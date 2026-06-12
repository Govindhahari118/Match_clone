package com.match.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room schema migrations. Add new entries here before every DB version bump. */
object Migrations {

    /**
     * v13 → v14
     * Sprint 7 additions:
     *  - users.lastActiveAt  (INTEGER, default 0)
     *  - users.isIncognito   (INTEGER/boolean, default 0)
     *  - users.phoneNumber   (TEXT, default '')
     *  - likes.isSuperLike   (INTEGER/boolean, default 0)
     */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN lastActiveAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE users ADD COLUMN isIncognito INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE users ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE likes ADD COLUMN isSuperLike INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * v14 → v15
     * Sprint 9 (Discovery 2.0):
     *  - users.ageBucket  (TEXT, default '')
     *    Discrete bucket like "18-22", "23-27" so Firestore can use equality
     *    queries (composite-index friendly) instead of expensive range queries.
     *    Backfilled from existing age via UPDATE.
     */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN ageBucket TEXT NOT NULL DEFAULT ''")
            // Backfill in one statement using SQLite arithmetic.
            // Bucket = floor((age - 18) / 5) → maps 18-22, 23-27, 28-32, ...
            db.execSQL(
                """
                UPDATE users SET ageBucket =
                  CASE
                    WHEN age < 18 THEN '<18'
                    WHEN age >= 63 THEN '63+'
                    ELSE printf('%d-%d',
                      18 + ((age - 18) / 5) * 5,
                      18 + ((age - 18) / 5) * 5 + 4)
                  END
                """.trimIndent()
            )
        }
    }

    /**
     * v15 → v16
     * Sprint 10 — complete biodata + family fields previously silently discarded:
     *  - users.familyValues  (TEXT, default '')
     *  - users.aboutFamily   (TEXT, default '')
     *  - users.manglik       (TEXT, default '')
     */
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN familyValues TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN aboutFamily TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN manglik TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * v16 → v17
     * Sprint 10 — Complete profile schema for production:
     *  - Physical details: dateOfBirth, weight, complexion, physicalStatus
     *  - Kundali: birthTime, birthPlace
     *  - Family: familyStatus
     *  - Education: educationField, institution, graduationYear
     *  - Occupation: occupationCategory, employer, employerType
     *  - NRI: citizenship, isNRI
     *  - Lifestyle: fitnessActivities
     *  - Platform: matrimonyId, photoUrl, voiceBioUrl
     *  - Quality: profileCompleteness, verificationLevel
     *  - Privacy: stealthMode, showLastActive, showHoroscope, incomeDisclosure
     *  - Subscription: subscriptionPlan, subscriptionExpiry
     *  - Matching: matchScore
     */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Physical details
            db.execSQL("ALTER TABLE users ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN weight REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE users ADD COLUMN complexion TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN physicalStatus TEXT NOT NULL DEFAULT ''")
            // Kundali essentials
            db.execSQL("ALTER TABLE users ADD COLUMN birthTime TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN birthPlace TEXT NOT NULL DEFAULT ''")
            // Family economic status
            db.execSQL("ALTER TABLE users ADD COLUMN familyStatus TEXT NOT NULL DEFAULT ''")
            // Education detail
            db.execSQL("ALTER TABLE users ADD COLUMN educationField TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN institution TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN graduationYear INTEGER NOT NULL DEFAULT 0")
            // Occupation detail
            db.execSQL("ALTER TABLE users ADD COLUMN occupationCategory TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN employer TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN employerType TEXT NOT NULL DEFAULT ''")
            // Citizenship & NRI
            db.execSQL("ALTER TABLE users ADD COLUMN citizenship TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN isNRI INTEGER NOT NULL DEFAULT 0")
            // Lifestyle
            db.execSQL("ALTER TABLE users ADD COLUMN fitnessActivities TEXT NOT NULL DEFAULT ''")
            // Platform identity
            db.execSQL("ALTER TABLE users ADD COLUMN matrimonyId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN photoUrl TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN voiceBioUrl TEXT NOT NULL DEFAULT ''")
            // Profile quality
            db.execSQL("ALTER TABLE users ADD COLUMN profileCompleteness REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE users ADD COLUMN verificationLevel INTEGER NOT NULL DEFAULT 0")
            // Privacy settings
            db.execSQL("ALTER TABLE users ADD COLUMN stealthMode INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE users ADD COLUMN showLastActive INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE users ADD COLUMN showHoroscope INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE users ADD COLUMN incomeDisclosure TEXT NOT NULL DEFAULT 'range'")
            // Subscription
            db.execSQL("ALTER TABLE users ADD COLUMN subscriptionPlan TEXT NOT NULL DEFAULT 'FREE'")
            db.execSQL("ALTER TABLE users ADD COLUMN subscriptionExpiry INTEGER NOT NULL DEFAULT 0")
            // Match scoring
            db.execSQL("ALTER TABLE users ADD COLUMN matchScore REAL NOT NULL DEFAULT 0")
        }
    }
}
