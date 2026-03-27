package com.getaltair.kairos.domain.sync

/**
 * Represents the current state of Firestore synchronization.
 */
sealed class SyncState {
    /** Local database is fully synchronized with Firestore. */
    data object Synced : SyncState()

    /** Synchronization is currently in progress. */
    data object Syncing : SyncState()

    /**
     * Device is offline; Firestore's built-in offline persistence will queue writes.
     * The sync state will return to Synced when the next successful snapshot is received.
     */
    data object Offline : SyncState()

    /** Synchronization encountered an error. */
    data class Error(val message: String, val cause: Throwable? = null) : SyncState()

    /** No user is signed in; sync is inactive. */
    data object NotSignedIn : SyncState()
}
