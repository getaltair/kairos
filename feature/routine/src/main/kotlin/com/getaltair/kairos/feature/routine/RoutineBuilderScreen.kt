package com.getaltair.kairos.feature.routine

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import java.util.UUID
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Routine builder screen for creating or editing a routine.
 *
 * Layout: name field, category selector, available habits section with add buttons,
 * selected habits section with drag-to-reorder and optional duration overrides.
 * Follows DESIGN.md: no borders, tonal surface layering, generous spacing,
 * rounded corners, understated inputs.
 *
 * @param routineId Null for create mode, non-null for edit mode
 * @param onNavigateBack Navigate back to the previous screen
 * @param onRoutineSaved Called when the routine is saved with the new routine's UUID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    routineId: String? = null,
    onNavigateBack: () -> Unit,
    onRoutineSaved: (UUID) -> Unit,
    viewModel: RoutineBuilderViewModel = koinViewModel(parameters = { parametersOf(routineId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate on save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved && uiState.savedRoutineId != null) {
            onRoutineSaved(UUID.fromString(uiState.savedRoutineId))
        }
    }

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Edit Routine" else "Create Routine")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isLoading && uiState.availableHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Name field
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Routine name") },
                        placeholder = { Text("Morning flow, Wind-down, etc.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Category selector
                item {
                    CategorySelector(
                        selectedCategory = uiState.category,
                        onCategorySelected = { viewModel.updateCategory(it) },
                    )
                }

                // Selected habits section
                if (uiState.selectedHabits.isNotEmpty()) {
                    item {
                        Text(
                            text = "In this routine (${uiState.selectedHabits.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    itemsIndexed(
                        uiState.selectedHabits,
                        key = { _, selectedHabit -> selectedHabit.habit.id },
                    ) { index, selectedHabit ->
                        SelectedHabitRow(
                            habit = selectedHabit.habit,
                            durationOverride = selectedHabit.overrideDurationSeconds,
                            index = index,
                            totalCount = uiState.selectedHabits.size,
                            onRemove = { viewModel.removeHabit(selectedHabit.habit.id) },
                            onDurationChanged = { seconds ->
                                viewModel.setDurationOverride(selectedHabit.habit.id, seconds)
                            },
                            onReorder = { fromIdx, toIdx ->
                                viewModel.reorderHabits(fromIdx, toIdx)
                            },
                        )
                    }
                }

                // Available habits section
                item {
                    Text(
                        text = "Available habits",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val selectedIds = uiState.selectedHabits.map { it.habit.id }.toSet()
                val availableToAdd = uiState.availableHabits.filter { it.id !in selectedIds }

                if (availableToAdd.isEmpty()) {
                    item {
                        Text(
                            text = if (uiState.availableHabits.isEmpty()) {
                                "No habits found. Create some habits first."
                            } else {
                                "All habits have been added."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(availableToAdd, key = { it.id }) { habit ->
                        AvailableHabitRow(
                            habit = habit,
                            onAdd = { viewModel.addHabit(habit) },
                        )
                    }
                }

                // Save button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isLoading &&
                            uiState.name.isNotBlank() &&
                            uiState.selectedHabits.size >= 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (uiState.isEditMode) "Save Changes" else "Create Routine",
                            )
                        }
                    }
                    if (uiState.selectedHabits.size < 2) {
                        Text(
                            text = buildString {
                                val remaining = 2 - uiState.selectedHabits.size
                                append("Add at least $remaining more habit")
                                if (uiState.selectedHabits.isEmpty()) append("s")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dropdown selector for routine category (Morning, Afternoon, Evening, Anytime).
 * Excludes Departure category since it's not a valid routine category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(selectedCategory: HabitCategory, onCategorySelected: (HabitCategory) -> Unit,) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf(
        HabitCategory.Morning,
        HabitCategory.Afternoon,
        HabitCategory.Evening,
        HabitCategory.Anytime,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text("${category.emoji} ${category.displayName}")
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Row displaying a habit that has been added to the routine.
 * Shows a drag handle, habit name, optional duration override input,
 * and a remove button. Supports drag-to-reorder via long press.
 */
@Composable
private fun SelectedHabitRow(
    habit: Habit,
    durationOverride: Int?,
    index: Int,
    totalCount: Int,
    onRemove: () -> Unit,
    onDurationChanged: (Int?) -> Unit,
    onReorder: (Int, Int) -> Unit,
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "drag_bg",
    )

    val minutes = durationOverride?.let { it / 60 } ?: (habit.estimatedSeconds / 60)
    var durationText by remember(durationOverride, habit.estimatedSeconds) {
        mutableStateOf(minutes.toString())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drag handle
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .pointerInput(index, totalCount) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            // Determine target index based on offset
                            val itemHeight = 56.dp.toPx()
                            val steps = (offsetY / itemHeight).roundToInt()
                            val targetIndex = (index + steps).coerceIn(0, totalCount - 1)
                            if (targetIndex != index) {
                                onReorder(index, targetIndex)
                            }
                            offsetY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY += dragAmount.y
                        },
                    )
                },
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Step number
        Text(
            text = "${index + 1}.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Habit name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Duration input (minutes)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = durationText,
                onValueChange = { text ->
                    durationText = text
                    val parsed = text.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onDurationChanged(parsed * 60) // Convert minutes to seconds
                    }
                },
                modifier = Modifier.width(56.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(
                text = "min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ${habit.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Row displaying an available habit that can be added to the routine.
 * Shows the habit name and an add button.
 */
@Composable
private fun AvailableHabitRow(habit: Habit, onAdd: () -> Unit,) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (habit.icon != null) {
            Text(
                text = habit.icon!!,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${habit.category.emoji} ${habit.category.displayName} - ${habit.estimatedSeconds / 60}min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add ${habit.name}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}
