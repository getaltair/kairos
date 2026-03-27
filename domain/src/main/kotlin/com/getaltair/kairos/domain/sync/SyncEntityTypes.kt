package com.getaltair.kairos.domain.sync

/**
 * Canonical entity type names used by [SyncTrigger].
 * These must match the enum values in the sync module's SyncEntityType.
 */
object SyncEntityTypes {
    const val HABIT = "HABIT"
    const val COMPLETION = "COMPLETION"
    const val ROUTINE = "ROUTINE"
    const val ROUTINE_HABIT = "ROUTINE_HABIT"
    const val ROUTINE_VARIANT = "ROUTINE_VARIANT"
    const val ROUTINE_EXECUTION = "ROUTINE_EXECUTION"
    const val RECOVERY_SESSION = "RECOVERY_SESSION"
    const val USER_PREFERENCE = "USER_PREFERENCE"
}
