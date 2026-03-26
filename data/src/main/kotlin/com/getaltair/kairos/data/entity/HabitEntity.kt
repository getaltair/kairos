package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing a habit.
 * The fundamental unit of behavior change in Kairos.
 *
 * @property id Unique identifier for this habit
 * @property name Display name (1-100 characters, required)
 * @property description Optional description of the habit
 * @property icon Emoji or icon reference for UI
 * @property color Hex color code for UI
 * @property anchorBehavior The context trigger (e.g., "After brushing teeth")
 * @property anchorType The type of anchor (after/before/location/time)
 * @property timeWindowStart Optional start time for AT_TIME anchor
 * @property timeWindowEnd Optional end time for AT_TIME anchor
 * @property category Time-of-day category for the habit
 * @property frequency How often the habit should be done
 * @property activeDays Set of days (for CUSTOM frequency)
 * @property estimatedSeconds Estimated duration in seconds (default 300)
 * @property microVersion Optional smaller habit version for flexibility
 * @property allowPartialCompletion Whether partial completion is allowed (always true)
 * @property subtasks Ordered list of subtasks
 * @property phase Current habit phase in lifecycle
 * @property status Current habit status (active/paused/archived)
 * @property userId ID of the user (for sync, nullable)
 * @property createdAt When the habit was created
 * @property updatedAt When the habit was last updated
 * @property pausedAt When the habit was paused (nullable)
 * @property archivedAt When the habit was archived (nullable)
 * @property lapseThresholdDays Days missed before triggering lapse (default 3)
 * @property relapseThresholdDays Days missed before triggering relapse (default 7)
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["status", "category"]),
        Index(value = ["phase"]),
        Index(value = ["user_id"])
    ]
)
data class HabitEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "anchor_behavior")
    val anchorBehavior: String,

    @ColumnInfo(name = "anchor_type")
    val anchorType: String,

    @ColumnInfo(name = "time_window_start")
    val timeWindowStart: String? = null,

    @ColumnInfo(name = "time_window_end")
    val timeWindowEnd: String? = null,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "frequency")
    val frequency: String,

    @ColumnInfo(name = "active_days")
    val activeDays: String? = null,

    @ColumnInfo(name = "estimated_seconds")
    val estimatedSeconds: Int = 300,

    @ColumnInfo(name = "micro_version")
    val microVersion: String? = null,

    @ColumnInfo(name = "allow_partial_completion")
    val allowPartialCompletion: Boolean = true,

    @ColumnInfo(name = "subtasks")
    val subtasks: String? = null,

    @ColumnInfo(name = "phase")
    val phase: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "paused_at")
    val pausedAt: Long? = null,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,

    @ColumnInfo(name = "lapse_threshold_days")
    val lapseThresholdDays: Int = 3,

    @ColumnInfo(name = "relapse_threshold_days")
    val relapseThresholdDays: Int = 7
)
