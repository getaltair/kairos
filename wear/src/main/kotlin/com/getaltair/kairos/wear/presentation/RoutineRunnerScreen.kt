package com.getaltair.kairos.wear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Text
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Routine runner screen for the watch.
 * Shows the current step with a countdown timer, progress indicator,
 * and controls to mark done, skip, or pause the routine.
 */
@Composable
fun RoutineRunnerScreen(
    routineId: String,
    onFinished: () -> Unit,
    viewModel: RoutineRunnerViewModel = koinViewModel(parameters = { parametersOf(routineId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Step ${uiState.stepIndex + 1}/${uiState.totalSteps}",
        )

        if (uiState.totalSteps > 0) {
            CircularProgressIndicator(
                progress = {
                    uiState.stepIndex.toFloat() / uiState.totalSteps
                },
            )
        }

        Text(
            text = uiState.currentStep,
            textAlign = TextAlign.Center,
        )

        if (uiState.remainingSeconds > 0) {
            val minutes = uiState.remainingSeconds / 60
            val seconds = uiState.remainingSeconds % 60
            Text(text = "%d:%02d".format(minutes, seconds))
        }

        uiState.nextStep?.let {
            Text(text = "Up next: $it")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { viewModel.onDone() },
            ) { Text("Done") }
            FilledTonalButton(
                onClick = { viewModel.onSkip() },
            ) { Text("Skip") }
        }
    }
}
