package com.getaltair.kairos.sync

/**
 * Represents the current state of Firestore synchronization.
 */
sealed class SyncState {
    /** Local database is fully synchronized with Firestore. */
    data object Synced : SyncState()

    /** Synchronization is currently in progress. */
    data object Syncing : SyncState()

    /** Device is offline; sync will resume when connectivity returns. */
    data object Offline : SyncState()

    /** Synchronization encountered an error. */
    data class Error(val message: String) : SyncState()

    /** No user is signed in; sync is inactive. */
    data object NotSignedIn : SyncState()
}
