package com.getaltair.kairos.feature.habit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.feature.habit.steps.AnchorStep
import com.getaltair.kairos.feature.habit.steps.CategoryStep
import com.getaltair.kairos.feature.habit.steps.NameStep
import com.getaltair.kairos.feature.habit.steps.OptionsStep
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(onBack: () -> Unit, onCreated: () -> Unit, viewModel: CreateHabitViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) {
            onCreated()
        }
    }

    LaunchedEffect(uiState.creationError) {
        uiState.creationError?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearCreationError()
        }
    }

    val stepNumber = uiState.currentStep.ordinal + 1
    val stepTitle = when (uiState.currentStep) {
        WizardStep.NAME -> "Name your habit"
        WizardStep.ANCHOR -> "Choose an anchor"
        WizardStep.CATEGORY -> "Choose a category"
        WizardStep.OPTIONS -> "Optional details"
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.currentStep == WizardStep.NAME) {
                                    onBack()
                                } else {
                                    viewModel.goToPreviousStep()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text(text = "$stepTitle ($stepNumber/${WizardStep.entries.size})")
                    }
                )
                if (uiState.isCreating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "wizard_step"
                ) { step ->
                    when (step) {
                        WizardStep.NAME -> NameStep(
                            name = uiState.name,
                            nameError = uiState.nameError,
                            onNameChanged = viewModel::onNameChanged,
                            onContinue = viewModel::goToNextStep
                        )

                        WizardStep.ANCHOR -> AnchorStep(
                            anchorType = uiState.anchorType,
                            anchorBehavior = uiState.anchorBehavior,
                            anchorTime = uiState.anchorTime,
                            anchorError = uiState.anchorError,
                            onAnchorTypeSelected = viewModel::onAnchorTypeSelected,
                            onAnchorBehaviorChanged = viewModel::onAnchorBehaviorChanged,
                            onAnchorTimeChanged = viewModel::onAnchorTimeChanged,
                            onContinue = viewModel::goToNextStep
                        )

                        WizardStep.CATEGORY -> CategoryStep(
                            selected = uiState.category,
                            categoryError = uiState.categoryError,
                            onCategorySelected = viewModel::onCategorySelected,
                            onContinue = viewModel::goToNextStep
                        )

                        WizardStep.OPTIONS -> OptionsStep(
                            estimatedSeconds = uiState.estimatedSeconds,
                            microVersion = uiState.microVersion,
                            icon = uiState.icon,
                            color = uiState.color,
                            frequency = uiState.frequency,
                            activeDays = uiState.activeDays,
                            isCreating = uiState.isCreating,
                            onEstimatedSecondsChanged = viewModel::onEstimatedSecondsChanged,
                            onMicroVersionChanged = viewModel::onMicroVersionChanged,
                            onIconSelected = viewModel::onIconSelected,
                            onColorSelected = viewModel::onColorSelected,
                            onFrequencySelected = viewModel::onFrequencySelected,
                            onActiveDaysChanged = viewModel::onActiveDaysChanged,
                            onCreateHabit = viewModel::createHabit
                        )
                    }
                }
            }
        }
    }
}
