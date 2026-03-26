package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import java.time.LocalDate

/**
 * Validates Completion entities against business rules.
 *
 * Rules:
 * - C-2: Partial completion percent constraints
 * - C-4: No future completions
 * - C-5: Limited backdating (7 days max)
 */
object CompletionValidator {

    fun validate(completion: Completion, today: LocalDate = LocalDate.now()): Result<Unit> {
        // C-2: Partial completion percent constraints
        if (completion.type is CompletionType.Partial) {
            val percent = completion.partialPercent
            if (percent == null || percent !in 1..99) {
                return Result.Error("partialPercent must be in 1..99 for PARTIAL completions")
            }
        } else {
            if (completion.partialPercent != null) {
                return Result.Error("partialPercent must be null for non-PARTIAL completions")
            }
        }

        // C-4: No future completions
        if (completion.date > today) {
            return Result.Error("Completion date must not be in the future")
        }

        // C-5: Limited backdating (7 days max)
        val earliestAllowed = today.minusDays(7)
        if (completion.date < earliestAllowed) {
            return Result.Error("Completion date must be within the last 7 days")
        }

        return Result.Success(Unit)
    }
}
