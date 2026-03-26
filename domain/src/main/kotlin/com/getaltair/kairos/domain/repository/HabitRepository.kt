package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import java.time.LocalDate
import java.util.UUID

/**
 * Repository interface for Habit entity operations.
 * Implemented in data layer with Room database.
 */
interface HabitRepository {
    /**
     * Gets a habit by its ID.
     */
    suspend fun getById(id: UUID): Result<Habit>

    /**
     * Gets all active habits.
     * Active habits are those with status = Active and pausedAt = null.
     */
    suspend fun getActiveHabits(): Result<List<Habit>>

    /**
     * Gets habits due for a specific date.
     * Filters by frequency and checks habit phase.
     */
    suspend fun getHabitsForDate(date: LocalDate): Result<List<Habit>>

    /**
     * Gets habits by status.
     */
    suspend fun getByStatus(status: HabitStatus): Result<List<Habit>>

    /**
     * Gets habits by category.
     */
    suspend fun getByCategory(category: HabitCategory): Result<List<Habit>>

    /**
     * Gets habits in LAPSED phase.
     * Used for lapse detection.
     */
    suspend fun getLapsedHabits(): Result<List<Habit>>

    /**
     * Inserts a new habit.
     */
    suspend fun insert(habit: Habit): Result<Habit>

    /**
     * Updates an existing habit.
     * Use copy() to create updated instance.
     */
    suspend fun update(habit: Habit): Result<Habit>

    /**
     * Deletes a habit.
     * Cascade delete is handled in data layer.
     */
    suspend fun delete(id: UUID): Result<Unit>
}
