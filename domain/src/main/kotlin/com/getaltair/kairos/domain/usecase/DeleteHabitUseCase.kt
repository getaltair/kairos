package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Permanently deletes an archived habit.
 *
 * State machine: ARCHIVED -> (deleted). Only archived habits can be deleted.
 * Completions are cascade-deleted by the database (Room ON DELETE CASCADE).
 */
class DeleteHabitUseCase(private val habitRepository: HabitRepository) {

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

            habitRepository.delete(habitId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to delete habit: ${e.message}", cause = e)
        }
    }
}
