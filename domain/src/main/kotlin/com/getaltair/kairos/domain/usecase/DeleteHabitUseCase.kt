package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Permanently deletes an archived habit and all its completions.
 *
 * State machine: ARCHIVED -> (deleted). Only archived habits can be deleted.
 * Cascade: completions are deleted first, then the habit itself.
 */
class DeleteHabitUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(habitId: UUID): Result<Unit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value

            if (habit.status !is HabitStatus.Archived) {
                return Result.Error(
                    "Cannot delete habit: current status is ${habit.status.displayName}, expected Archived"
                )
            }

            val deleteCompletions = completionRepository.deleteForHabit(habitId)
            if (deleteCompletions is Result.Error) return deleteCompletions

            habitRepository.delete(habitId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to delete habit: ${e.message}", cause = e)
        }
    }
}
