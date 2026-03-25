package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getaltair.kairos.data.entity.RecoverySessionEntity
import com.getaltair.kairos.domain.enums.SessionStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [RecoverySessionEntity] operations.
 * Provides CRUD and specialized queries for recovery session data.
 */
@Dao
interface RecoverySessionDao {

    /**
     * Get all recovery sessions.
     */
    @Query("SELECT * FROM recovery_sessions ORDER BY triggered_at DESC")
    fun getAll(): List<RecoverySessionEntity>

    /**
     * Get a recovery session by ID.
     */
    @Query("SELECT * FROM recovery_sessions WHERE id = :id")
    fun getById(id: UUID): RecoverySessionEntity?

    /**
     * Get pending recovery sessions for a specific habit.
     */
    @Query(
        """
        SELECT * FROM recovery_sessions
        WHERE habit_id = :habitId AND status = 'Pending'
        ORDER BY triggered_at DESC
    """
    )
    fun getPendingForHabit(habitId: UUID): List<RecoverySessionEntity>

    /**
     * Get all recovery sessions for a habit.
     */
    @Query(
        """
        SELECT * FROM recovery_sessions
        WHERE habit_id = :habitId
        ORDER BY triggered_at DESC
    """
    )
    fun getByHabit(habitId: UUID): List<RecoverySessionEntity>

    /**
     * Get all pending recovery sessions.
     */
    @Query("SELECT * FROM recovery_sessions WHERE status = 'Pending' ORDER BY triggered_at DESC")
    fun getPendingSessions(): List<RecoverySessionEntity>

    /**
     * Get completed recovery sessions.
     */
    @Query("SELECT * FROM recovery_sessions WHERE status = 'Completed' ORDER BY completed_at DESC")
    fun getCompletedSessions(): List<RecoverySessionEntity>

    /**
     * Get recovery sessions by type.
     */
    @Query("SELECT * FROM recovery_sessions WHERE type = :type ORDER BY triggered_at DESC")
    fun getByType(type: String): List<RecoverySessionEntity>

    /**
     * Get recovery sessions by status.
     */
    @Query("SELECT * FROM recovery_sessions WHERE status = :status ORDER BY triggered_at DESC")
    fun getByStatus(status: SessionStatus): List<RecoverySessionEntity>

    /**
     * Get sessions within a date range.
     */
    @Query(
        """
        SELECT * FROM recovery_sessions
        WHERE triggered_at BETWEEN :startDate AND :endDate
        ORDER BY triggered_at DESC
    """
    )
    fun getForDateRange(startDate: Long, endDate: Long): List<RecoverySessionEntity>

    /**
     * Insert a new recovery session.
     */
    @Insert
    fun insert(session: RecoverySessionEntity)

    /**
     * Insert multiple recovery sessions.
     */
    @Insert
    fun insertAll(sessions: List<RecoverySessionEntity>)

    /**
     * Update a recovery session.
     */
    @Query(
        """
        UPDATE recovery_sessions SET
            status = :status,
            completed_at = :completedAt,
            action = :action,
            notes = :notes,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(id: UUID, status: SessionStatus, completedAt: Long?, action: String?, notes: String?, updatedAt: Long)

    /**
     * Delete a recovery session.
     */
    @Query("DELETE FROM recovery_sessions WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all recovery sessions for a habit.
     */
    @Query("DELETE FROM recovery_sessions WHERE habit_id = :habitId")
    fun deleteByHabitId(habitId: UUID)

    /**
     * Delete all recovery sessions.
     */
    @Query("DELETE FROM recovery_sessions")
    fun deleteAll()

    /**
     * Get recovery sessions as Flow for reactive updates.
     */
    @Query("SELECT * FROM recovery_sessions ORDER BY triggered_at DESC")
    fun getAllFlow(): Flow<List<RecoverySessionEntity>>
}
