package com.getaltair.kairos.wear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Text
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Detail screen for a single habit on the watch.
 * Shows the habit name and anchor behavior, with action buttons
 * to mark as done, partially complete, or skip.
 */
@Composable
fun HabitDetailScreen(
    habitId: String,
    onActionComplete: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel(parameters = { parametersOf(habitId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showPartialSelector by remember { mutableStateOf(false) }

    if (showPartialSelector) {
        PartialPercentSelector(
            onSelect = { percent ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.completeHabit("PARTIAL", percent)
                onActionComplete()
            },
            onDismiss = { showPartialSelector = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        uiState.habit?.let { habit ->
            Text(text = habit.name)
            Text(
                text = habit.anchorBehavior.lowercase().replaceFirstChar { it.uppercase() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.completeHabit("FULL")
                        onActionComplete()
                    },
                ) { Text("Done") }
                FilledTonalButton(
                    onClick = { showPartialSelector = true },
                ) { Text("Part") }
                FilledTonalButton(
                    onClick = {
                        viewModel.skipHabit()
                        onActionComplete()
                    },
                ) { Text("Skip") }
            }
        } ?: Text("Loading...")
    }
}

/**
 * Sub-screen for selecting a partial completion percentage.
 * Shows 25%, 50%, 75% options plus a cancel button.
 */
@Composable
private fun PartialPercentSelector(onSelect: (Int) -> Unit, onDismiss: () -> Unit,) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Text("How much?")
        listOf(25, 50, 75).forEach { percent ->
            FilledTonalButton(
                onClick = { onSelect(percent) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("$percent%")
            }
        }
        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }
    }
}
