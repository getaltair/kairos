package com.getaltair.kairos.sync.firestore

@JvmInline
value class CollectionPath(val value: String)

@JvmInline
value class DocumentPath(val value: String)

/**
 * Type-safe path builder for all Firestore collection and document paths.
 *
 * Every path follows the structure defined in the ERD (docs/08-erd.md).
 * Functions that end with a plural noun return a [CollectionPath];
 * functions that end with a singular noun return a [DocumentPath].
 */
object FirestoreCollections {

    // --- Users -----------------------------------------------------------

    /** Collection: all user documents. */
    fun users(): CollectionPath = CollectionPath("users")

    /** Document: a single user. */
    fun user(userId: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return DocumentPath("users/$userId")
    }

    // --- Habits ----------------------------------------------------------

    /** Collection: all habits for a user. */
    fun habits(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/habits")
    }

    /** Document: a single habit. */
    fun habit(userId: String, habitId: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(habitId.isNotBlank()) { "habitId must not be blank" }
        return DocumentPath("users/$userId/habits/$habitId")
    }

    // --- Completions -----------------------------------------------------

    /** Collection: all completions for a user. */
    fun completions(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/completions")
    }

    /** Document: a single completion. */
    fun completion(userId: String, completionId: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(completionId.isNotBlank()) { "completionId must not be blank" }
        return DocumentPath("users/$userId/completions/$completionId")
    }

    // --- Routines --------------------------------------------------------

    /** Collection: all routines for a user. */
    fun routines(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/routines")
    }

    /** Document: a single routine. */
    fun routine(userId: String, routineId: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        return DocumentPath("users/$userId/routines/$routineId")
    }

    // --- Routine Habits (subcollection of a routine) ---------------------

    /** Collection: habits belonging to a routine. */
    fun routineHabits(userId: String, routineId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        return CollectionPath("users/$userId/routines/$routineId/habits")
    }

    /** Document: a single routine-habit association. */
    fun routineHabit(userId: String, routineId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/routines/$routineId/habits/$id")
    }

    // --- Routine Variants (subcollection of a routine) -------------------

    /** Collection: variants belonging to a routine. */
    fun routineVariants(userId: String, routineId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        return CollectionPath("users/$userId/routines/$routineId/variants")
    }

    /** Document: a single routine variant. */
    fun routineVariant(userId: String, routineId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/routines/$routineId/variants/$id")
    }

    // --- Routine Executions ----------------------------------------------

    /** Collection: all routine executions for a user. */
    fun routineExecutions(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/routine_executions")
    }

    /** Document: a single routine execution. */
    fun routineExecution(userId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/routine_executions/$id")
    }

    // --- Recovery Sessions -----------------------------------------------

    /** Collection: all recovery sessions for a user. */
    fun recoverySessions(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/recovery_sessions")
    }

    /** Document: a single recovery session. */
    fun recoverySession(userId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/recovery_sessions/$id")
    }

    // --- Preferences -----------------------------------------------------

    /** Collection: user preferences documents. */
    fun preferences(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/preferences")
    }

    /** Document: a single preferences document. */
    fun preference(userId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/preferences/$id")
    }

    // --- Deletions -------------------------------------------------------

    /** Collection: soft-deletion log for a user. */
    fun deletions(userId: String): CollectionPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return CollectionPath("users/$userId/deletions")
    }

    /** Document: a single deletion record. */
    fun deletion(userId: String, id: String): DocumentPath {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        return DocumentPath("users/$userId/deletions/$id")
    }
}
