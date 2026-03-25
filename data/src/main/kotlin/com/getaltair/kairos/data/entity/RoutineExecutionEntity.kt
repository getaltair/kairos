package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.getaltair.kairos.data.converter.ExecutionStatusConverter
import com.getaltair.kairos.domain.enums.ExecutionStatus
import java.util.UUID

/**
 * Room entity representing a single run of a routine.
 * Tracks the execution state of a routine instance.
 *
 * @property id Unique identifier for this execution
 * @property routineId ID of parent routine
 * @property variantId ID of the variant being used (nullable)
 * @property startedAt When the routine execution started
 * @property completedAt When the routine execution completed (nullable)
 * @property status Current execution status
 * @property currentStepIndex Index of the current step being executed
 * @property currentStepRemainingSeconds Remaining seconds for the current step (nullable)
 * @property totalPausedSeconds Total seconds spent paused
 * @property createdAt When this execution record was created
 * @property updatedAt When this execution record was last updated
 */
@Entity(
    tableName = "routine_executions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["routineId"]),
        Index(value = ["variantId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@TypeConverters(ExecutionStatusConverter::class)
data class RoutineExecutionEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "routine_id")
    val routineId: UUID,

    @ColumnInfo(name = "variant_id")
    val variantId: UUID? = null,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "status")
    val status: ExecutionStatus,

    @ColumnInfo(name = "current_step_index")
    val currentStepIndex: Int = 0,

    @ColumnInfo(name = "current_step_remaining_seconds")
    val currentStepRemainingSeconds: Int? = null,

    @ColumnInfo(name = "total_paused_seconds")
    val totalPausedSeconds: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    init {
        require(currentStepIndex >= 0) {
            "currentStepIndex must be >= 0"
        }
        require(currentStepRemainingSeconds == null || currentStepRemainingSeconds > 0) {
            "currentStepRemainingSeconds must be positive if set"
        }
        require(totalPausedSeconds >= 0) {
            "totalPausedSeconds must be >= 0"
        }
    }
}
