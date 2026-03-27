package com.getaltair.kairos.sync

/**
 * Enumeration of all entity types that participate in Firestore sync.
 * Used by [SyncManager.pushLocalChange] and [SyncManager.pushDeletion]
 * to resolve the correct Firestore collection path.
 */
enum class SyncEntityType {
    HABIT,
    COMPLETION,
    ROUTINE,

    /** Habit within a routine. Requires `routineId` in metadata for collection path resolution. */
    ROUTINE_HABIT,

    /** Variant within a routine. Requires `routineId` in metadata for collection path resolution. */
    ROUTINE_VARIANT,

    ROUTINE_EXECUTION,
    RECOVERY_SESSION,
    USER_PREFERENCE,
}
