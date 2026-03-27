package com.getaltair.kairos.feature.recovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Recovery session wizard screen.
 *
 * Guides the user through a warm, shame-free flow: Welcome, optional blocker
 * selection, action selection, and confirmation. All copy follows FR-6 messaging
 * rules. Visual styling follows DESIGN.md (no hard borders, generous spacing,
 * soft tones, rounded shapes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySessionScreen(
    habitId: String,
    onComplete: () -> Unit,
    viewModel: RecoverySessionViewModel = koinViewModel(parameters = { parametersOf(habitId) })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == RecoveryStep.Complete) onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (uiState.currentStep != RecoveryStep.Welcome) {
                        IconButton(onClick = viewModel::goBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = when (uiState.currentStep) {
                            RecoveryStep.Welcome -> ""
                            RecoveryStep.BlockerSelection -> "Reflection"
                            RecoveryStep.ActionSelection -> "Your Path Forward"
                            RecoveryStep.Confirmation -> "All Set"
                            RecoveryStep.Complete -> ""
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading && uiState.habit == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.habit == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text(text = "Try Again")
                        }
                    }
                }

                else -> {
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        },
                        label = "recovery_step"
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (step) {
                                RecoveryStep.Welcome -> WelcomeStep(
                                    habitName = uiState.habit?.name.orEmpty(),
                                    habitIcon = uiState.habit?.icon,
                                    isRelapse = uiState.session?.type is RecoveryType.Relapse,
                                    onProceed = viewModel::proceedFromWelcome
                                )

                                RecoveryStep.BlockerSelection -> BlockerSelectionStep(
                                    selectedBlockers = uiState.selectedBlockers,
                                    onToggleBlocker = viewModel::toggleBlocker,
                                    onSkip = viewModel::skipBlockers,
                                    onContinue = viewModel::proceedFromBlockers
                                )

                                RecoveryStep.ActionSelection -> ActionSelectionStep(
                                    isSimplifyEnabled = uiState.isSimplifyEnabled,
                                    isFreshStartAvailable = uiState.isFreshStartAvailable,
                                    onChooseAction = viewModel::chooseAction
                                )

                                RecoveryStep.Confirmation -> ConfirmationStep(
                                    chosenAction = uiState.chosenAction,
                                    confirmationMessage = uiState.confirmationMessage.orEmpty(),
                                    isLoading = uiState.isLoading,
                                    error = uiState.error,
                                    onDone = viewModel::confirmAction
                                )

                                RecoveryStep.Complete -> {
                                    // Terminal state -- LaunchedEffect triggers onComplete()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Step composables
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeStep(habitName: String, habitIcon: String?, isRelapse: Boolean, onProceed: () -> Unit) {
    Spacer(modifier = Modifier.height(48.dp))

    // Habit icon if available
    if (!habitIcon.isNullOrBlank()) {
        Text(
            text = habitIcon,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text(
        text = if (isRelapse) "It's good to see you!" else "Welcome back!",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = habitName,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = if (isRelapse) {
            "Life happens. Let's figure out the best path forward together."
        } else {
            "Let's figure this out together."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onProceed,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Let's talk about it")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockerSelectionStep(
    selectedBlockers: Set<Blocker>,
    onToggleBlocker: (Blocker) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "What got in the way?",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "(optional -- this helps us help you)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        allBlockers.forEach { blocker ->
            FilterChip(
                selected = blocker in selectedBlockers,
                onClick = { onToggleBlocker(blocker) },
                label = { Text(text = blocker.displayName) }
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Continue")
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Skip")
    }
}

@Composable
private fun ActionSelectionStep(
    isSimplifyEnabled: Boolean,
    isFreshStartAvailable: Boolean,
    onChooseAction: (RecoveryAction) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "What would you like to do?",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    ActionCard(
        icon = Icons.Filled.PlayArrow,
        title = "Resume",
        description = "Pick up where you left off",
        onClick = { onChooseAction(RecoveryAction.Resume) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    ActionCard(
        icon = Icons.Filled.Compress,
        title = "Simplify",
        description = "Try a smaller version",
        enabled = isSimplifyEnabled,
        onClick = { onChooseAction(RecoveryAction.Simplify) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    ActionCard(
        icon = Icons.Filled.Pause,
        title = "Pause",
        description = "Take a break",
        onClick = { onChooseAction(RecoveryAction.Pause) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    ActionCard(
        icon = Icons.Filled.Archive,
        title = "Archive",
        description = "Mark this complete",
        onClick = { onChooseAction(RecoveryAction.Archive) }
    )

    if (isFreshStartAvailable) {
        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Filled.RestartAlt,
            title = "Fresh Start",
            description = "Start fresh",
            onClick = { onChooseAction(RecoveryAction.FreshStart) }
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun ConfirmationStep(
    chosenAction: RecoveryAction?,
    confirmationMessage: String,
    isLoading: Boolean,
    error: String? = null,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = if (chosenAction is RecoveryAction.FreshStart) {
            Icons.Filled.AutoAwesome
        } else {
            Icons.Filled.CheckCircle
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(72.dp)
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = chosenAction?.displayName ?: "",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = confirmationMessage,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onDone,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text = "Done")
        }
    }

    if (error != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * All available blocker options.
 *
 * Sealed-class data objects do not have an auto-generated `entries` list, so
 * we enumerate them manually.
 */
private val allBlockers: List<Blocker> = listOf(
    Blocker.NoEnergy,
    Blocker.PainPhysical,
    Blocker.PainMental,
    Blocker.TooBusy,
    Blocker.FamilyEmergency,
    Blocker.WorkEmergency,
    Blocker.Sick,
    Blocker.Weather,
    Blocker.EquipmentFailure,
    Blocker.Other
)
