package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing a record of habit execution for a specific day.
 * Each completion is tied to a specific habit and date.
 *
 * @property id Unique identifier for this completion
 * @property habitId ID of the associated habit
 * @property date The habit date (date when the habit was due)
 * @property completedAt When the completion was logged
 * @property type The type of completion (FULL, PARTIAL, SKIPPED, MISSED)
 * @property partialPercent Percentage for partial completions (1-99)
 * @property skipReason Optional reason for skipping
 * @property energyLevel User's energy level (1-5) during completion
 * @property note Optional free text note
 * @property createdAt When this completion record was created
 * @property updatedAt When this completion record was last updated
 */
@Entity(
    tableName = "completions",
    indices = [
        Index(value = ["habit_id", "date"], name = "idx_habit_date"),
        Index(value = ["date"])
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
data class CompletionEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "habit_id")
    val habitId: UUID,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "partial_percent")
    val partialPercent: Int? = null,

    @ColumnInfo(name = "skip_reason")
    val skipReason: String? = null,

    @ColumnInfo(name = "energy_level")
    val energyLevel: Int? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    init {
        require(type != "Partial" || partialPercent != null) {
            "partialPercent must only be set for PARTIAL type"
        }
        require(type == "Partial" || partialPercent == null) {
            "partialPercent must not be set for non-PARTIAL type"
        }
        require(type != "Skipped" || skipReason != null) {
            "skipReason must only be set for SKIPPED type"
        }
        require(type == "Skipped" || skipReason == null) {
            "skipReason must not be set for non-SKIPPED type"
        }
    }
}
