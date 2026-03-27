package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.getaltair.kairos.data.converter.LocalTimeConverter
import com.getaltair.kairos.data.converter.ThemeConverter
import com.getaltair.kairos.domain.enums.Theme
import java.time.LocalTime
import java.util.UUID

/**
 * Room entity representing global user settings and preferences.
 * A singleton document containing all user-wide configuration.
 *
 * @property id Unique identifier for the preferences document
 * @property userId ID of the user (for sync purposes)
 * @property notificationEnabled Whether notifications are enabled
 * @property defaultReminderTime Default time for reminders
 * @property theme Current theme preference
 * @property energyTrackingEnabled Whether energy level tracking is enabled
 * @property notificationChannels Per-channel notification settings (JSON)
 * @property createdAt When the preferences were created
 * @property updatedAt When the preferences were last updated
 */
@Entity(
    tableName = "user_preferences",
    indices = [
        Index(value = ["user_id"])
    ]
)
@TypeConverters(LocalTimeConverter::class, ThemeConverter::class)
data class UserPreferencesEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,

    @ColumnInfo(name = "default_reminder_time")
    val defaultReminderTime: LocalTime,

    @ColumnInfo(name = "theme")
    val theme: Theme,

    @ColumnInfo(name = "energy_tracking_enabled")
    val energyTrackingEnabled: Boolean = false,

    @ColumnInfo(name = "notification_channels")
    val notificationChannels: String? = null,

    @ColumnInfo(name = "quiet_hours_enabled")
    val quietHoursEnabled: Boolean = true,

    @ColumnInfo(name = "quiet_hours_start")
    val quietHoursStart: String = "22:00",

    @ColumnInfo(name = "quiet_hours_end")
    val quietHoursEnd: String = "07:00",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
