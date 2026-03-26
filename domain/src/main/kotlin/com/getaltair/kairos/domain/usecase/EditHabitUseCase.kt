package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.validator.HabitValidator
import kotlinx.coroutines.CancellationException

/**
 * Validates and persists an updated habit. Verifies the habit exists before applying changes.
 */
class EditHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(updatedHabit: Habit): Result<Habit> {
        return try {
            val existing = habitRepository.getById(updatedHabit.id)
            if (existing is Result.Error) {
                return Result.Error("Habit not found: ${existing.message}")
            }

            val validation = HabitValidator.validate(updatedHabit)
            if (validation is Result.Error) return validation

            habitRepository.update(updatedHabit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to edit habit: ${e.message}", cause = e)
        }
    }
}
