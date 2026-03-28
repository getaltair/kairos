package com.getaltair.kairos.wear.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main watch screen showing today's habits grouped by category.
 * Completed habits are shown as disabled, incomplete habits are tappable.
 * A "Routines" entry at the bottom navigates to the routine list.
 */
@Composable
fun HabitListScreen(
    onHabitClick: (String) -> Unit,
    onRoutinesClick: () -> Unit,
    viewModel: HabitListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            ListHeader {
                Text(
                    text = if (uiState.isLoading) {
                        "Today"
                    } else {
                        "Today ${uiState.completedCount}/${uiState.totalCount}"
                    },
                )
            }
        }

        uiState.habitsByCategory.forEach { (category, habits) ->
            item {
                ListHeader {
                    Text(
                        text = category.lowercase().replaceFirstChar { it.uppercase() },
                    )
                }
            }
            items(habits, key = { it.id }) { habit ->
                val isCompleted = habit.id in uiState.completedIds
                FilledTonalButton(
                    onClick = { if (!isCompleted) onHabitClick(habit.id) },
                    enabled = !isCompleted,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isCompleted) "\u2713 ${habit.name}" else habit.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = onRoutinesClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Routines")
            }
        }
    }
}
