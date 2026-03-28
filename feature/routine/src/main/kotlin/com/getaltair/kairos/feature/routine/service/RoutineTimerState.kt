package com.getaltair.kairos.feature.routine.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton communication bridge between [RoutineTimerService] and the ViewModel.
 *
 * The service emits [TimerAction] values when the user interacts with
 * notification action buttons (Done, Skip, Pause). The ViewModel observes
 * these actions and handles them accordingly.
 *
 * After consuming an action, the ViewModel should call [clearAction] to reset.
 */
object RoutineTimerState {

    enum class TimerAction { DONE, SKIP, PAUSE, RESUME }

    private val _action = MutableStateFlow<TimerAction?>(null)
    val action: StateFlow<TimerAction?> = _action.asStateFlow()

    fun emitAction(action: TimerAction) {
        _action.value = action
    }

    fun clearAction() {
        _action.value = null
    }
}
