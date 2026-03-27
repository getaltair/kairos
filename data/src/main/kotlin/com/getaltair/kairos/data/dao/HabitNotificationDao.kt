package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getaltair.kairos.data.entity.HabitNotificationEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [HabitNotificationEntity] operations.
 * Provides CRUD and specialized queries for habit notification data.
 */
@Dao
interface HabitNotificationDao {

    /**
     * Get all habit notifications.
     */
    @Query("SELECT * FROM habit_notifications ORDER BY created_at DESC")
    fun getAll(): List<HabitNotificationEntity>

    /**
     * Get a habit notification by ID.
     */
    @Query("SELECT * FROM habit_notifications WHERE id = :id")
    fun getById(id: UUID): HabitNotificationEntity?

    /**
     * Get notification for a specific habit.
     */
    @Query("SELECT * FROM habit_notifications WHERE habit_id = :habitId LIMIT 1")
    suspend fun getForHabit(habitId: UUID): HabitNotificationEntity?

    /**
     * Get enabled notifications.
     */
    @Query("SELECT * FROM habit_notifications WHERE is_enabled = 1 ORDER BY created_at DESC")
    suspend fun getEnabled(): List<HabitNotificationEntity>

    /**
     * Get enabled notifications that have persistent reminders turned on.
     */
    @Query("SELECT * FROM habit_notifications WHERE is_enabled = 1 AND is_persistent = 1")
    suspend fun getEnabledPersistent(): List<HabitNotificationEntity>

    /**
     * Insert a new habit notification.
     */
    @Insert
    fun insert(notification: HabitNotificationEntity)

    /**
     * Insert multiple habit notifications.
     */
    @Insert
    fun insertAll(notifications: List<HabitNotificationEntity>)

    /**
     * Update a habit notification.
     */
    @Query(
        """
        UPDATE habit_notifications SET
            time = :time,
            is_enabled = :isEnabled,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(id: UUID, time: String, isEnabled: Boolean, updatedAt: Long)

    /**
     * Enable or disable a notification.
     */
    @Query("UPDATE habit_notifications SET is_enabled = :isEnabled, updated_at = :updatedAt WHERE id = :id")
    fun setEnabled(id: UUID, isEnabled: Boolean, updatedAt: Long)

    /**
     * Delete a habit notification.
     */
    @Query("DELETE FROM habit_notifications WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all notifications for a habit.
     */
    @Query("DELETE FROM habit_notifications WHERE habit_id = :habitId")
    fun deleteForHabit(habitId: UUID)

    /**
     * Delete all habit notifications.
     */
    @Query("DELETE FROM habit_notifications")
    fun deleteAll()

    /**
     * Get habit notifications as Flow for reactive updates.
     */
    @Query("SELECT * FROM habit_notifications ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<HabitNotificationEntity>>
}
