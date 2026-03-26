package com.getaltair.kairos.domain.entity

import java.util.UUID

/**
 * Associates a habit with a routine in a specific position.
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
data class RoutineHabit(
    val id: UUID = UUID.randomUUID(),
    val routineId: UUID,
    val habitId: UUID,
    val orderIndex: Int,
    val overrideDurationSeconds: Int? = null,
    val variantIds: List<UUID>? = null,
    val createdAt: java.time.Instant = java.time.Instant.now(),
    val updatedAt: java.time.Instant = java.time.Instant.now()
) {
    init {
        require(orderIndex >= 0) {
            "orderIndex must be >= 0"
        }
        require(overrideDurationSeconds == null || overrideDurationSeconds > 0) {
            "overrideDurationSeconds must be positive if set"
        }
    }

    /**
     * Creates a copy of this routine-habit with the specified changes.
     */
    fun copy(
        orderIndex: Int = this.orderIndex,
        overrideDurationSeconds: Int? = this.overrideDurationSeconds,
        variantIds: List<UUID>? = this.variantIds,
        updatedAt: java.time.Instant = java.time.Instant.now()
    ): RoutineHabit = RoutineHabit(
        id = this.id,
        routineId = this.routineId,
        habitId = this.habitId,
        orderIndex = orderIndex,
        overrideDurationSeconds = overrideDurationSeconds,
        variantIds = variantIds,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )
}
