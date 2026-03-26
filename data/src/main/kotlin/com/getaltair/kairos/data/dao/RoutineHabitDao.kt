package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getaltair.kairos.data.entity.RoutineHabitEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [RoutineHabitEntity] operations.
 * Provides CRUD and ordered queries for routine-habit associations.
 */
@Dao
interface RoutineHabitDao {

    /**
     * Get all routine-habit associations.
     */
    @Query("SELECT * FROM routine_habits ORDER BY created_at DESC")
    fun getAll(): List<RoutineHabitEntity>

    /**
     * Get a routine-habit association by ID.
     */
    @Query("SELECT * FROM routine_habits WHERE id = :id")
    fun getById(id: UUID): RoutineHabitEntity?

    /**
     * Get all habits for a routine, ordered by order_index.
     */
    @Query(
        """
        SELECT * FROM routine_habits
        WHERE routine_id = :routineId
        ORDER BY order_index ASC
    """
    )
    fun getByRoutineId(routineId: UUID): List<RoutineHabitEntity>

    /**
     * Get routine-habit associations by habit ID.
     */
    @Query("SELECT * FROM routine_habits WHERE habit_id = :habitId ORDER BY created_at DESC")
    fun getByHabitId(habitId: UUID): List<RoutineHabitEntity>

    /**
     * Get routine-habit associations by user ID (for sync).
     */
    @Query(
        """
        SELECT rh.* FROM routine_habits rh
        INNER JOIN routines r ON rh.routine_id = r.id
        WHERE r.user_id = :userId
        ORDER BY rh.created_at DESC
    """
    )
    fun getByUserId(userId: String): List<RoutineHabitEntity>

    /**
     * Insert a new routine-habit association.
     */
    @Insert
    fun insert(routineHabit: RoutineHabitEntity)

    /**
     * Insert multiple routine-habit associations.
     */
    @Insert
    fun insertAll(routineHabits: List<RoutineHabitEntity>)

    /**
     * Update a routine-habit association.
     */
    @Query(
        """
        UPDATE routine_habits SET
            habit_id = :habitId,
            order_index = :orderIndex,
            override_duration_seconds = :overrideDurationSeconds,
            variant_ids = :variantIds,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(
        id: UUID,
        habitId: UUID,
        orderIndex: Int,
        overrideDurationSeconds: Int?,
        variantIds: String?,
        updatedAt: Long
    )

    /**
     * Delete a routine-habit association.
     */
    @Query("DELETE FROM routine_habits WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all habits for a routine.
     */
    @Query("DELETE FROM routine_habits WHERE routine_id = :routineId")
    fun deleteByRoutineId(routineId: UUID)

    /**
     * Delete a habit from all routines.
     */
    @Query("DELETE FROM routine_habits WHERE habit_id = :habitId")
    fun deleteByHabitId(habitId: UUID)

    /**
     * Delete all routine-habit associations.
     */
    @Query("DELETE FROM routine_habits")
    fun deleteAll()

    /**
     * Get routine-habit associations as Flow for reactive updates.
     */
    @Query("SELECT * FROM routine_habits ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<RoutineHabitEntity>>
}
