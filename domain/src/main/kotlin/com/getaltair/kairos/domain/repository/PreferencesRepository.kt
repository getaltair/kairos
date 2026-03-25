package com.getaltair.kairos.domain.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.UserPreferences

/**
 * Repository interface for UserPreferences operations.
 * Implemented in data layer with Room database.
 * There should only be one UserPreferences document per user.
 */
interface PreferencesRepository {
    /**
     * Gets the user's preferences.
     * Creates default preferences if none exist.
     */
    suspend fun get(): Result<UserPreferences>

    /**
     * Updates the user's preferences.
     * Use copy() to create updated instance.
     */
    suspend fun update(preferences: UserPreferences): Result<UserPreferences>
}
