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
import androidx.compose.material3.Button
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
import com.getaltair.kairos.feature.habit.steps.AnchorStep
import com.getaltair.kairos.feature.habit.steps.CategoryStep
import com.getaltair.kairos.feature.habit.steps.NameStep
import com.getaltair.kairos.feature.habit.steps.OptionsStep
import java.util.UUID
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    habitId: UUID,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditHabitViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(habitId) {
        viewModel.loadHabit(habitId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = { Text("Edit Habit") }
                )
                if (uiState.isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
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

            uiState.loadError != null && uiState.habitId == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.loadError ?: "Something went wrong",
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

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    NameStep(
                        name = uiState.name,
                        nameError = uiState.nameError,
                        onNameChanged = viewModel::onNameChanged,
                        onContinue = { /* no-op in edit mode */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AnchorStep(
                        anchorType = uiState.anchorType,
                        anchorBehavior = uiState.anchorBehavior,
                        anchorTime = uiState.anchorTime,
                        anchorError = uiState.anchorError,
                        onAnchorTypeSelected = viewModel::onAnchorTypeSelected,
                        onAnchorBehaviorChanged = viewModel::onAnchorBehaviorChanged,
                        onAnchorTimeChanged = viewModel::onAnchorTimeChanged,
                        onContinue = { /* no-op in edit mode */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CategoryStep(
                        selected = uiState.category,
                        categoryError = uiState.categoryError,
                        onCategorySelected = viewModel::onCategorySelected,
                        onContinue = { /* no-op in edit mode */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OptionsStep(
                        estimatedSeconds = uiState.estimatedSeconds,
                        microVersion = uiState.microVersion,
                        icon = uiState.icon,
                        color = uiState.color,
                        frequency = uiState.frequency,
                        activeDays = uiState.activeDays,
                        isCreating = uiState.isSaving,
                        onEstimatedSecondsChanged = viewModel::onEstimatedSecondsChanged,
                        onMicroVersionChanged = viewModel::onMicroVersionChanged,
                        onIconSelected = viewModel::onIconSelected,
                        onColorSelected = viewModel::onColorSelected,
                        onFrequencySelected = viewModel::onFrequencySelected,
                        onActiveDaysChanged = viewModel::onActiveDaysChanged,
                        onCreateHabit = { /* no-op, save button below */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = viewModel::saveHabit,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
