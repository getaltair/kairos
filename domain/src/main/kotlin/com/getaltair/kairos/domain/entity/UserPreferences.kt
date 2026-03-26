package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.Theme
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Global user settings and preferences.
 * A singleton document containing all user-wide configuration.
 *
 * @property id Unique identifier for preferences document
 * @property userId ID of the user (for sync purposes)
 * @property notificationEnabled Whether notifications are enabled
 * @property defaultReminderTime Default time for reminders
 * @property theme Current theme preference
 * @property energyTrackingEnabled Whether energy level tracking is enabled
 * @property notificationChannels Per-channel notification settings (JSON)
 * @property createdAt When preferences were created
 * @property updatedAt When preferences were last updated
 */
data class UserPreferences(
    val id: UUID = UUID.randomUUID(),
    val userId: String? = null, // Will be populated by sync layer
    val notificationEnabled: Boolean = true,
    val defaultReminderTime: LocalTime = LocalTime.of(9, 0),
    val theme: Theme = Theme.System,
    val energyTrackingEnabled: Boolean = false,
    val notificationChannels: Map<String, Any>? = null, // JSON representation
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Creates a copy of this preferences with the specified changes.
     */
    fun copy(
        notificationEnabled: Boolean = this.notificationEnabled,
        defaultReminderTime: LocalTime = this.defaultReminderTime,
        theme: Theme = this.theme,
        energyTrackingEnabled: Boolean = this.energyTrackingEnabled,
        notificationChannels: Map<String, Any>? = this.notificationChannels,
        updatedAt: Instant = Instant.now()
    ): UserPreferences = UserPreferences(
        id = this.id,
        userId = this.userId,
        notificationEnabled = notificationEnabled,
        defaultReminderTime = defaultReminderTime,
        theme = theme,
        energyTrackingEnabled = energyTrackingEnabled,
        notificationChannels = notificationChannels,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )
}
