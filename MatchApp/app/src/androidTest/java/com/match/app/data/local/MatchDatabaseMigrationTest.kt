package com.match.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseNames = mutableListOf<String>()

    @After
    fun cleanup() {
        databaseNames.forEach(context::deleteDatabase)
        databaseNames.clear()
    }

    @Test
    fun everySupportedVersionMigratesToCurrentWithoutLosingExistingRows() {
        for (startVersion in Migrations.OLDEST_SUPPORTED_VERSION until Migrations.CURRENT_VERSION) {
            val name = "migration_" + startVersion + "_to_" + Migrations.CURRENT_VERSION + ".db"
            databaseNames += name
            val helper = createOldestSupportedDatabase(name)
            val db = helper.writableDatabase

            applyMigrations(db, Migrations.OLDEST_SUPPORTED_VERSION, startVersion)
            db.execSQL("DELETE FROM users")
            db.execSQL("INSERT INTO users(id, age) VALUES(?, ?)", arrayOf(42L, 31))
            db.version = startVersion

            applyMigrations(db, startVersion, Migrations.CURRENT_VERSION)

            assertEquals(
                "version " + startVersion + " did not reach current",
                Migrations.CURRENT_VERSION,
                db.version
            )
            db.query("SELECT id, age, profileCreatedFor FROM users WHERE id = 42").use { cursor ->
                assertTrue("existing user was lost from " + startVersion, cursor.moveToFirst())
                assertEquals(42L, cursor.getLong(0))
                assertEquals(31, cursor.getInt(1))
                assertEquals("SELF", cursor.getString(2))
            }
            assertCurrentMigrationArtifacts(db)
            helper.close()
        }
    }

    @Test
    fun migrationChainIsContiguousAndDeclaresTheSupportedBoundary() {
        assertEquals(13, Migrations.OLDEST_SUPPORTED_VERSION)
        assertEquals(25, Migrations.CURRENT_VERSION)
        assertEquals(
            Migrations.CURRENT_VERSION - Migrations.OLDEST_SUPPORTED_VERSION,
            Migrations.ALL.size
        )

        var expectedStart = Migrations.OLDEST_SUPPORTED_VERSION
        Migrations.ALL.forEach { migration ->
            assertEquals(expectedStart, migration.startVersion)
            assertEquals(expectedStart + 1, migration.endVersion)
            expectedStart = migration.endVersion
        }
        assertEquals(Migrations.CURRENT_VERSION, expectedStart)
    }

    private fun createOldestSupportedDatabase(name: String): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(Migrations.OLDEST_SUPPORTED_VERSION) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Minimal v13 shape containing every table/column referenced by the supported
                // migration chain. The test is intentionally independent from current entities so
                // a destructive migration cannot hide behind Room recreating the latest schema.
                db.execSQL("CREATE TABLE users (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firebaseUid` TEXT NOT NULL DEFAULT '', `email` TEXT NOT NULL DEFAULT '', `passwordHash` TEXT NOT NULL DEFAULT '', `displayName` TEXT NOT NULL DEFAULT '', `age` INTEGER NOT NULL DEFAULT 0, `gender` TEXT NOT NULL DEFAULT '', `lookingFor` TEXT NOT NULL DEFAULT '', `city` TEXT NOT NULL DEFAULT '', `bio` TEXT NOT NULL DEFAULT '', `rasi` TEXT NOT NULL DEFAULT '', `nakshatra` TEXT NOT NULL DEFAULT '', `isSeed` INTEGER NOT NULL DEFAULT 0, `religion` TEXT NOT NULL DEFAULT '', `motherTongue` TEXT NOT NULL DEFAULT '', `education` TEXT NOT NULL DEFAULT '', `profession` TEXT NOT NULL DEFAULT '', `maritalStatus` TEXT NOT NULL DEFAULT '', `heightCm` INTEGER NOT NULL DEFAULT 0, `isVerified` INTEGER NOT NULL DEFAULT 0, `isPremium` INTEGER NOT NULL DEFAULT 0, `isShortlisted` INTEGER NOT NULL DEFAULT 0, `profileViewCount` INTEGER NOT NULL DEFAULT 0, `caste` TEXT NOT NULL DEFAULT '', `state` TEXT NOT NULL DEFAULT '', `subCaste` TEXT NOT NULL DEFAULT '', `gothra` TEXT NOT NULL DEFAULT '', `incomeBand` TEXT NOT NULL DEFAULT '', `diet` TEXT NOT NULL DEFAULT '', `familyType` TEXT NOT NULL DEFAULT '', `fatherOccupation` TEXT NOT NULL DEFAULT '', `motherOccupation` TEXT NOT NULL DEFAULT '', `siblings` INTEGER NOT NULL DEFAULT 0, `smoking` TEXT NOT NULL DEFAULT '', `drinking` TEXT NOT NULL DEFAULT '', `personalityType` TEXT NOT NULL DEFAULT '', `hobbies` TEXT NOT NULL DEFAULT '', `spokenLanguages` TEXT NOT NULL DEFAULT '', `videoUrl` TEXT NOT NULL DEFAULT '', `residentialStatus` TEXT NOT NULL DEFAULT '', `hasChildren` INTEGER NOT NULL DEFAULT 0, `boostActiveUntil` INTEGER NOT NULL DEFAULT 0, `nativeState` TEXT NOT NULL DEFAULT '', `countryOfResidence` TEXT NOT NULL DEFAULT '', `visaStatus` TEXT NOT NULL DEFAULT '', `willingToRelocate` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE likes (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE pending_messages (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE saved_searches (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE messages (id INTEGER NOT NULL PRIMARY KEY)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        )
    }

    private fun applyMigrations(db: SupportSQLiteDatabase, from: Int, to: Int) {
        if (from == to) {
            db.version = to
            return
        }

        var current = from
        while (current < to) {
            val migration = Migrations.ALL.singleOrNull {
                it.startVersion == current && it.endVersion == current + 1
            } ?: error("Missing migration " + current + " -> " + (current + 1))
            migration.migrate(db)
            current = migration.endVersion
            db.version = current
        }
    }

    private fun assertCurrentMigrationArtifacts(db: SupportSQLiteDatabase) {
        val userColumns = columns(db, "users")
        listOf(
            "lastActiveAt",
            "isIncognito",
            "phoneNumber",
            "ageBucket",
            "familyValues",
            "dateOfBirth",
            "username",
            "faithTradition",
            "faithSubTradition",
            "faithInstitution",
            "profileRevision",
            "profileCreatedFor"
        ).forEach { column ->
            assertTrue("users." + column + " missing after migration", column in userColumns)
        }

        assertTrue("users.passwordHash must be removed in v25", "passwordHash" !in userColumns)
        assertTrue("users.isSeed must be removed in v25", "isSeed" !in userColumns)
        assertIndexExists(db, "index_users_email")
        assertIndexExists(db, "index_users_firebaseUid")

        val pendingColumns = columns(db, "pending_messages")
        listOf("localMessageId", "clientMessageId", "type", "mediaUri", "durationMs")
            .forEach { column ->
                assertTrue("pending_messages." + column + " missing after migration", column in pendingColumns)
            }

        val searchColumns = columns(db, "saved_searches")
        listOf(
            "subCaste",
            "minScore",
            "verifiedOnly",
            "countryOfResidence",
            "withPhotoOnly",
            "lastActiveWithinDays",
            "hasHoroscope"
        ).forEach { column ->
            assertTrue("saved_searches." + column + " missing after migration", column in searchColumns)
        }

        assertTrue("messages.clientMessageId missing", "clientMessageId" in columns(db, "messages"))
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' " +
                "AND name = 'index_messages_clientMessageId'"
        ).use { cursor ->
            assertTrue("client message id index missing", cursor.moveToFirst())
        }
    }

    private fun assertIndexExists(db: SupportSQLiteDatabase, indexName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = '" + indexName + "'")
            .use { cursor -> assertTrue(indexName + " missing after migration", cursor.moveToFirst()) }
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val result = linkedSetOf<String>()
        db.query("PRAGMA table_info(" + table + ")").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) result += cursor.getString(nameIndex)
        }
        return result
    }
}
