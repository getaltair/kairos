package com.getaltair.kairos.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.ui.icons.filled.SelfImprovement
import com.getaltair.kairos.ui.icons.filled.SkipNext
import com.getaltair.kairos.ui.icons.filled.Timer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Routine summary screen displayed after completing a routine.
 *
 * Shows: header with calm completion message (no streaks per D-1, no judgment per D-2),
 * stats (total time, habits done, habits skipped), per-habit result breakdown,
 * and a "Done" button.
 *
 * Follows DESIGN.md: warm amber for celebration, generous spacing,
 * tonal surface layering, rounded cards, no borders.
 *
 * @param executionId The UUID of the completed RoutineExecution
 * @param onDone Navigate away from the summary screen
 */
@Composable
fun RoutineSummaryScreen(
    executionId: String,
    onDone: () -> Unit,
    viewModel: RoutineSummaryViewModel = koinViewModel(parameters = { parametersOf(executionId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
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
                        Button(onClick = onDone) {
                            Text("Go back")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Completion header
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelfImprovement,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Routine complete",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.routineName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Stats row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatChip(
                                label = "Time",
                                value = formatDuration(uiState.totalTimeSeconds),
                                icon = Icons.Filled.Timer,
                            )
                            StatChip(
                                label = "Done",
                                value = "${uiState.habitsCompleted}",
                                icon = Icons.Filled.Check,
                            )
                            StatChip(
                                label = "Skipped",
                                value = "${uiState.habitsSkipped}",
                                icon = Icons.Filled.SkipNext,
                            )
                        }
                    }

                    // Per-habit breakdown
                    item {
                        Text(
                            text = "Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(uiState.habitResults) { item ->
                        HabitResultRow(item = item)
                    }

                    // Done button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

/**
 * A compact stat display chip for the summary.
 */
@Composable
private fun StatChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A row displaying a single habit's result in the summary.
 */
@Composable
private fun HabitResultRow(item: HabitSummaryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (item.wasCompleted) Icons.Filled.Check else Icons.Filled.SkipNext,
            contentDescription = if (item.wasCompleted) "Completed" else "Skipped",
            tint = if (item.wasCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.habitName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDuration(item.durationSeconds.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Formats a duration in seconds to a human-readable string.
 */
private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        "${mins}m ${secs}s"
    } else {
        "${secs}s"
    }
}
