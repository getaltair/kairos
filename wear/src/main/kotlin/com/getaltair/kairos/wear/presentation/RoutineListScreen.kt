package com.getaltair.kairos.wear.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen showing the currently active routine on the watch.
 * If no routine is active, a placeholder message is shown.
 * Tapping an active routine navigates to the routine runner.
 */
@Composable
fun RoutineListScreen(onRoutineClick: (String) -> Unit, viewModel: RoutineListViewModel = koinViewModel(),) {
    val activeRoutine by viewModel.activeRoutine.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        item {
            ListHeader { Text("Routines") }
        }
        activeRoutine?.let { routine ->
            item {
                FilledTonalButton(
                    onClick = { onRoutineClick(routine.routineId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${routine.name} - Step ${routine.currentStepIndex + 1}/${routine.steps.size}")
                }
            }
        } ?: run {
            item { Text("No active routines") }
        }
    }
}
