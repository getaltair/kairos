package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.getaltair.kairos.data.entity.RoutineExecutionEntity
import com.getaltair.kairos.domain.enums.ExecutionStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [RoutineExecutionEntity] operations.
 * Provides CRUD and specialized queries for routine execution data.
 */
@Dao
interface RoutineExecutionDao {

    /**
     * Get all routine executions.
     */
    @Query("SELECT * FROM routine_executions ORDER BY started_at DESC")
    fun getAll(): List<RoutineExecutionEntity>

    /**
     * Get a routine execution by ID.
     */
    @Query("SELECT * FROM routine_executions WHERE id = :id")
    fun getById(id: UUID): RoutineExecutionEntity?

    /**
     * Get all executions for a specific routine.
     */
    @Query(
        """
        SELECT * FROM routine_executions
        WHERE routine_id = :routineId
        ORDER BY started_at DESC
    """
    )
    fun getByRoutineId(routineId: UUID): List<RoutineExecutionEntity>

    /**
     * Get the active execution for a specific routine.
     * Returns null if no active execution exists.
     */
    @Query(
        """
        SELECT * FROM routine_executions
        WHERE routine_id = :routineId AND status = 'InProgress'
        ORDER BY started_at DESC
        LIMIT 1
    """
    )
    fun getActiveForRoutine(routineId: UUID): RoutineExecutionEntity?

    /**
     * Get all in-progress executions.
     */
    @Query("SELECT * FROM routine_executions WHERE status = 'InProgress' ORDER BY started_at DESC")
    fun getActiveExecutions(): List<RoutineExecutionEntity>

    /**
     * Get executions by status.
     */
    @Query("SELECT * FROM routine_executions WHERE status = :status ORDER BY started_at DESC")
    fun getByStatus(status: ExecutionStatus): List<RoutineExecutionEntity>

    /**
     * Get completed executions for a routine.
     */
    @Query(
        """
        SELECT * FROM routine_executions
        WHERE routine_id = :routineId AND status = 'Completed'
        ORDER BY started_at DESC
    """
    )
    fun getCompletedForRoutine(routineId: UUID): List<RoutineExecutionEntity>

    /**
     * Insert a new routine execution.
     */
    @Insert
    fun insert(execution: RoutineExecutionEntity)

    /**
     * Insert multiple routine executions.
     */
    @Insert
    fun insertAll(executions: List<RoutineExecutionEntity>)

    /**
     * Insert or update a routine execution.
     * If an execution with the same primary key exists, it is updated; otherwise, inserted.
     */
    @Upsert
    suspend fun upsert(entity: RoutineExecutionEntity)

    /**
     * Update a routine execution.
     */
    @Query(
        """
        UPDATE routine_executions SET
            completed_at = :completedAt,
            status = :status,
            current_step_index = :currentStepIndex,
            current_step_remaining_seconds = :currentStepRemainingSeconds,
            total_paused_seconds = :totalPausedSeconds,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(
        id: UUID,
        completedAt: Long?,
        status: ExecutionStatus,
        currentStepIndex: Int,
        currentStepRemainingSeconds: Int?,
        totalPausedSeconds: Int,
        updatedAt: Long
    )

    /**
     * Update execution status.
     * Used for starting, pausing, resuming, or completing an execution.
     */
    @Query(
        """
        UPDATE routine_executions SET
            status = :status,
            completed_at = CASE
                WHEN :status = 'Completed' THEN :completedAt
                ELSE completed_at
            END,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun updateStatus(id: UUID, status: ExecutionStatus, completedAt: Long?, updatedAt: Long)

    /**
     * Delete a routine execution.
     */
    @Query("DELETE FROM routine_executions WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all executions for a routine.
     */
    @Query("DELETE FROM routine_executions WHERE routine_id = :routineId")
    fun deleteByRoutineId(routineId: UUID)

    /**
     * Delete all routine executions.
     */
    @Query("DELETE FROM routine_executions")
    fun deleteAll()

    /**
     * Get routine executions as Flow for reactive updates.
     */
    @Query("SELECT * FROM routine_executions ORDER BY started_at DESC")
    fun getAllFlow(): Flow<List<RoutineExecutionEntity>>
}
