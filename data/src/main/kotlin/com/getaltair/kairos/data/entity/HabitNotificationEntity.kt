package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.util.UUID

/**
 * Room entity representing a notification for a habit.
 *
 * @property id Unique identifier for this notification
 * @property habitId ID of the associated habit
 * @property time Time for the notification
 * @property isEnabled Whether this notification is enabled
 * @property createdAt When this notification was created
 * @property updatedAt When this notification was last updated
 */
@Entity(
    tableName = "habit_notifications",
    indices = [
        Index(value = ["habit_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitNotificationEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "habit_id")
    val habitId: UUID,

    @ColumnInfo(name = "time")
    val time: LocalTime,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
