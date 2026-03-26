package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit

/**
 * Validates Habit entities against business rules.
 *
 * Rules:
 * - H-1: anchorBehavior must not be blank
 * - H-4: allowPartialCompletion must be true
 * - H-5: relapseThresholdDays must be > lapseThresholdDays
 * - H-6: Timestamp ordering constraints
 */
object HabitValidator {

    fun validate(habit: Habit): Result<Unit> {
        // H-1: anchorBehavior must not be blank
        if (habit.anchorBehavior.isBlank()) {
            return Result.Error("anchorBehavior must not be blank")
        }

        // H-4: allowPartialCompletion must be true
        if (!habit.allowPartialCompletion) {
            return Result.Error("allowPartialCompletion must be true")
        }

        // H-5: relapseThresholdDays must be > lapseThresholdDays
        if (habit.relapseThresholdDays <= habit.lapseThresholdDays) {
            return Result.Error(
                "relapseThresholdDays (${habit.relapseThresholdDays}) must be greater than lapseThresholdDays (${habit.lapseThresholdDays})"
            )
        }

        // H-6: Timestamp ordering constraints
        if (habit.createdAt > habit.updatedAt) {
            return Result.Error("createdAt must be <= updatedAt")
        }

        val pausedAt = habit.pausedAt
        if (pausedAt != null && pausedAt < habit.createdAt) {
            return Result.Error("pausedAt must be >= createdAt")
        }

        val archivedAt = habit.archivedAt
        if (archivedAt != null && archivedAt < habit.createdAt) {
            return Result.Error("archivedAt must be >= createdAt")
        }

        if (pausedAt != null && archivedAt != null && pausedAt > archivedAt) {
            return Result.Error("pausedAt must be <= archivedAt when both are set")
        }

        return Result.Success(Unit)
    }
}
