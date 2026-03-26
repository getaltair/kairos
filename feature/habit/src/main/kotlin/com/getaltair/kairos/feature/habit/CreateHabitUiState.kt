package com.getaltair.kairos.feature.habit

import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.time.DayOfWeek

enum class WizardStep { NAME, ANCHOR, CATEGORY, OPTIONS }

/**
 * Represents the lifecycle of a habit creation attempt.
 *
 * Transitions: Idle -> Creating -> Created (terminal) or
 * Creating -> Failed -> Idle (via [CreateHabitViewModel.clearCreationError]).
 */
sealed interface CreationStatus {
    data object Idle : CreationStatus
    data object Creating : CreationStatus
    data object Created : CreationStatus
    data class Failed(val message: String) : CreationStatus
}

data class CreateHabitUiState(
    val currentStep: WizardStep = WizardStep.NAME,
    val name: String = "",
    val nameError: String? = null,
    val anchorType: AnchorType = AnchorType.AfterBehavior,
    val anchorBehavior: String = "",
    val anchorError: String? = null,
    val anchorTime: String? = null,
    val category: HabitCategory? = null,
    val categoryError: String? = null,
    val estimatedSeconds: Int = 300,
    val microVersion: String = "",
    val icon: String? = null,
    val color: String? = null,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val activeDays: Set<DayOfWeek> = emptySet(),
    val creationStatus: CreationStatus = CreationStatus.Idle
)
