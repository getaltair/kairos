package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RecoverySession
import java.util.UUID

/**
 * Repository interface for RecoverySession entity operations.
 * Implemented in data layer with Room database.
 */
interface RecoveryRepository {
    /**
     * Gets pending recovery sessions for a specific habit.
     * Pending sessions are those with status = Pending.
     */
    suspend fun getPendingForHabit(habitId: UUID): Result<List<RecoverySession>>

    /**
     * Gets all recovery sessions for a habit.
     */
    suspend fun getAllForHabit(habitId: UUID): Result<List<RecoverySession>>

    /**
     * Gets a recovery session by its ID.
     */
    suspend fun getById(id: UUID): Result<RecoverySession?>

    /**
     * Inserts a new recovery session.
     */
    suspend fun insert(session: RecoverySession): Result<RecoverySession>

    /**
     * Updates an existing recovery session.
     * Use copy() to create updated instance.
     */
    suspend fun update(session: RecoverySession): Result<RecoverySession>

    /**
     * Deletes a recovery session.
     */
    suspend fun delete(id: UUID): Result<Unit>
}
