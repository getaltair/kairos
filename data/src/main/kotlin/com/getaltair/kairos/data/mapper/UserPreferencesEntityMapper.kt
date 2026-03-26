package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.converter.JsonMapConverter
import com.getaltair.kairos.data.entity.UserPreferencesEntity
import com.getaltair.kairos.domain.entity.UserPreferences
import java.time.Instant

/**
 * Bidirectional mapper between [UserPreferencesEntity] and [UserPreferences].
 */
object UserPreferencesEntityMapper {

    private val jsonMapConverter = JsonMapConverter()

    /**
     * Converts [UserPreferencesEntity] to domain [UserPreferences].
     */
    fun toDomain(entity: UserPreferencesEntity): UserPreferences {
        val notificationChannels = entity.notificationChannels?.let {
            try {
                jsonMapConverter.stringToMap(it)
            } catch (e: Exception) {
                emptyMap()
            }
        }

        return UserPreferences(
            id = entity.id,
            userId = entity.userId,
            notificationEnabled = entity.notificationEnabled,
            defaultReminderTime = entity.defaultReminderTime,
            theme = entity.theme,
            energyTrackingEnabled = entity.energyTrackingEnabled,
            notificationChannels = notificationChannels,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    /**
     * Converts domain [UserPreferences] to [UserPreferencesEntity].
     */
    fun toEntity(domain: UserPreferences): UserPreferencesEntity {
        val notificationChannels = domain.notificationChannels?.takeIf { it.isNotEmpty() }?.let {
            try {
                jsonMapConverter.mapToString(it)
            } catch (e: Exception) {
                null
            }
        }

        return UserPreferencesEntity(
            id = domain.id,
            userId = domain.userId,
            notificationEnabled = domain.notificationEnabled,
            defaultReminderTime = domain.defaultReminderTime,
            theme = domain.theme,
            energyTrackingEnabled = domain.energyTrackingEnabled,
            notificationChannels = notificationChannels,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }
}
