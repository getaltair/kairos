package com.getaltair.kairos.domain.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Provides observable sync state for UI consumption.
 * Implemented by the sync module's SyncManager.
 */
interface SyncStateProvider {
    val syncState: StateFlow<SyncState>
}
