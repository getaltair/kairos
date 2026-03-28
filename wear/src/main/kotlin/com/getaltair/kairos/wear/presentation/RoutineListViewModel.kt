package com.getaltair.kairos.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.wear.WearRoutineData
import com.getaltair.kairos.wear.data.WearDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the routine list screen on the watch.
 * Exposes the currently active routine (if any) from the data layer.
 */
class RoutineListViewModel(private val repository: WearDataRepository) : ViewModel() {

    val activeRoutine: StateFlow<WearRoutineData?> = repository.activeRoutine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
