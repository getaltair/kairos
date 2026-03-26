package com.getaltair.kairos.sync.firestore

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreCollectionsTest {

    // Realistic IDs used across all tests
    private val userId = "user-abc-123"
    private val habitId = "habit-def-456"
    private val completionId = "completion-ghi-789"
    private val routineId = "routine-jkl-012"
    private val routineHabitId = "rh-mno-345"
    private val variantId = "variant-pqr-678"
    private val executionId = "exec-stu-901"
    private val recoveryId = "recovery-vwx-234"
    private val preferenceId = "pref-yza-567"
    private val deletionId = "del-bcd-890"

    // --- Users ---------------------------------------------------------------

    @Test
    fun `users() returns top-level collection path`() {
        assertEquals("users", FirestoreCollections.users())
    }

    @Test
    fun `user() returns document path`() {
        assertEquals(
            "users/$userId",
            FirestoreCollections.user(userId),
        )
    }

    // --- Habits --------------------------------------------------------------

    @Test
    fun `habits() returns subcollection path`() {
        assertEquals(
            "users/$userId/habits",
            FirestoreCollections.habits(userId),
        )
    }

    @Test
    fun `habit() returns document path`() {
        assertEquals(
            "users/$userId/habits/$habitId",
            FirestoreCollections.habit(userId, habitId),
        )
    }

    // --- Completions ---------------------------------------------------------

    @Test
    fun `completions() returns subcollection path`() {
        assertEquals(
            "users/$userId/completions",
            FirestoreCollections.completions(userId),
        )
    }

    @Test
    fun `completion() returns document path`() {
        assertEquals(
            "users/$userId/completions/$completionId",
            FirestoreCollections.completion(userId, completionId),
        )
    }

    // --- Routines ------------------------------------------------------------

    @Test
    fun `routines() returns subcollection path`() {
        assertEquals(
            "users/$userId/routines",
            FirestoreCollections.routines(userId),
        )
    }

    @Test
    fun `routine() returns document path`() {
        assertEquals(
            "users/$userId/routines/$routineId",
            FirestoreCollections.routine(userId, routineId),
        )
    }

    // --- Routine Habits (nested subcollection) --------------------------------

    @Test
    fun `routineHabits() returns nested subcollection path`() {
        assertEquals(
            "users/$userId/routines/$routineId/habits",
            FirestoreCollections.routineHabits(userId, routineId),
        )
    }

    @Test
    fun `routineHabit() returns nested document path`() {
        assertEquals(
            "users/$userId/routines/$routineId/habits/$routineHabitId",
            FirestoreCollections.routineHabit(userId, routineId, routineHabitId),
        )
    }

    // --- Routine Variants (nested subcollection) -----------------------------

    @Test
    fun `routineVariants() returns nested subcollection path`() {
        assertEquals(
            "users/$userId/routines/$routineId/variants",
            FirestoreCollections.routineVariants(userId, routineId),
        )
    }

    @Test
    fun `routineVariant() returns nested document path`() {
        assertEquals(
            "users/$userId/routines/$routineId/variants/$variantId",
            FirestoreCollections.routineVariant(userId, routineId, variantId),
        )
    }

    // --- Routine Executions --------------------------------------------------

    @Test
    fun `routineExecutions() returns subcollection path`() {
        assertEquals(
            "users/$userId/routine_executions",
            FirestoreCollections.routineExecutions(userId),
        )
    }

    @Test
    fun `routineExecution() returns document path`() {
        assertEquals(
            "users/$userId/routine_executions/$executionId",
            FirestoreCollections.routineExecution(userId, executionId),
        )
    }

    // --- Recovery Sessions ---------------------------------------------------

    @Test
    fun `recoverySessions() returns subcollection path`() {
        assertEquals(
            "users/$userId/recovery_sessions",
            FirestoreCollections.recoverySessions(userId),
        )
    }

    @Test
    fun `recoverySession() returns document path`() {
        assertEquals(
            "users/$userId/recovery_sessions/$recoveryId",
            FirestoreCollections.recoverySession(userId, recoveryId),
        )
    }

    // --- Preferences ---------------------------------------------------------

    @Test
    fun `preferences() returns subcollection path`() {
        assertEquals(
            "users/$userId/preferences",
            FirestoreCollections.preferences(userId),
        )
    }

    @Test
    fun `preference() returns document path`() {
        assertEquals(
            "users/$userId/preferences/$preferenceId",
            FirestoreCollections.preference(userId, preferenceId),
        )
    }

    // --- Deletions -----------------------------------------------------------

    @Test
    fun `deletions() returns subcollection path`() {
        assertEquals(
            "users/$userId/deletions",
            FirestoreCollections.deletions(userId),
        )
    }

    @Test
    fun `deletion() returns document path`() {
        assertEquals(
            "users/$userId/deletions/$deletionId",
            FirestoreCollections.deletion(userId, deletionId),
        )
    }

    // --- Subcollection nesting depth -----------------------------------------

    @Test
    fun `nested paths maintain correct hierarchy depth`() {
        val routineHabitPath = FirestoreCollections.routineHabit(
            userId,
            routineId,
            routineHabitId,
        )
        val segments = routineHabitPath.split("/")
        // users / {userId} / routines / {routineId} / habits / {id} = 6
        assertEquals(6, segments.size)

        assertEquals("users", segments[0])
        assertEquals(userId, segments[1])
        assertEquals("routines", segments[2])
        assertEquals(routineId, segments[3])
        assertEquals("habits", segments[4])
        assertEquals(routineHabitId, segments[5])
    }

    @Test
    fun `collection path is always the prefix of its document path`() {
        val collection = FirestoreCollections.habits(userId)
        val document = FirestoreCollections.habit(userId, habitId)
        assert(document.startsWith("$collection/")) {
            "Document path should start with collection path + /"
        }
    }
}
