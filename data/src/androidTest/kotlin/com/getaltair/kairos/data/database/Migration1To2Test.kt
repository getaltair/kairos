package com.getaltair.kairos.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying the Room migration from version 1 to version 2.
 *
 * Uses [MigrationTestHelper] to create a v1 database, run the migration,
 * and verify that the new columns exist in both user_preferences and
 * habit_notifications tables.
 */
@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KairosDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsQuietHoursColumnsToUserPreferences() {
        val db = helper.createDatabase(TEST_DB_NAME, 1)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            MIGRATION_1_2
        )

        // Verify new columns exist by querying them.
        // The cursor may be empty if no rows exist, but the query succeeding
        // means the columns were created by the migration.
        val cursor = migratedDb.query(
            "SELECT quiet_hours_enabled, quiet_hours_start, quiet_hours_end FROM user_preferences LIMIT 1"
        )
        cursor.close()
        migratedDb.close()
    }

    @Test
    fun migrate1To2_addsPersistentColumnsToHabitNotifications() {
        val db = helper.createDatabase(TEST_DB_NAME, 1)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            2,
            true,
            MIGRATION_1_2
        )

        val cursor = migratedDb.query(
            "SELECT is_persistent, max_follow_ups FROM habit_notifications LIMIT 1"
        )
        cursor.close()
        migratedDb.close()
    }

    private companion object {
        const val TEST_DB_NAME = "migration-test"
    }
}
