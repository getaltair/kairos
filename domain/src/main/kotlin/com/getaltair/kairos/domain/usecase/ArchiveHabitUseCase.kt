package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.Instant
import kotlinx.coroutines.CancellationException

/**
 * Archives an active or paused habit.
 *
 * State machine: ACTIVE|PAUSED -> ARCHIVED. Sets [Habit.archivedAt] to now.
 * Rejects transitions from Archived status.
 */
class ArchiveHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(habitId: java.util.UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value

            if (habit.status is HabitStatus.Archived) {
                return Result.Error("Cannot archive habit: already archived")
            }

            val updated = habit.copy(
                status = HabitStatus.Archived,
                archivedAt = Instant.now()
            )
            habitRepository.update(updated)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to archive habit: ${e.message}", cause = e)
        }
    }
}
