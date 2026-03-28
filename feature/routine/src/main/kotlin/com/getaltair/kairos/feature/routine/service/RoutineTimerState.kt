package com.getaltair.kairos.feature.routine.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Singleton shared state for the RoutineTimerService foreground notification actions.
 *
 * Actions emitted from the notification:
 * - [TimerAction.DONE]: User tapped the "Done" (complete step) action.
 * - [TimerAction.SKIP]: User tapped the "Skip" action.
 * - [TimerAction.PAUSE]: User tapped the "Pause" action.
 * - [TimerAction.RESUME]: User tapped the "Resume" action.
 *
 * Using a [Channel] ensures each action is consumed exactly once and not replayed
 * to late collectors.
 */
object RoutineTimerState {

    enum class TimerAction { DONE, SKIP, PAUSE, RESUME }

    private val _action = Channel<TimerAction>(Channel.BUFFERED)
    val action: Flow<TimerAction> = _action.receiveAsFlow()

    fun emitAction(action: TimerAction) {
        _action.trySend(action)
    }
}
