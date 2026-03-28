package com.getaltair.kairos.wear.data

import com.getaltair.kairos.domain.wear.WearAction
import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.domain.wear.WearRoutineData
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class WearDataRepository(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val capabilityClient: CapabilityClient,
    private val localCache: LocalCache,
    private val actionQueue: ActionQueue,
) {
    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected.asStateFlow()

    val todayHabits: Flow<List<WearHabitData>> = localCache.habits
    val todayCompletions: Flow<List<WearCompletionData>> = localCache.completions
    val activeRoutine: Flow<WearRoutineData?> = localCache.activeRoutine

    fun updatePhoneConnected(connected: Boolean) {
        _isPhoneConnected.value = connected
    }

    suspend fun completeHabit(habitId: String, type: String, partialPercent: Int? = null) {
        sendAction(WearAction.CompleteHabit(habitId, type, partialPercent))
    }

    suspend fun skipHabit(habitId: String, reason: String? = null) {
        sendAction(WearAction.SkipHabit(habitId, reason))
    }

    suspend fun startRoutine(routineId: String) {
        sendAction(WearAction.StartRoutine(routineId))
    }

    suspend fun advanceRoutineStep(executionId: String) {
        sendAction(WearAction.AdvanceRoutineStep(executionId))
    }

    suspend fun pauseRoutine(executionId: String) {
        sendAction(WearAction.PauseRoutine(executionId))
    }

    suspend fun flushQueue() {
        if (actionQueue.isEmpty()) return
        val pending = actionQueue.dequeueAll()
        val nodeId = getPhoneNodeId() ?: return
        for (action in pending) {
            sendMessage(nodeId, action)
        }
        Timber.d("Flushed ${pending.size} queued actions to phone")
    }

    private suspend fun sendAction(action: WearAction) {
        if (_isPhoneConnected.value) {
            val nodeId = getPhoneNodeId()
            if (nodeId != null) {
                sendMessage(nodeId, action)
                return
            }
        }
        actionQueue.enqueue(action)
        Timber.d("Phone disconnected, queued action: ${action::class.simpleName}")
    }

    private suspend fun sendMessage(nodeId: String, action: WearAction) {
        val path = when (action) {
            is WearAction.CompleteHabit -> WearDataPaths.MESSAGE_HABIT_COMPLETED
            is WearAction.SkipHabit -> WearDataPaths.MESSAGE_HABIT_SKIPPED
            is WearAction.StartRoutine -> WearDataPaths.MESSAGE_ROUTINE_STARTED
            is WearAction.AdvanceRoutineStep -> WearDataPaths.MESSAGE_ROUTINE_STEP_DONE
            is WearAction.PauseRoutine -> WearDataPaths.MESSAGE_ROUTINE_PAUSED
        }
        try {
            messageClient.sendMessage(
                nodeId,
                path,
                action.toJson().toByteArray(Charsets.UTF_8),
            ).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message to phone, queuing action")
            actionQueue.enqueue(action)
        }
    }

    private suspend fun getPhoneNodeId(): String? = try {
        val capability = capabilityClient
            .getCapability(WearDataPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
            .await()
        capability.nodes.firstOrNull()?.id
    } catch (e: Exception) {
        Timber.e(e, "Failed to get phone node ID")
        null
    }
}
