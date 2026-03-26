package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.validator.HabitValidator
import kotlinx.coroutines.CancellationException

/**
 * Edits an existing habit by applying field updates and re-validating.
 *
 * Fetches the current habit, applies the caller-supplied [Habit] (which should
 * be built via `habit.copy(...)` to preserve identity), validates with
 * [HabitValidator], and persists the update.
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
