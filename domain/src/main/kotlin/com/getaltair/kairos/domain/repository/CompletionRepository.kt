package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import java.time.LocalDate
import java.util.UUID

/**
 * Repository interface for Completion entity operations.
 * Implemented in data layer with Room database.
 */
interface CompletionRepository {
    /**
     * Gets completion for a specific habit on a specific date.
     */
    suspend fun getForHabitOnDate(habitId: UUID, date: LocalDate): Result<Completion?>

    /**
     * Gets all completions for a specific date.
     */
    suspend fun getForDate(date: LocalDate): Result<List<Completion>>

    /**
     * Gets completions for a date range.
     */
    suspend fun getForDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Completion>>

    /**
     * Gets completions for a specific habit within a date range.
     */
    suspend fun getForHabitInDateRange(
        habitId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Completion>>

    /**
     * Inserts a new completion.
     * Uses REPLACE conflict strategy; duplicate prevention is enforced at the use case layer.
     */
    suspend fun insert(completion: Completion): Result<Completion>

    /**
     * Updates an existing completion.
     * Use copy() to create updated instance.
     */
    suspend fun update(completion: Completion): Result<Completion>

    /**
     * Deletes a completion.
     * Used for undo operations.
     */
    suspend fun delete(id: UUID): Result<Unit>

    /**
     * Gets the most recent completion for a habit.
     */
    suspend fun getLatestForHabit(habitId: UUID): Result<Completion?>

    /**
     * Deletes all completions for a habit.
     * Used for cascade deletion when a habit is permanently removed.
     */
    suspend fun deleteForHabit(habitId: UUID): Result<Unit>
}
