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
    ROUTINE_HABIT,
    ROUTINE_VARIANT,
    ROUTINE_EXECUTION,
    RECOVERY_SESSION,
    USER_PREFERENCE,
}
