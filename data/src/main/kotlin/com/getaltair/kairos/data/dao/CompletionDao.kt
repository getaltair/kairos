package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getaltair.kairos.data.entity.CompletionEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [CompletionEntity] operations.
 * Provides CRUD and specialized queries for completion data.
 */
@Dao
interface CompletionDao {

    /**
     * Get all completions.
     */
    @Query("SELECT * FROM completions ORDER BY date DESC, completed_at DESC")
    fun getAll(): List<CompletionEntity>

    /**
     * Get a completion by ID.
     */
    @Query("SELECT * FROM completions WHERE id = :id")
    fun getById(id: UUID): CompletionEntity?

    /**
     * Get completion for a specific habit on a specific date.
     * Enforces one-per-day constraint via unique index.
     */
    @Query("SELECT * FROM completions WHERE habit_id = :habitId AND date = :date LIMIT 1")
    fun getForHabitOnDate(habitId: UUID, date: String): CompletionEntity?

    /**
     * Get all completions for a specific day.
     */
    @Query("SELECT * FROM completions WHERE date = :date ORDER BY completed_at DESC")
    fun getForDate(date: String): List<CompletionEntity>

    /**
     * Get completions for a date range.
     * Used for weekly reports.
     */
    @Query(
        """
        SELECT * FROM completions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, completed_at DESC
    """
    )
    fun getForDateRange(startDate: String, endDate: String): List<CompletionEntity>

    /**
     * Get completion history for a specific habit within a date range.
     */
    @Query(
        """
        SELECT * FROM completions
        WHERE habit_id = :habitId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """
    )
    fun getForHabitInRange(
        habitId: UUID,
        startDate: String,
        endDate: String
    ): List<CompletionEntity>

    /**
     * Get completions by habit ID.
     */
    @Query(
        "SELECT * FROM completions WHERE habit_id = :habitId ORDER BY date DESC, completed_at DESC"
    )
    fun getForHabit(habitId: UUID): List<CompletionEntity>

    /**
     * Insert a new completion with REPLACE strategy.
     * Updates existing completion if habitId/date already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(completion: CompletionEntity)

    /**
     * Insert multiple completions with REPLACE strategy.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(completions: List<CompletionEntity>)

    /**
     * Update a completion.
     */
    @Query(
        """
        UPDATE completions SET
            completed_at = :completedAt,
            type = :type,
            partial_percent = :partialPercent,
            skip_reason = :skipReason,
            energy_level = :energyLevel,
            note = :note,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(
        id: UUID,
        completedAt: Long,
        type: String,
        partialPercent: Int?,
        skipReason: String?,
        energyLevel: Int?,
        note: String?,
        updatedAt: Long
    )

    /**
     * Delete a completion.
     */
    @Query("DELETE FROM completions WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all completions for a habit.
     */
    @Query("DELETE FROM completions WHERE habit_id = :habitId")
    fun deleteForHabit(habitId: UUID)

    /**
     * Delete all completions in a date range.
     */
    @Query("DELETE FROM completions WHERE date BETWEEN :startDate AND :endDate")
    fun deleteForDateRange(startDate: String, endDate: String)

    /**
     * Delete all completions.
     */
    @Query("DELETE FROM completions")
    fun deleteAll()

    /**
     * Get completions as Flow for reactive updates.
     */
    @Query("SELECT * FROM completions ORDER BY date DESC, completed_at DESC")
    fun getAllFlow(): Flow<List<CompletionEntity>>
}
