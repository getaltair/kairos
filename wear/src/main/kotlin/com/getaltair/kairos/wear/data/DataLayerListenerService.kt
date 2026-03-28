package com.getaltair.kairos.wear.data

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Listens for Data Layer changes from the phone and updates local cache.
 * Also monitors phone connectivity to flush the action queue when reconnected.
 */
class DataLayerListenerService : WearableListenerService() {
    private val localCache: LocalCache by inject()
    private val repository: WearDataRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: return@forEach
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val json = dataMap.getString("data") ?: return@forEach
            scope.launch {
                try {
                    when (path) {
                        WearDataPaths.PATH_TODAY_HABITS -> localCache.updateHabits(json)
                        WearDataPaths.PATH_TODAY_COMPLETIONS -> localCache.updateCompletions(json)
                        WearDataPaths.PATH_ROUTINE_ACTIVE -> localCache.updateRoutineState(json)
                        else -> Timber.d("Unknown data path: $path")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating local cache for path: $path")
                }
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val connected = capabilityInfo.nodes.isNotEmpty()
        repository.updatePhoneConnected(connected)
        if (connected) {
            scope.launch {
                repository.flushQueue()
            }
        }
        Timber.d("Phone connectivity changed: $connected")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
