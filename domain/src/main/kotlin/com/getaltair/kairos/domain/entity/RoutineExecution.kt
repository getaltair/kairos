package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.ExecutionStatus
import java.time.Instant
import java.util.UUID

/**
 * A single run of a routine.
 * Tracks the execution state of a routine instance.
 *
 * @property id Unique identifier for this execution
 * @property routineId ID of the parent routine
 * @property variantId ID of the variant being used (nullable)
 * @property startedAt When the routine execution started
 * @property completedAt When the routine execution completed (nullable)
 * @property status Current execution status
 * @property currentStepIndex Index of the current step being executed
 * @property currentStepRemainingSeconds Remaining seconds for current step (nullable)
 * @property totalPausedSeconds Total seconds spent paused
 * @property createdAt When this execution record was created
 * @property updatedAt When this execution record was last updated
 */
data class RoutineExecution(
    val id: UUID = UUID.randomUUID(),
    val routineId: UUID,
    val variantId: UUID? = null,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val status: ExecutionStatus = ExecutionStatus.NotStarted,
    val currentStepIndex: Int = 0,
    val currentStepRemainingSeconds: Int? = null,
    val totalPausedSeconds: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
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
        require(completedAt == null || status == ExecutionStatus.Completed || status == ExecutionStatus.Abandoned) {
            "completedAt may only be set for terminal executions"
        }
    }

    /**
     * Creates a copy of this execution with the specified changes.
     */
    fun copy(
        status: ExecutionStatus = this.status,
        currentStepIndex: Int = this.currentStepIndex,
        currentStepRemainingSeconds: Int? = this.currentStepRemainingSeconds,
        completedAt: Instant? = this.completedAt,
        totalPausedSeconds: Int = this.totalPausedSeconds,
        updatedAt: Instant = Instant.now()
    ): RoutineExecution = RoutineExecution(
        id = this.id,
        routineId = this.routineId,
        variantId = this.variantId,
        startedAt = this.startedAt,
        completedAt = completedAt,
        status = status,
        currentStepIndex = currentStepIndex,
        currentStepRemainingSeconds = currentStepRemainingSeconds,
        totalPausedSeconds = totalPausedSeconds,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )

    /**
     * Checks if the execution is currently active.
     */
    val isActive: Boolean
        get() = status == ExecutionStatus.InProgress || status == ExecutionStatus.Paused

    /**
     * Checks if the execution has reached a terminal state.
     */
    val isTerminal: Boolean
        get() = status == ExecutionStatus.Completed || status == ExecutionStatus.Abandoned

    /**
     * Checks if the execution is complete.
     */
    val isComplete: Boolean
        get() = status == ExecutionStatus.Completed
}
