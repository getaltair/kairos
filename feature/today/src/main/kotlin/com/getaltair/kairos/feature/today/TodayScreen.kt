package com.getaltair.kairos.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.feature.today.components.CompletionBottomSheet
import com.getaltair.kairos.feature.today.components.EmptyState
import com.getaltair.kairos.feature.today.components.HabitCard
import com.getaltair.kairos.feature.today.components.ProgressRing
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(onAddHabit: () -> Unit = {}, viewModel: TodayViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var selectedHabit by remember { mutableStateOf<HabitWithStatus?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show undo snackbar
    LaunchedEffect(uiState.undoState?.completionId) {
        uiState.undoState?.let { undo ->
            val actionLabel = when (undo.actionType) {
                UndoActionType.COMPLETE -> "marked complete"
                UndoActionType.PARTIAL -> "partially completed"
                UndoActionType.SKIP -> "skipped"
            }
            val result = snackbarHostState.showSnackbar(
                message = "\"${undo.habitName}\" $actionLabel",
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoCompletion()
            } else {
                viewModel.onDismissUndo()
            }
        }
    }

    // Show transient error snackbar when habits are already loaded
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            if (uiState.habitsByCategory.isNotEmpty()) {
                snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.date.format(
                            DateTimeFormatter.ofPattern("EEEE, MMMM d")
                        )
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Filled.Add, contentDescription = "Add habit")
            }
        }
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

            uiState.error != null && uiState.habitsByCategory.isEmpty() -> {
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
                        OutlinedButton(onClick = { viewModel.retryLoad() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.isEmpty -> {
                EmptyState(modifier = Modifier.padding(paddingValues))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Progress header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProgressRing(progress = uiState.progress)
                        }
                    }

                    // All done celebration
                    if (uiState.isAllDone) {
                        item {
                            Text(
                                text = "All done for today! Great work!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Category groups
                    uiState.habitsByCategory.forEach { (category, habits) ->
                        item {
                            Text(
                                text = "${category.emoji} ${category.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(habits, key = { it.habit.id }) { habitWithStatus ->
                            HabitCard(
                                habitWithStatus = habitWithStatus,
                                onClick = { selectedHabit = habitWithStatus }
                            )
                        }
                    }
                }
            }
        }
    }

    // Completion bottom sheet
    selectedHabit?.let { habitWithStatus ->
        CompletionBottomSheet(
            habit = habitWithStatus.habit,
            onDone = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onHabitComplete(
                    habitWithStatus.habit.id,
                    CompletionType.Full
                )
            },
            onPartial = { percent ->
                viewModel.onHabitComplete(
                    habitWithStatus.habit.id,
                    CompletionType.Partial,
                    percent
                )
            },
            onSkip = { reason ->
                viewModel.onHabitSkip(habitWithStatus.habit.id, reason)
            },
            onDismiss = { selectedHabit = null }
        )
    }
}
