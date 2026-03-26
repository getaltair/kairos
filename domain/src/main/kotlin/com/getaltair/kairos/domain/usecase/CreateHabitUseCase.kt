package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.validator.HabitValidator
import kotlinx.coroutines.CancellationException

/**
 * Creates a new habit after validating business rules.
 *
 * Validates the habit using [HabitValidator] before persisting.
 * Returns [Result.Error] if validation fails or the repository insert fails.
 */
class CreateHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(habit: Habit): Result<Habit> {
        return try {
            val validation = HabitValidator.validate(habit)
            if (validation is Result.Error) return validation

            habitRepository.insert(habit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to create habit: ${e.message}", cause = e)
        }
    }
}
