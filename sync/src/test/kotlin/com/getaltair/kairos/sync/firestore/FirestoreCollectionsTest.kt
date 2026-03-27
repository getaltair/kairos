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
        assertEquals("users", FirestoreCollections.users().value)
    }

    @Test
    fun `user() returns document path`() {
        assertEquals(
            "users/$userId",
            FirestoreCollections.user(userId).value,
        )
    }

    // --- Habits --------------------------------------------------------------

    @Test
    fun `habits() returns subcollection path`() {
        assertEquals(
            "users/$userId/habits",
            FirestoreCollections.habits(userId).value,
        )
    }

    @Test
    fun `habit() returns document path`() {
        assertEquals(
            "users/$userId/habits/$habitId",
            FirestoreCollections.habit(userId, habitId).value,
        )
    }

    // --- Completions ---------------------------------------------------------

    @Test
    fun `completions() returns subcollection path`() {
        assertEquals(
            "users/$userId/completions",
            FirestoreCollections.completions(userId).value,
        )
    }

    @Test
    fun `completion() returns document path`() {
        assertEquals(
            "users/$userId/completions/$completionId",
            FirestoreCollections.completion(userId, completionId).value,
        )
    }

    // --- Routines ------------------------------------------------------------

    @Test
    fun `routines() returns subcollection path`() {
        assertEquals(
            "users/$userId/routines",
            FirestoreCollections.routines(userId).value,
        )
    }

    @Test
    fun `routine() returns document path`() {
        assertEquals(
            "users/$userId/routines/$routineId",
            FirestoreCollections.routine(userId, routineId).value,
        )
    }

    // --- Routine Habits (nested subcollection) --------------------------------

    @Test
    fun `routineHabits() returns nested subcollection path`() {
        assertEquals(
            "users/$userId/routines/$routineId/habits",
            FirestoreCollections.routineHabits(userId, routineId).value,
        )
    }

    @Test
    fun `routineHabit() returns nested document path`() {
        assertEquals(
            "users/$userId/routines/$routineId/habits/$routineHabitId",
            FirestoreCollections.routineHabit(userId, routineId, routineHabitId).value,
        )
    }

    // --- Routine Variants (nested subcollection) -----------------------------

    @Test
    fun `routineVariants() returns nested subcollection path`() {
        assertEquals(
            "users/$userId/routines/$routineId/variants",
            FirestoreCollections.routineVariants(userId, routineId).value,
        )
    }

    @Test
    fun `routineVariant() returns nested document path`() {
        assertEquals(
            "users/$userId/routines/$routineId/variants/$variantId",
            FirestoreCollections.routineVariant(userId, routineId, variantId).value,
        )
    }

    // --- Routine Executions --------------------------------------------------

    @Test
    fun `routineExecutions() returns subcollection path`() {
        assertEquals(
            "users/$userId/routine_executions",
            FirestoreCollections.routineExecutions(userId).value,
        )
    }

    @Test
    fun `routineExecution() returns document path`() {
        assertEquals(
            "users/$userId/routine_executions/$executionId",
            FirestoreCollections.routineExecution(userId, executionId).value,
        )
    }

    // --- Recovery Sessions ---------------------------------------------------

    @Test
    fun `recoverySessions() returns subcollection path`() {
        assertEquals(
            "users/$userId/recovery_sessions",
            FirestoreCollections.recoverySessions(userId).value,
        )
    }

    @Test
    fun `recoverySession() returns document path`() {
        assertEquals(
            "users/$userId/recovery_sessions/$recoveryId",
            FirestoreCollections.recoverySession(userId, recoveryId).value,
        )
    }

    // --- Preferences ---------------------------------------------------------

    @Test
    fun `preferences() returns subcollection path`() {
        assertEquals(
            "users/$userId/preferences",
            FirestoreCollections.preferences(userId).value,
        )
    }

    @Test
    fun `preference() returns document path`() {
        assertEquals(
            "users/$userId/preferences/$preferenceId",
            FirestoreCollections.preference(userId, preferenceId).value,
        )
    }

    // --- Deletions -----------------------------------------------------------

    @Test
    fun `deletions() returns subcollection path`() {
        assertEquals(
            "users/$userId/deletions",
            FirestoreCollections.deletions(userId).value,
        )
    }

    @Test
    fun `deletion() returns document path`() {
        assertEquals(
            "users/$userId/deletions/$deletionId",
            FirestoreCollections.deletion(userId, deletionId).value,
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
        val segments = routineHabitPath.value.split("/")
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
        assert(document.value.startsWith("${collection.value}/")) {
            "Document path should start with collection path + /"
        }
    }

    // --- Blank input validation ----------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `user with blank userId throws`() {
        FirestoreCollections.user("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `user with whitespace userId throws`() {
        FirestoreCollections.user("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `habits with blank userId throws`() {
        FirestoreCollections.habits("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `habits with whitespace userId throws`() {
        FirestoreCollections.habits("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `habit with blank userId throws`() {
        FirestoreCollections.habit("", habitId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `habit with blank habitId throws`() {
        FirestoreCollections.habit(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `habit with whitespace habitId throws`() {
        FirestoreCollections.habit(userId, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completions with blank userId throws`() {
        FirestoreCollections.completions("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completions with whitespace userId throws`() {
        FirestoreCollections.completions("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completion with blank userId throws`() {
        FirestoreCollections.completion("", completionId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completion with blank completionId throws`() {
        FirestoreCollections.completion(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completion with whitespace completionId throws`() {
        FirestoreCollections.completion(userId, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routines with blank userId throws`() {
        FirestoreCollections.routines("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routines with whitespace userId throws`() {
        FirestoreCollections.routines("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routine with blank userId throws`() {
        FirestoreCollections.routine("", routineId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routine with blank routineId throws`() {
        FirestoreCollections.routine(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routine with whitespace routineId throws`() {
        FirestoreCollections.routine(userId, "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineHabits with blank userId throws`() {
        FirestoreCollections.routineHabits("", routineId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineHabits with blank routineId throws`() {
        FirestoreCollections.routineHabits(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineHabit with blank userId throws`() {
        FirestoreCollections.routineHabit("", routineId, routineHabitId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineHabit with blank routineId throws`() {
        FirestoreCollections.routineHabit(userId, "", routineHabitId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineHabit with blank id throws`() {
        FirestoreCollections.routineHabit(userId, routineId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineVariants with blank userId throws`() {
        FirestoreCollections.routineVariants("", routineId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineVariants with blank routineId throws`() {
        FirestoreCollections.routineVariants(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineVariant with blank userId throws`() {
        FirestoreCollections.routineVariant("", routineId, variantId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineVariant with blank routineId throws`() {
        FirestoreCollections.routineVariant(userId, "", variantId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineVariant with blank id throws`() {
        FirestoreCollections.routineVariant(userId, routineId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineExecutions with blank userId throws`() {
        FirestoreCollections.routineExecutions("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineExecutions with whitespace userId throws`() {
        FirestoreCollections.routineExecutions("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineExecution with blank userId throws`() {
        FirestoreCollections.routineExecution("", executionId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routineExecution with blank id throws`() {
        FirestoreCollections.routineExecution(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recoverySessions with blank userId throws`() {
        FirestoreCollections.recoverySessions("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recoverySessions with whitespace userId throws`() {
        FirestoreCollections.recoverySessions("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recoverySession with blank userId throws`() {
        FirestoreCollections.recoverySession("", recoveryId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recoverySession with blank id throws`() {
        FirestoreCollections.recoverySession(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `preferences with blank userId throws`() {
        FirestoreCollections.preferences("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `preferences with whitespace userId throws`() {
        FirestoreCollections.preferences("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `preference with blank userId throws`() {
        FirestoreCollections.preference("", preferenceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `preference with blank id throws`() {
        FirestoreCollections.preference(userId, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deletions with blank userId throws`() {
        FirestoreCollections.deletions("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deletions with whitespace userId throws`() {
        FirestoreCollections.deletions("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deletion with blank userId throws`() {
        FirestoreCollections.deletion("", deletionId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deletion with blank id throws`() {
        FirestoreCollections.deletion(userId, "")
    }
}
