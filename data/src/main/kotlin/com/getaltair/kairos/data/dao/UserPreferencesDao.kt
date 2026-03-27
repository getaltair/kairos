package com.getaltair.kairos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.getaltair.kairos.data.entity.UserPreferencesEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [UserPreferencesEntity] operations.
 * Provides CRUD operations for user preferences (singleton pattern).
 */
@Dao
interface UserPreferencesDao {

    /**
     * Get the single user preferences record.
     * Returns the first (and only) record.
     */
    @Query("SELECT * FROM user_preferences LIMIT 1")
    fun get(): UserPreferencesEntity?

    /**
     * Get user preferences by ID.
     */
    @Query("SELECT * FROM user_preferences WHERE id = :id")
    fun getById(id: UUID): UserPreferencesEntity?

    /**
     * Get user preferences by user ID.
     */
    @Query("SELECT * FROM user_preferences WHERE user_id = :userId LIMIT 1")
    fun getByUserId(userId: String): UserPreferencesEntity?

    /**
     * Insert new user preferences.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(preferences: UserPreferencesEntity)

    /**
     * Insert or update user preferences.
     * If preferences with the same primary key exist, they are updated; otherwise, inserted.
     */
    @Upsert
    suspend fun upsert(entity: UserPreferencesEntity)

    /**
     * Update user preferences.
     */
    @Query(
        """
        UPDATE user_preferences SET
            user_id = :userId,
            notification_enabled = :notificationEnabled,
            default_reminder_time = :defaultReminderTime,
            theme = :theme,
            energy_tracking_enabled = :energyTrackingEnabled,
            notification_channels = :notificationChannels,
            quiet_hours_enabled = :quietHoursEnabled,
            quiet_hours_start = :quietHoursStart,
            quiet_hours_end = :quietHoursEnd,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    fun update(
        id: UUID,
        userId: String?,
        notificationEnabled: Boolean,
        defaultReminderTime: String,
        theme: String,
        energyTrackingEnabled: Boolean,
        notificationChannels: String?,
        quietHoursEnabled: Boolean,
        quietHoursStart: String,
        quietHoursEnd: String,
        updatedAt: Long
    )

    /**
     * Update quiet hours settings.
     */
    @Query(
        "UPDATE user_preferences SET quiet_hours_enabled = :enabled, quiet_hours_start = :start, quiet_hours_end = :end WHERE id = :id"
    )
    suspend fun updateQuietHours(id: String, enabled: Boolean, start: String, end: String)

    /**
     * Update notification enabled flag.
     */
    @Query(
        "UPDATE user_preferences SET notification_enabled = :notificationEnabled, updated_at = :updatedAt WHERE id = :id"
    )
    fun updateNotificationEnabled(id: UUID, notificationEnabled: Boolean, updatedAt: Long)

    /**
     * Update theme.
     */
    @Query("UPDATE user_preferences SET theme = :theme, updated_at = :updatedAt WHERE id = :id")
    fun updateTheme(id: UUID, theme: String, updatedAt: Long)

    /**
     * Delete user preferences.
     */
    @Query("DELETE FROM user_preferences WHERE id = :id")
    fun delete(id: UUID)

    /**
     * Delete all user preferences.
     */
    @Query("DELETE FROM user_preferences")
    fun deleteAll()

    /**
     * Get user preferences as Flow for reactive updates.
     */
    @Query("SELECT * FROM user_preferences LIMIT 1")
    fun getFlow(): Flow<UserPreferencesEntity?>
}
