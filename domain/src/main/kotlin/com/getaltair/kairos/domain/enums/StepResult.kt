package com.getaltair.kairos.domain.enums

/**
 * Represents the outcome of a single step within a routine execution.
 * Each step can be completed (habit done) or skipped (habit intentionally bypassed).
 */
sealed class StepResult {
    data object Completed : StepResult()

    data object Skipped : StepResult()
}
