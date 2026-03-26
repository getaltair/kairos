package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.HabitRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

class GetHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get habit: ${e.message}", cause = e)
        }
    }
}
