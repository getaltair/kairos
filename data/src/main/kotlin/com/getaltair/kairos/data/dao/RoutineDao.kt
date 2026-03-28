package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.data.entity.RoutineHabitEntity
import com.getaltair.kairos.domain.enums.RoutineStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [RoutineEntity] operations.
 * Provides CRUD and specialized queries for routine data.
 */
@Dao
interface RoutineDao {

    /**
     * Get all routines.
     */
    @Query("SELECT * FROM routines ORDER BY created_at DESC")
    fun getAll(): List<RoutineEntity>

    /**
     * Get a routine by ID.
     */
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getById(id: UUID): RoutineEntity?

    /**
     * Get active routines.
     */
    @Query("SELECT * FROM routines WHERE status = 'Active' ORDER BY created_at DESC")
    fun getActiveRoutines(): List<RoutineEntity>

    /**
     * Get routines by status.
     */
    @Query("SELECT * FROM routines WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: RoutineStatus): List<RoutineEntity>

    /**
     * Insert a new routine.
     */
    @Insert
    fun insert(routine: RoutineEntity)

    /**
     * Atomically insert a routine and its associated habit entries.
     * Both inserts succeed or both roll back.
     */
    @Transaction
    fun insertWithHabits(routine: RoutineEntity, habits: List<RoutineHabitEntity>) {
        insert(routine)
        insertAllHabits(habits)
    }

    /**
     * Insert multiple routine-habit associations (internal helper for [insertWithHabits]).
     */
    @Insert
    fun insertAllHabits(habits: List<RoutineHabitEntity>)

    /**
     * Insert multiple routines.
     */
    @Insert
    fun insertAll(routines: List<RoutineEntity>)

    /**
     * Insert or update a routine.
     * If a routine with the same primary key exists, it is updated; otherwise, inserted.
     */
    @Upsert
    suspend fun upsert(entity: RoutineEntity)

    /**
     * Update a routine.
     */
    @Query(
        """
        UPDATE routines SET
            name = :name,
            description = :description,
            icon = :icon,
            color = :color,
            category = :category,
            status = :status,
            user_id = :userId,
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
        category: String,
        status: RoutineStatus,
        userId: String?,
        updatedAt: Long
    )

    /**
     * Delete a routine.
     */
    @Query("DELETE FROM routines WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all routines.
     */
    @Query("DELETE FROM routines")
    fun deleteAll()

    /**
     * Get routines by user ID (for sync).
     */
    @Query("SELECT * FROM routines WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserId(userId: String): List<RoutineEntity>

    /**
     * Get routines as Flow for reactive updates.
     */
    @Query("SELECT * FROM routines ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<RoutineEntity>>
}
