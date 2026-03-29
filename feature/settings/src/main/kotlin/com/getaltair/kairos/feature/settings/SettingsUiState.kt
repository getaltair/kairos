package com.getaltair.kairos.feature.settings

import com.getaltair.kairos.domain.sync.SyncState
import java.time.Instant

/**
 * Models the current phase of the account deletion flow.
 */
sealed interface DeletionState {
    /** No deletion flow is active. */
    data object Idle : DeletionState

    /** User has been asked to confirm they want to delete their account. */
    data object ConfirmDialog : DeletionState

    /** User is entering their password for re-authentication. */
    data object ReauthDialog : DeletionState

    /** Deletion is in progress (network calls active). */
    data object Deleting : DeletionState

    /** Account was successfully deleted. */
    data object Deleted : DeletionState

    /** Deletion failed with a user-facing message. */
    data class Failed(val message: String) : DeletionState
}

data class SettingsUiState(
    val syncState: SyncState = SyncState.NotSignedIn,
    val userEmail: String? = null,
    val isSignedIn: Boolean = false,
    val lastSyncTime: Instant? = null,
    val showSignOutDialog: Boolean = false,
    val deletionState: DeletionState = DeletionState.Idle,
    val errorMessage: String? = null,
)
