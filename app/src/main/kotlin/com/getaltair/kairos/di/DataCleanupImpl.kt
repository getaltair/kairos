package com.getaltair.kairos.di

import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.domain.sync.DataCleanup
import com.getaltair.kairos.sync.SyncManager

/**
 * Bridges the domain-layer [DataCleanup] interface with concrete infrastructure.
 *
 * Delegates sync operations to [SyncManager] and local storage operations
 * to [KairosDatabase]. Lives in the app module because it needs access to
 * both the sync and data modules, which the domain layer cannot depend on.
 */
class DataCleanupImpl(private val syncManager: SyncManager, private val database: KairosDatabase) : DataCleanup {

    override fun stopSyncListeners() {
        syncManager.stopListening()
    }

    override suspend fun deleteCloudData(userId: String) {
        syncManager.deleteAllUserData(userId)
    }

    override fun clearLocalData() {
        database.clearAllTables()
    }
}
