package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.converter.LocalTimeConverter
import com.getaltair.kairos.data.converter.ThemeConverter
import com.getaltair.kairos.data.dao.UserPreferencesDao
import com.getaltair.kairos.data.mapper.UserPreferencesEntityMapper
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.UserPreferences
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncEntityTypes
import com.getaltair.kairos.domain.sync.SyncTrigger
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Room-backed implementation of [com.getaltair.kairos.domain.repository.PreferencesRepository].
 * Delegates persistence to [UserPreferencesDao] and maps between entity and domain layers
 * using [UserPreferencesEntityMapper].
 *
 * Ensures a single preferences record always exists by creating defaults on first access.
 */
class PreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao,
    private val syncTrigger: SyncTrigger,
    private val authRepository: AuthRepository,
    private val syncScope: CoroutineScope,
) : com.getaltair.kairos.domain.repository.PreferencesRepository {

    private val localTimeConverter = LocalTimeConverter()
    private val themeConverter = ThemeConverter()

    override suspend fun get(): Result<UserPreferences> = try {
        val entity = userPreferencesDao.get()
        if (entity != null) {
            Result.Success(UserPreferencesEntityMapper.toDomain(entity))
        } else {
            val defaultPreferences = UserPreferences()
            val defaultEntity = UserPreferencesEntityMapper.toEntity(defaultPreferences)
            userPreferencesDao.insert(defaultEntity)
            triggerSync(SyncEntityTypes.USER_PREFERENCE, defaultPreferences.id.toString(), defaultPreferences)
            Result.Success(defaultPreferences)
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get user preferences")
        Result.Error("Failed to get user preferences: ${e.message}", cause = e)
    }

    override suspend fun update(preferences: UserPreferences): Result<UserPreferences> = try {
        val defaultReminderTime = localTimeConverter.localTimeToString(preferences.defaultReminderTime)
            ?: throw IllegalStateException("Failed to convert: defaultReminderTime")
        val theme = themeConverter.themeToString(preferences.theme)
            ?: throw IllegalStateException("Failed to convert: theme")
        val entity = UserPreferencesEntityMapper.toEntity(preferences)

        userPreferencesDao.update(
            id = preferences.id,
            userId = preferences.userId,
            notificationEnabled = preferences.notificationEnabled,
            defaultReminderTime = defaultReminderTime,
            theme = theme,
            energyTrackingEnabled = preferences.energyTrackingEnabled,
            notificationChannels = entity.notificationChannels,
            updatedAt = Instant.now().toEpochMilli()
        )
        triggerSync(SyncEntityTypes.USER_PREFERENCE, preferences.id.toString(), preferences)
        Result.Success(preferences)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to update user preferences id=%s", preferences.id)
        Result.Error("Failed to update user preferences: ${e.message}", cause = e)
    }

    /**
     * Fire-and-forget sync push. Runs in a non-blocking scope so that the
     * local Room operation is never delayed by Firestore.
     */
    private fun triggerSync(entityType: String, id: String, entity: Any) {
        val userId = authRepository.getCurrentUserId() ?: run {
            Timber.d("Skipping sync push: user not signed in")
            return
        }
        syncScope.launch(Dispatchers.IO) {
            try {
                syncTrigger.triggerPush(userId, entityType, id, entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to push preferences sync change id=%s", id)
            }
        }
    }
}
