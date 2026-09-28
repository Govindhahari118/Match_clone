package com.match.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationsTest {

    @Test
    fun supportedMigrationChainIsContiguousAndEndsAtCurrentVersion() {
        val actual = Migrations.ALL.map { it.startVersion to it.endVersion }
        val expected = (Migrations.OLDEST_SUPPORTED_VERSION until Migrations.CURRENT_VERSION)
            .map { it to (it + 1) }

        assertEquals(expected, actual)
        assertEquals(Migrations.CURRENT_VERSION, actual.last().second)
    }

    @Test
    fun migrationsNeverDowngradeOrSkipAVersion() {
        assertTrue(Migrations.ALL.isNotEmpty())
        Migrations.ALL.forEach { migration ->
            assertEquals(migration.startVersion + 1, migration.endVersion)
        }
    }
}
