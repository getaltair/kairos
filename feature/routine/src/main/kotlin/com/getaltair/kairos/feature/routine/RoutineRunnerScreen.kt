package com.getaltair.kairos.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.feature.routine.components.StepIndicator
import com.getaltair.kairos.feature.routine.components.TimerDisplay
import com.getaltair.kairos.ui.icons.filled.Pause
import com.getaltair.kairos.ui.icons.filled.SkipNext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Routine runner screen providing timer-led guided execution.
 *
 * Layout (top to bottom):
 * - Top bar: routine name + step counter
 * - Step indicator dots
 * - Current habit name (large)
 * - Timer display (circular countdown)
 * - Control buttons: Skip, Done, Pause/Resume
 * - Up next preview
 *
 * Follows DESIGN.md: dark sanctuary background, teal primary actions,
 * generous spacing, no borders, large touch targets (48dp+).
 *
 * @param routineId ID of the routine to run
 * @param onComplete Called when the routine is complete with the execution ID
 * @param onNavigateBack Navigate back (abandons the routine)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineRunnerScreen(
    routineId: String,
    onComplete: (executionId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RoutineRunnerViewModel = koinViewModel(parameters = { parametersOf(routineId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAbandonDialog by remember { mutableStateOf(false) }

    // Navigate to summary on completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && uiState.executionId != null) {
            onComplete(uiState.executionId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading) {
                        Column {
                            Text(
                                text = uiState.routineName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Step ${uiState.currentStepIndex + 1} of ${uiState.totalSteps}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showAbandonDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave routine",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparing your routine...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    ) {
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Go back")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Step indicator
                    StepIndicator(
                        currentIndex = uiState.currentStepIndex,
                        stepResults = uiState.stepResults,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Current habit name
                    Text(
                        text = uiState.currentHabitName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Timer display
                    TimerDisplay(
                        timeRemainingSeconds = uiState.timeRemainingSeconds,
                        totalTimeSeconds = uiState.totalTimeSeconds,
                        habitName = uiState.currentHabitName,
                        isPaused = uiState.isPaused,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Skip button
                        FilledTonalButton(
                            onClick = { viewModel.onSkip() },
                            modifier = Modifier.size(width = 96.dp, height = 48.dp),
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Skip",
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip")
                        }

                        // Done button (primary)
                        Button(
                            onClick = { viewModel.onDone() },
                            modifier = Modifier.size(width = 120.dp, height = 56.dp),
                            shape = RoundedCornerShape(28.dp),
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Done",
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Done",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Pause/Resume button
                        FilledTonalButton(
                            onClick = {
                                if (uiState.isPaused) viewModel.onResume() else viewModel.onPause()
                            },
                            modifier = Modifier.size(width = 96.dp, height = 48.dp),
                        ) {
                            Icon(
                                if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isPaused) "Go" else "Pause")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Up next preview
                    val upNext = uiState.upNextHabitName
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (upNext != null) {
                            Text(
                                text = "Up next: $upNext",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "Last step",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Abandon confirmation dialog
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Leave routine?") },
            text = {
                Text("Your progress on completed steps will be saved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbandonDialog = false
                        // C3 FIX: Pass navigation as callback to avoid race condition
                        viewModel.onAbandon { onNavigateBack() }
                    },
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("Continue")
                }
            },
        )
    }
}
