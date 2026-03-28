package com.getaltair.kairos.wear

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.enums.StepResult
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import com.getaltair.kairos.domain.repository.RoutineRepository
import com.getaltair.kairos.domain.usecase.AbandonRoutineUseCase
import com.getaltair.kairos.domain.usecase.AdvanceRoutineStepUseCase
import com.getaltair.kairos.domain.usecase.StartRoutineUseCase
import com.getaltair.kairos.domain.wear.WearAction
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Receives messages sent from the WearOS companion app via the Data Layer
 * message API and dispatches them to the appropriate domain use cases.
 *
 * Registered in AndroidManifest with an intent-filter for
 * `com.google.android.gms.wearable.MESSAGE_RECEIVED` so the system
 * delivers messages even when the app process is not running.
 *
 * Each message path maps to a [WearAction] subclass that is deserialized
 * from the UTF-8 JSON payload.
 */
class WearMessageListenerService : WearableListenerService() {

    // Use cases -- injected via Koin
    private val completeHabitUseCase: com.getaltair.kairos.core.usecase.CompleteHabitUseCase by inject()
    private val skipHabitUseCase: com.getaltair.kairos.core.usecase.SkipHabitUseCase by inject()
    private val startRoutineUseCase: StartRoutineUseCase by inject()
    private val advanceRoutineStepUseCase: AdvanceRoutineStepUseCase by inject()
    private val abandonRoutineUseCase: AbandonRoutineUseCase by inject()

    // Repositories -- needed for routine step lookup
    private val routineExecutionRepository: RoutineExecutionRepository by inject()
    private val routineRepository: RoutineRepository by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data.toString(Charsets.UTF_8)

        Timber.d("WearMessage received: path=%s payloadLength=%d", path, payload.length)

        if (payload.isBlank()) {
            Timber.w("Empty payload for message path=%s", path)
            return
        }

        val action = WearAction.fromJson(payload)
        if (action == null) {
            Timber.w("Failed to parse WearAction from payload on path=%s", path)
            return
        }

        when (path) {
            WearDataPaths.MESSAGE_HABIT_COMPLETED -> handleHabitCompleted(action)
            WearDataPaths.MESSAGE_HABIT_SKIPPED -> handleHabitSkipped(action)
            WearDataPaths.MESSAGE_ROUTINE_STARTED -> handleRoutineStarted(action)
            WearDataPaths.MESSAGE_ROUTINE_STEP_DONE -> handleRoutineStepDone(action)
            WearDataPaths.MESSAGE_ROUTINE_PAUSED -> handleRoutinePaused(action)
            else -> Timber.w("Unknown message path: %s", path)
        }
    }

    // ------------------------------------------------------------------
    // Habit actions
    // ------------------------------------------------------------------

    private fun handleHabitCompleted(action: WearAction) {
        val data = action as? WearAction.CompleteHabit ?: run {
            Timber.w("Expected CompleteHabit action but got %s", action::class.simpleName)
            return
        }

        serviceScope.launch {
            try {
                val habitId = UUID.fromString(data.habitId)
                val completionType = parseCompletionType(data.type)
                val result = completeHabitUseCase(habitId, completionType, data.partialPercent)
                when (result) {
                    is Result.Success -> Timber.d("Wear: habit %s completed", data.habitId)
                    is Result.Error -> Timber.w("Wear: failed to complete habit %s: %s", data.habitId, result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Wear: error handling habit completion for %s", data.habitId)
            }
        }
    }

    private fun handleHabitSkipped(action: WearAction) {
        val data = action as? WearAction.SkipHabit ?: run {
            Timber.w("Expected SkipHabit action but got %s", action::class.simpleName)
            return
        }

        serviceScope.launch {
            try {
                val habitId = UUID.fromString(data.habitId)
                val skipReason = data.reason?.let { parseSkipReason(it) }
                val result = skipHabitUseCase(habitId, skipReason)
                when (result) {
                    is Result.Success -> Timber.d("Wear: habit %s skipped", data.habitId)
                    is Result.Error -> Timber.w("Wear: failed to skip habit %s: %s", data.habitId, result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Wear: error handling habit skip for %s", data.habitId)
            }
        }
    }

    // ------------------------------------------------------------------
    // Routine actions
    // ------------------------------------------------------------------

    private fun handleRoutineStarted(action: WearAction) {
        val data = action as? WearAction.StartRoutine ?: run {
            Timber.w("Expected StartRoutine action but got %s", action::class.simpleName)
            return
        }

        serviceScope.launch {
            try {
                val routineId = UUID.fromString(data.routineId)
                val result = startRoutineUseCase(routineId)
                when (result) {
                    is Result.Success -> Timber.d("Wear: routine %s started", data.routineId)
                    is Result.Error -> Timber.w("Wear: failed to start routine %s: %s", data.routineId, result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Wear: error handling routine start for %s", data.routineId)
            }
        }
    }

    private fun handleRoutineStepDone(action: WearAction) {
        val data = action as? WearAction.AdvanceRoutineStep ?: run {
            Timber.w("Expected AdvanceRoutineStep action but got %s", action::class.simpleName)
            return
        }

        serviceScope.launch {
            try {
                val executionId = UUID.fromString(data.executionId)

                // Look up the execution to find the current step's habitId
                val execResult = routineExecutionRepository.getById(executionId)
                if (execResult is Result.Error) {
                    Timber.w("Wear: execution not found %s: %s", data.executionId, execResult.message)
                    return@launch
                }
                val execution = (execResult as Result.Success).value
                if (execution == null) {
                    Timber.w("Wear: execution %s is null", data.executionId)
                    return@launch
                }

                // Look up the routine's habits to find the habitId at the current step
                val routineWithHabitsResult = routineRepository.getRoutineWithHabits(execution.routineId)
                if (routineWithHabitsResult is Result.Error) {
                    Timber.w(
                        "Wear: failed to load routine habits for %s: %s",
                        execution.routineId,
                        routineWithHabitsResult.message,
                    )
                    return@launch
                }
                val routineWithHabits = (routineWithHabitsResult as Result.Success).value
                if (routineWithHabits == null) {
                    Timber.w("Wear: routine %s not found", execution.routineId)
                    return@launch
                }

                val sortedHabits = routineWithHabits.habits.sortedBy { it.orderIndex }
                val currentHabit = sortedHabits.getOrNull(execution.currentStepIndex)
                if (currentHabit == null) {
                    Timber.w(
                        "Wear: no habit at step index %d for routine %s",
                        execution.currentStepIndex,
                        execution.routineId,
                    )
                    return@launch
                }

                val result = advanceRoutineStepUseCase(
                    executionId = executionId,
                    stepResult = StepResult.Completed,
                    habitId = currentHabit.habitId,
                )
                when (result) {
                    is Result.Success -> Timber.d("Wear: routine step advanced for execution %s", data.executionId)

                    is Result.Error -> Timber.w(
                        "Wear: failed to advance routine step %s: %s",
                        data.executionId,
                        result.message,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Wear: error handling routine step done for %s", data.executionId)
            }
        }
    }

    private fun handleRoutinePaused(action: WearAction) {
        val data = action as? WearAction.PauseRoutine ?: run {
            Timber.w("Expected PauseRoutine action but got %s", action::class.simpleName)
            return
        }

        serviceScope.launch {
            try {
                val executionId = UUID.fromString(data.executionId)
                val result = abandonRoutineUseCase(executionId)
                when (result) {
                    is Result.Success -> Timber.d("Wear: routine %s abandoned", data.executionId)

                    is Result.Error -> Timber.w(
                        "Wear: failed to abandon routine %s: %s",
                        data.executionId,
                        result.message,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Wear: error handling routine pause/abandon for %s", data.executionId)
            }
        }
    }

    // ------------------------------------------------------------------
    // Enum parsing helpers
    // ------------------------------------------------------------------

    private fun parseCompletionType(type: String): CompletionType = when (type.uppercase()) {
        "FULL", "DONE" -> CompletionType.Full
        "PARTIAL" -> CompletionType.Partial
        "SKIPPED" -> CompletionType.Skipped
        else -> CompletionType.Full
    }

    private fun parseSkipReason(reason: String): SkipReason? = when (reason.lowercase()) {
        "too tired", "too_tired" -> SkipReason.TooTired
        "no time", "no_time" -> SkipReason.NoTime
        "not feeling well", "not_feeling_well" -> SkipReason.NotFeelingWell
        "traveling" -> SkipReason.Traveling
        "took day off", "took_day_off" -> SkipReason.TookDayOff
        "other" -> SkipReason.Other
        else -> null
    }
}
