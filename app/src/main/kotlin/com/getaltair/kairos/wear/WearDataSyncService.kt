package com.getaltair.kairos.wear

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearDataPaths
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.domain.wear.WearRoutineData
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Service that periodically syncs habit, completion, and routine data to the
 * Wear Data Layer so the watch can display current state.
 *
 * Started explicitly from [com.getaltair.kairos.KairosApp.startWearDataSync]. Uses
 * [Service.START_STICKY] so the system restarts it after process death.
 *
 * Repositories are suspend-based (not Flow-based), so this service polls at
 * a regular interval rather than collecting reactive streams.
 */
class WearDataSyncService : Service() {

    private val habitRepository: HabitRepository by inject()
    private val completionRepository: CompletionRepository by inject()
    private val routineExecutionRepository: RoutineExecutionRepository by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var syncJob: Job? = null

    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate() {
        super.onCreate()
        Timber.d("WearDataSyncService created")
        startPeriodicSync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure sync is running (covers restart-after-kill)
        if (syncJob?.isActive != true) {
            startPeriodicSync()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.d("WearDataSyncService destroyed")
        serviceJob.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Periodic sync loop
    // ------------------------------------------------------------------

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            // Initial sync immediately
            syncAll()

            // Then re-sync every 30 seconds
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                syncAll()
            }
        }
    }

    private suspend fun syncAll() {
        try {
            syncHabits()
        } catch (
            e: Exception
        ) {
            if (e is CancellationException) throw e
            Timber.e(e, "WearDataSyncService: failed to sync habits")
        }
        try {
            syncCompletions()
        } catch (
            e: Exception
        ) {
            if (e is CancellationException) throw e
            Timber.e(e, "WearDataSyncService: failed to sync completions")
        }
        try {
            syncActiveRoutine()
        } catch (
            e: Exception
        ) {
            if (e is CancellationException) throw e
            Timber.e(e, "WearDataSyncService: failed to sync routine")
        }
    }

    // ------------------------------------------------------------------
    // Habits
    // ------------------------------------------------------------------

    private suspend fun syncHabits() {
        when (val result = habitRepository.getActiveHabits()) {
            is Result.Success -> publishHabits(result.value.map { it.toWearData() })
            is Result.Error -> Timber.w("Failed to load active habits for wear sync: %s", result.message)
        }
    }

    private suspend fun publishHabits(habits: List<WearHabitData>) {
        val json = WearHabitData.listToJson(habits)
        val request = PutDataMapRequest.create(WearDataPaths.PATH_TODAY_HABITS).apply {
            dataMap.putString("data", json)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        try {
            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "WearDataSyncService: failed to publish habits")
        }
    }

    // ------------------------------------------------------------------
    // Completions
    // ------------------------------------------------------------------

    private suspend fun syncCompletions() {
        val today = LocalDate.now()
        when (val result = completionRepository.getForDate(today)) {
            is Result.Success -> publishCompletions(result.value.map { it.toWearData() })
            is Result.Error -> Timber.w("Failed to load completions for wear sync: %s", result.message)
        }
    }

    private suspend fun publishCompletions(completions: List<WearCompletionData>) {
        val json = WearCompletionData.listToJson(completions)
        val request = PutDataMapRequest.create(WearDataPaths.PATH_TODAY_COMPLETIONS).apply {
            dataMap.putString("data", json)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        try {
            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "WearDataSyncService: failed to publish completions")
        }
    }

    // ------------------------------------------------------------------
    // Active routine
    // ------------------------------------------------------------------

    /**
     * Syncs the active routine execution state. Since [RoutineExecutionRepository]
     * requires a specific routineId, and we do not have a "get any active execution"
     * method, we publish an empty/null data item to indicate no active routine.
     *
     * When a routine is actively running, the [WearDataSyncService] will be
     * triggered by the routine runner (which updates the execution), and the
     * next poll cycle will pick up the new state.
     *
     * For now, we clear the active routine data item so the watch knows there
     * is nothing running. A future enhancement can add a repository method to
     * query the most recent active execution across all routines.
     */
    private suspend fun syncActiveRoutine() {
        try {
            dataClient.deleteDataItems(
                android.net.Uri.Builder()
                    .scheme("wear")
                    .path(WearDataPaths.PATH_ROUTINE_ACTIVE)
                    .build()
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Failed to clear active routine data item")
        }
    }

    // ------------------------------------------------------------------
    // Domain -> WearData mapping helpers
    // ------------------------------------------------------------------

    private fun Habit.toWearData() = WearHabitData(
        id = id.toString(),
        name = name,
        anchorBehavior = anchorBehavior,
        category = category.displayName.uppercase(),
        estimatedSeconds = estimatedSeconds,
        icon = icon,
        color = color,
    )

    private fun Completion.toWearData() = WearCompletionData(
        id = id.toString(),
        habitId = habitId.toString(),
        date = date.toString(),
        type = type.displayName,
        partialPercent = partialPercent,
    )

    companion object {
        /** How often to re-sync data to the watch, in milliseconds. */
        private const val SYNC_INTERVAL_MS = 30_000L
    }
}
