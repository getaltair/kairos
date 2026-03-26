package com.getaltair.kairos.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [HabitEntity] operations.
 * Provides CRUD and specialized queries for habit data.
 */
@Dao
interface HabitDao {

    /**
     * Get all habits.
     */
    @Query("SELECT * FROM habits ORDER BY created_at DESC")
    fun getAll(): List<HabitEntity>

    /**
     * Get a habit by ID.
     */
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getById(id: UUID): HabitEntity?

    /**
     * Insert a new habit.
     */
    @Insert
    fun insert(habit: HabitEntity)

    /**
     * Insert multiple habits.
     */
    @Insert
    fun insertAll(habits: List<HabitEntity>)

    /**
     * Update a habit.
     */
    @Query(
        """
        UPDATE habits SET
            name = :name,
            description = :description,
            icon = :icon,
            color = :color,
            anchor_behavior = :anchorBehavior,
            anchor_type = :anchorType,
            time_window_start = :timeWindowStart,
            time_window_end = :timeWindowEnd,
            category = :category,
            frequency = :frequency,
            active_days = :activeDays,
            estimated_seconds = :estimatedSeconds,
            micro_version = :microVersion,
            allow_partial_completion = :allowPartialCompletion,
            subtasks = :subtasks,
            phase = :phase,
            status = :status,
            paused_at = :pausedAt,
            archived_at = :archivedAt,
            lapse_threshold_days = :lapseThresholdDays,
            relapse_threshold_days = :relapseThresholdDays,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(
        id: UUID,
        name: String,
        description: String?,
        icon: String?,
        color: String?,
        anchorBehavior: String,
        anchorType: String,
        timeWindowStart: String?,
        timeWindowEnd: String?,
        category: HabitCategory,
        frequency: String,
        activeDays: String?,
        estimatedSeconds: Int,
        microVersion: String?,
        allowPartialCompletion: Boolean,
        subtasks: String?,
        phase: String,
        status: HabitStatus,
        pausedAt: Long?,
        archivedAt: Long?,
        lapseThresholdDays: Int,
        relapseThresholdDays: Int,
        updatedAt: Long = Instant.now().toEpochMilli()
    )

    /**
     * Delete a habit.
     */
    @Query("DELETE FROM habits WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all habits.
     */
    @Query("DELETE FROM habits")
    fun deleteAll()

    /**
     * Get habits by status.
     */
    @Query("SELECT * FROM habits WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: String): List<HabitEntity>

    /**
     * Get active habits (status = Active and not paused/archived).
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE status = 'Active'
        AND paused_at IS NULL
        AND archived_at IS NULL
        ORDER BY created_at DESC
    """
    )
    fun getActiveHabits(): List<HabitEntity>

    /**
     * Get habits by status and category.
     * Used for management screens and departure filtering.
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE status = :status
        AND category = :category
        ORDER BY created_at DESC
    """
    )
    fun getHabitsByStatusAndCategory(status: HabitStatus, category: HabitCategory): List<HabitEntity>

    /**
     * Get lapsed habits based on missed days threshold.
     * Used for lapse detection.
     *
     * Lapse detection algorithm:
     * - Finds active, unpaused, unarchived habits
     * - Joins with completions to find the most recent completion date
     * - Habits with no completions (NULL MAX(c.date)) are NOT included
     *   because NULL < comparison always returns false
     * - Returns habits whose last completion was before the threshold date
     * - Results ordered by oldest completion first (highest lapse priority)
     *
     * @param thresholdDays Number of days back to check for lapse.
     *   Habits with no completions in the last `thresholdDays` days
     *   are considered lapsed.
     */
    @Query(
        """
        SELECT h.* FROM habits h
        LEFT JOIN completions c ON h.id = c.habit_id
        WHERE h.status = 'Active'
        AND h.paused_at IS NULL
        AND h.archived_at IS NULL
        GROUP BY h.id
        HAVING MAX(c.date) < datetime('now', '-' || :thresholdDays || ' days')
        ORDER BY MAX(c.date) ASC
    """
    )
    fun getLapsedHabits(thresholdDays: Int): List<HabitEntity>

    /**
     * Get habits by phase.
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE phase = :phase
        ORDER BY created_at DESC
    """
    )
    fun getHabitsByPhase(phase: HabitPhase): List<HabitEntity>

    /**
     * Get today's habits with completion status.
     * JOIN query for the Today screen.
     */
    @Transaction
    @Query(
        """
        SELECT
            h.*,
            c.id as completion_id,
            c.date as completion_date,
            c.completed_at,
            c.type as completion_type,
            c.partial_percent,
            c.skip_reason,
            c.energy_level,
            c.note
        FROM habits h
        LEFT JOIN completions c
            ON h.id = c.habit_id AND c.date = date('now', 'localtime')
        WHERE h.status = 'Active'
        AND h.paused_at IS NULL
        AND h.archived_at IS NULL
        ORDER BY
            CASE
                WHEN h.category = 'Morning' THEN 1
                WHEN h.category = 'Afternoon' THEN 2
                WHEN h.category = 'Evening' THEN 3
                ELSE 4
            END,
            h.created_at DESC
    """
    )
    fun getTodayHabitsWithCompletions(): Flow<List<TodayHabitWithCompletion>>

    /**
     * Get habits by category.
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE category = :category
        ORDER BY created_at DESC
    """
    )
    fun getByCategory(category: HabitCategory): List<HabitEntity>

    /**
     * Get habits by user ID (for sync).
     */
    @Query("SELECT * FROM habits WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserId(userId: String): List<HabitEntity>

    /**
     * Get active habits as Flow.
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE status = 'Active'
        AND paused_at IS NULL
        AND archived_at IS NULL
        ORDER BY created_at DESC
    """
    )
    fun getActiveHabitsFlow(): Flow<List<HabitEntity>>
}

/**
 * Wrapper class for today's habits with completion information.
 */
data class TodayHabitWithCompletion(
    val id: UUID,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    @ColumnInfo(name = "anchor_behavior") val anchorBehavior: String,
    @ColumnInfo(name = "anchor_type") val anchorType: String,
    @ColumnInfo(name = "time_window_start") val timeWindowStart: String?,
    @ColumnInfo(name = "time_window_end") val timeWindowEnd: String?,
    val category: String,
    val frequency: String,
    @ColumnInfo(name = "active_days") val activeDays: String?,
    @ColumnInfo(name = "estimated_seconds") val estimatedSeconds: Int,
    @ColumnInfo(name = "micro_version") val microVersion: String?,
    @ColumnInfo(name = "allow_partial_completion") val allowPartialCompletion: Boolean,
    val subtasks: String?,
    val phase: String,
    val status: String,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "paused_at") val pausedAt: Long?,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
    @ColumnInfo(name = "lapse_threshold_days") val lapseThresholdDays: Int,
    @ColumnInfo(name = "relapse_threshold_days") val relapseThresholdDays: Int,
    // Completion fields (nullable)
    val completion_id: UUID?,
    val completion_date: String?,
    val completed_at: Long?,
    val completion_type: String?,
    val partial_percent: Int?,
    val skip_reason: String?,
    val energy_level: Int?,
    val note: String?
)
