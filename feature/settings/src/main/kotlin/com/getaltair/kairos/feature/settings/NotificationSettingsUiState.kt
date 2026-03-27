package com.getaltair.kairos.feature.settings

import java.time.LocalTime

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val isLoading: Boolean = true,
    val error: String? = null,
)
