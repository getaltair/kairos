package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import java.util.UUID

/**
 * Repository interface for RoutineExecution entity operations.
 * Implemented in data layer with Room database.
 */
interface RoutineExecutionRepository {
    /**
     * Gets an execution by its ID.
     */
    suspend fun getById(id: UUID): Result<RoutineExecution?>

    /**
     * Gets the active (InProgress or Paused) execution for a routine.
     * Returns null if no active execution exists.
     */
    suspend fun getActiveForRoutine(routineId: UUID): Result<RoutineExecution?>

    /**
     * Inserts a new routine execution.
     */
    suspend fun insert(execution: RoutineExecution): Result<RoutineExecution>

    /**
     * Updates an existing routine execution.
     */
    suspend fun update(execution: RoutineExecution): Result<RoutineExecution>
}
