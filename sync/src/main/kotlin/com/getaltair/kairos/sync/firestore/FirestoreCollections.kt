package com.getaltair.kairos.sync.firestore

/**
 * Type-safe path builder for all Firestore collection and document paths.
 *
 * Every path follows the structure defined in the ERD (docs/08-erd.md).
 * Functions that end with a plural noun return a collection path;
 * functions that end with a singular noun return a document path.
 */
object FirestoreCollections {

    // --- Users -----------------------------------------------------------

    /** Collection: all user documents. */
    fun users(): String = "users"

    /** Document: a single user. */
    fun user(userId: String): String = "users/$userId"

    // --- Habits ----------------------------------------------------------

    /** Collection: all habits for a user. */
    fun habits(userId: String): String = "users/$userId/habits"

    /** Document: a single habit. */
    fun habit(userId: String, habitId: String): String = "users/$userId/habits/$habitId"

    // --- Completions -----------------------------------------------------

    /** Collection: all completions for a user. */
    fun completions(userId: String): String = "users/$userId/completions"

    /** Document: a single completion. */
    fun completion(userId: String, completionId: String): String = "users/$userId/completions/$completionId"

    // --- Routines --------------------------------------------------------

    /** Collection: all routines for a user. */
    fun routines(userId: String): String = "users/$userId/routines"

    /** Document: a single routine. */
    fun routine(userId: String, routineId: String): String = "users/$userId/routines/$routineId"

    // --- Routine Habits (subcollection of a routine) ---------------------

    /** Collection: habits belonging to a routine. */
    fun routineHabits(userId: String, routineId: String): String = "users/$userId/routines/$routineId/habits"

    /** Document: a single routine-habit association. */
    fun routineHabit(userId: String, routineId: String, id: String): String =
        "users/$userId/routines/$routineId/habits/$id"

    // --- Routine Variants (subcollection of a routine) -------------------

    /** Collection: variants belonging to a routine. */
    fun routineVariants(userId: String, routineId: String): String = "users/$userId/routines/$routineId/variants"

    /** Document: a single routine variant. */
    fun routineVariant(userId: String, routineId: String, id: String): String =
        "users/$userId/routines/$routineId/variants/$id"

    // --- Routine Executions ----------------------------------------------

    /** Collection: all routine executions for a user. */
    fun routineExecutions(userId: String): String = "users/$userId/routine_executions"

    /** Document: a single routine execution. */
    fun routineExecution(userId: String, id: String): String = "users/$userId/routine_executions/$id"

    // --- Recovery Sessions -----------------------------------------------

    /** Collection: all recovery sessions for a user. */
    fun recoverySessions(userId: String): String = "users/$userId/recovery_sessions"

    /** Document: a single recovery session. */
    fun recoverySession(userId: String, id: String): String = "users/$userId/recovery_sessions/$id"

    // --- Preferences -----------------------------------------------------

    /** Collection: user preferences documents. */
    fun preferences(userId: String): String = "users/$userId/preferences"

    /** Document: a single preferences document. */
    fun preference(userId: String, id: String): String = "users/$userId/preferences/$id"

    // --- Deletions -------------------------------------------------------

    /** Collection: soft-deletion log for a user. */
    fun deletions(userId: String): String = "users/$userId/deletions"

    /** Document: a single deletion record. */
    fun deletion(userId: String, id: String): String = "users/$userId/deletions/$id"
}
