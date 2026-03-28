package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.HabitRepository
import kotlinx.coroutines.CancellationException

/**
 * Retrieves all active habits.
 * Used by the routine builder to allow users to select habits for a routine.
 */
class GetActiveHabitsUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(): Result<List<Habit>> = try {
        habitRepository.getActiveHabits()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to load active habits: ${e.message}", cause = e)
    }
}
