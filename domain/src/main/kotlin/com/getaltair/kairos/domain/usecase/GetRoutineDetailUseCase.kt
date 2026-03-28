package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.model.RoutineStep
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RoutineRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Gets a routine with its habit steps, each resolved to full Habit details.
 *
 * Returns the routine paired with a list of [RoutineStep] entries
 * in the order returned by the repository. This provides both the routine
 * association metadata and the full habit entity for display.
 *
 * Returns [Result.Error] if the routine is not found or a repository call fails.
 */
class GetRoutineDetailUseCase(
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository,
) {

    suspend operator fun invoke(routineId: UUID): Result<Pair<Routine, List<RoutineStep>>> {
        return try {
            val routineWithHabitsResult = routineRepository.getRoutineWithHabits(routineId)
            if (routineWithHabitsResult is Result.Error) {
                return Result.Error("Failed to load routine: ${routineWithHabitsResult.message}")
            }

            val routineWithHabits = (routineWithHabitsResult as Result.Success).value
                ?: return Result.Error("Routine not found")

            val (routine, routineHabits) = routineWithHabits

            val steps = routineHabits.map { routineHabit ->
                val habitResult = habitRepository.getById(routineHabit.habitId)
                if (habitResult is Result.Error) {
                    return Result.Error(
                        "Failed to load habit ${routineHabit.habitId}: ${habitResult.message}"
                    )
                }
                val habit = (habitResult as Result.Success).value
                RoutineStep(routineHabit, habit)
            }

            Result.Success(Pair(routine, steps))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get routine detail: ${e.message}", cause = e)
        }
    }
}
