package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.repository.RoutineRepository
import com.getaltair.kairos.domain.validator.RoutineValidator
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Creates a new routine after validating business rules.
 *
 * Validates R-1 (minimum 2 habits) via [RoutineValidator] before persisting.
 * Also validates optional duration overrides via R-4 (positive duration).
 * Returns [Result.Error] if validation fails or the repository insert fails.
 */
class CreateRoutineUseCase(private val routineRepository: RoutineRepository) {

    suspend operator fun invoke(
        name: String,
        category: HabitCategory,
        habitIds: List<UUID>,
        durations: Map<UUID, Int?> = emptyMap(),
    ): Result<Routine> {
        return try {
            val validation = RoutineValidator.validateCreate(name, habitIds)
            if (validation is Result.Error) return validation

            // R-4: Validate any provided duration overrides
            for ((_, seconds) in durations) {
                val durationValidation = RoutineValidator.validateDuration(seconds)
                if (durationValidation is Result.Error) return durationValidation
            }

            val routine = Routine(
                name = name,
                category = category,
            )

            routineRepository.insert(routine, habitIds)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to create routine: ${e.message}", cause = e)
        }
    }
}
