package com.getaltair.kairos.domain.enums

/**
 * Represents the lifecycle phase of a habit in the habit formation journey.
 *
 * The phases follow a structured progression based on habit formation science:
 * - ONBOARD: Initial state when a habit is created but not yet actively pursued
 * - FORMING: The active habit-building phase (typically 21-66 days)
 * - MAINTAINING: The habit is established and requires less conscious effort
 * - LAPSED: The habit was maintained but has been skipped for a period
 * - RELAPSED: The habit was lapsed and has not been resumed for an extended period
 */
sealed class HabitPhase {
    /**
     * User-friendly display name for UI presentation
     */
    abstract val displayName: String

    /**
     * Determines if a transition from this phase to the target phase is valid.
     *
     * Valid transitions:
     * - ONBOARD -> FORMING
     * - FORMING -> MAINTAINING or LAPSED
     * - MAINTAINING -> LAPSED
     * - LAPSED -> FORMING or RELAPSED
     * - RELAPSED -> FORMING
     *
     * @param target The phase to transition to
     * @return true if the transition is valid, false otherwise
     */
    abstract fun canTransitionTo(target: HabitPhase): Boolean

    /**
     * Initial state when a habit is first created.
     * The habit is defined but no active tracking has begun.
     */
    object ONBOARD : HabitPhase() {
        override val displayName = "Onboarding"
        override fun canTransitionTo(target: HabitPhase): Boolean = target is FORMING
    }

    /**
     * Active habit-building phase.
     * This is when the user is consciously working to establish the habit.
     */
    object FORMING : HabitPhase() {
        override val displayName = "Forming"
        override fun canTransitionTo(target: HabitPhase): Boolean = target is MAINTAINING || target is LAPSED
    }

    /**
     * The habit is established and requires minimal conscious effort.
     * The user has successfully integrated the habit into their routine.
     */
    object MAINTAINING : HabitPhase() {
        override val displayName = "Maintaining"
        override fun canTransitionTo(target: HabitPhase): Boolean = target is LAPSED
    }

    /**
     * The habit was maintained but has been skipped recently.
     * The user can resume from this phase without starting over.
     */
    object LAPSED : HabitPhase() {
        override val displayName = "Lapsed"
        override fun canTransitionTo(target: HabitPhase): Boolean = target is FORMING || target is RELAPSED
    }

    /**
     * The habit has been lapsed for an extended period.
     * Recovery requires starting from the forming phase.
     */
    object RELAPSED : HabitPhase() {
        override val displayName = "Relapsed"
        override fun canTransitionTo(target: HabitPhase): Boolean = target is FORMING
    }

    companion object {
        /**
         * All available habit phases for iteration and validation.
         */
        val ALL: List<HabitPhase> by lazy { listOf(ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED) }

        /**
         * Map of all valid transitions for quick reference.
         * Key is the source phase, value is the list of valid target phases.
         */
        val VALID_TRANSITIONS: Map<HabitPhase, List<HabitPhase>> by lazy {
            mapOf(
                ONBOARD to listOf(FORMING),
                FORMING to listOf(MAINTAINING, LAPSED),
                MAINTAINING to listOf(LAPSED),
                LAPSED to listOf(FORMING, RELAPSED),
                RELAPSED to listOf(FORMING),
            )
        }

        /**
         * Finds a HabitPhase by its display name.
         *
         * @param displayName The display name to search for
         * @return The matching HabitPhase or null if not found
         */
        fun fromDisplayName(displayName: String): HabitPhase? =
            ALL.find { it.displayName.equals(displayName, ignoreCase = true) }

        /**
         * Finds a HabitPhase by its simple class name (e.g., "ONBOARD").
         *
         * @param name The simple name to search for
         * @return The matching HabitPhase or null if not found
         */
        fun fromSimpleName(name: String): HabitPhase? =
            ALL.find { it::class.simpleName?.equals(name, ignoreCase = true) == true }
    }
}
