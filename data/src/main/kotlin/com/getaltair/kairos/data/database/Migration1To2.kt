package com.getaltair.kairos.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration from version 1 to version 2.
 *
 * Adds quiet hours columns to user_preferences and persistent reminder
 * columns to habit_notifications to support the notification system.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Quiet hours support for user_preferences
        database.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN quiet_hours_enabled INTEGER NOT NULL DEFAULT 1"
        )
        database.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN quiet_hours_start TEXT NOT NULL DEFAULT '22:00'"
        )
        database.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN quiet_hours_end TEXT NOT NULL DEFAULT '07:00'"
        )

        // Persistent reminder support for habit_notifications
        database.execSQL(
            "ALTER TABLE habit_notifications ADD COLUMN is_persistent INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE habit_notifications ADD COLUMN max_follow_ups INTEGER NOT NULL DEFAULT 3"
        )
    }
}
