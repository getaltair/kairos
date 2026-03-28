package com.getaltair.kairos.domain.wear

/**
 * Canonical Data Layer paths shared between phone and watch modules.
 * Lives in the domain module so both sides share a single source of truth.
 */
object WearDataPaths {
    const val PATH_TODAY_HABITS = "/kairos/today/habits"
    const val PATH_TODAY_COMPLETIONS = "/kairos/today/completions"
    const val PATH_ROUTINE_ACTIVE = "/kairos/routine/active"
    const val PATH_CONFIG = "/kairos/config"

    const val MESSAGE_HABIT_COMPLETED = "/kairos/message/habit_completed"
    const val MESSAGE_HABIT_SKIPPED = "/kairos/message/habit_skipped"
    const val MESSAGE_ROUTINE_STARTED = "/kairos/message/routine_started"
    const val MESSAGE_ROUTINE_STEP_DONE = "/kairos/message/routine_step_done"
    const val MESSAGE_ROUTINE_PAUSED = "/kairos/message/routine_paused"
    const val MESSAGE_ROUTINE_STEP_SKIPPED = "/kairos/message/routine_step_skipped"

    const val CAPABILITY_PHONE = "kairos_phone"
}
