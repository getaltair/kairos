package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineHabit

/**
 * Validates Routine entities against business rules.
 *
 * Rules:
 * - R-1: A routine must contain at least 2 habits
 * - R-2: Order indices must be sequential (0, 1, 2, ...) with no gaps or duplicates
 * - R-4: All durations must be positive if set
 */
object RoutineValidator {

    private const val MAX_NAME_LENGTH = 50

    /**
     * Validates routine creation parameters.
     *
     * R-1: habitIds must have at least 2 entries.
     * Name must not be blank and must be 1-50 characters.
     */
    fun validateCreate(name: String, habitIds: List<java.util.UUID>): Result<Unit> {
        if (name.isBlank()) {
            return Result.Error("Routine name must not be blank")
        }
        if (name.length > MAX_NAME_LENGTH) {
            return Result.Error("Routine name must be $MAX_NAME_LENGTH characters or fewer")
        }
        if (habitIds.size < 2) {
            return Result.Error(
                "A routine must contain at least 2 habits (R-1), got ${habitIds.size}"
            )
        }
        return Result.Success(Unit)
    }

    /**
     * Validates order indices of routine habits.
     *
     * R-2: Order indices must start at 0, be sequential with no gaps, and have no duplicates.
     */
    fun validateOrderIndices(habits: List<RoutineHabit>): Result<Unit> {
        if (habits.isEmpty()) return Result.Success(Unit)

        val indices = habits.map { it.orderIndex }.sorted()
        val expected = (0 until habits.size).toList()

        if (indices != expected) {
            return Result.Error(
                "Order indices must be sequential starting at 0 (R-2): expected $expected, got $indices"
            )
        }
        return Result.Success(Unit)
    }

    /**
     * Validates a duration value.
     *
     * R-4: Duration must be positive if set.
     */
    fun validateDuration(seconds: Int?): Result<Unit> {
        if (seconds != null && seconds <= 0) {
            return Result.Error("Duration must be positive (R-4), got $seconds")
        }
        return Result.Success(Unit)
    }
}
