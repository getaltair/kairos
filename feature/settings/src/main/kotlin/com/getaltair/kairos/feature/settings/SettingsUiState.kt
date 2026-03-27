package com.getaltair.kairos.feature.settings

import com.getaltair.kairos.sync.SyncState
import java.time.Instant

data class SettingsUiState(
    val syncState: SyncState = SyncState.NotSignedIn,
    val userEmail: String? = null,
    val isSignedIn: Boolean = false,
    val lastSyncTime: Instant? = null,
    val showDeleteAccountDialog: Boolean = false,
    val showSignOutDialog: Boolean = false,
)
