package com.getaltair.kairos.feature.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.feature.habit.components.CompletionCalendar
import com.getaltair.kairos.feature.habit.components.HabitActionButtons
import java.util.UUID
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: UUID,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onDeleted: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(habitId) {
        viewModel.loadHabit(habitId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleted()
        }
    }

    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let { message ->
            snackbarHostState.showSnackbar(message = message)
            viewModel.clearActionResult()
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteDialog,
            title = { Text("Delete habit?") },
            text = {
                Text("This will permanently delete this habit and all its completions. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmed() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(text = uiState.habit?.name ?: "Habit Detail")
                },
                actions = {
                    if (uiState.habit != null && uiState.habit?.status !is HabitStatus.Archived) {
                        IconButton(onClick = { onEdit(habitId) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit habit"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.habit == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadHabit(habitId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.habit != null -> {
                val habit = uiState.habit!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${habit.anchorType.displayName} ${habit.anchorBehavior}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${habit.category.emoji} ${habit.category.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val durationMinutes = habit.estimatedSeconds / 60
                    Text(
                        text = if (durationMinutes == 1) "1 minute" else "$durationMinutes minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Status: ${habit.status.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = when (habit.status) {
                            is HabitStatus.Active -> MaterialTheme.colorScheme.primary
                            is HabitStatus.Paused -> MaterialTheme.colorScheme.tertiary
                            is HabitStatus.Archived -> MaterialTheme.colorScheme.outline
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Weekly completion rate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val ratePercent = (uiState.weeklyCompletionRate * 100).toInt()
                    Text(
                        text = "$ratePercent%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LinearProgressIndicator(
                        progress = { uiState.weeklyCompletionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CompletionCalendar(
                        completions = uiState.recentCompletions,
                        onDateTapped = { date ->
                            viewModel.onBackdate(date, CompletionType.Full)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HabitActionButtons(
                        status = habit.status,
                        onPause = viewModel::onPause,
                        onResume = viewModel::onResume,
                        onArchive = viewModel::onArchive,
                        onRestore = viewModel::onRestore,
                        onDelete = viewModel::onDeleteRequested
                    )
                }
            }
        }
    }
}
