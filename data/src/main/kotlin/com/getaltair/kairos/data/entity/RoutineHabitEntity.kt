package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity associating a habit with a routine in a specific position.
 * RoutineHabit references (not owns) a Habit entity.
 *
 * @property id Unique identifier for this routine-habit association
 * @property routineId ID of the parent routine
 * @property habitId ID of the referenced habit
 * @property orderIndex Zero-based position in the routine sequence
 * @property overrideDurationSeconds Optional override for the habit's estimated duration
 * @property variantIds IDs of routine variants that include this habit
 * @property createdAt When this association was created
 * @property updatedAt When this association was last updated
 */
@Entity(
    tableName = "routine_habits",
    indices = [
        Index(value = ["routine_id", "order_index"], name = "idx_routine_order"),
        Index(value = ["habit_id"], name = "idx_habit_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineHabitEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "routine_id")
    val routineId: UUID,

    @ColumnInfo(name = "habit_id")
    val habitId: UUID,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "override_duration_seconds")
    val overrideDurationSeconds: Int? = null,

    @ColumnInfo(name = "variant_ids")
    val variantIds: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    init {
        require(orderIndex >= 0) {
            "orderIndex must be >= 0"
        }
        require(overrideDurationSeconds == null || overrideDurationSeconds > 0) {
            "overrideDurationSeconds must be positive if set"
        }
    }
}
