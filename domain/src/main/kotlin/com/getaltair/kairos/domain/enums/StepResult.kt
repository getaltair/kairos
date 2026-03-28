package com.getaltair.kairos.domain.enums

/**
 * Represents the outcome of a single step within a routine execution.
 * Each step can be completed (habit done) or skipped (habit intentionally bypassed).
 */
sealed class StepResult {
    abstract val displayName: String

    data object Completed : StepResult() {
        override val displayName: String = "Completed"
    }

    data object Skipped : StepResult() {
        override val displayName: String = "Skipped"
    }
}
