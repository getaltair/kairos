package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.dashboard.config.DashboardConfig
import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [DashboardStateHolder] covering optimistic updates, rollback on
 * Firestore failure, duplicate-call guards, and display-mode switching.
 *
 * Uses a [FakeFirebaseAdminClient] that extends [FirebaseAdminClient] (now
 * `open`) and overrides [writeCompletion] so no real Firebase connection is
 * needed.  [DashboardStateHolder.start] is intentionally NOT called -- these
 * tests exercise the public action methods which do not require active
 * Firestore listeners.
 */
class DashboardStateHolderTest {

    // -- test doubles --------------------------------------------------------

    private class FakeFirebaseAdminClient(config: DashboardConfig,) : FirebaseAdminClient(config) {
        var writeResult: Result<Unit> = Result.success(Unit)
        val writtenCompletions = mutableListOf<Triple<String, String, Map<String, Any?>>>()

        override fun writeCompletion(userId: String, completionId: String, data: Map<String, Any?>,): Result<Unit> {
            writtenCompletions.add(Triple(userId, completionId, data))
            return writeResult
        }
    }

    // -- fixtures ------------------------------------------------------------

    private val testConfig = DashboardConfig(
        firebaseServiceAccountPath = "/tmp/fake-sa.json",
        firebaseUserId = "test-user",
        fullscreen = false,
        width = 800,
        height = 600,
        serverPort = 9999,
    )
    private val userId = "test-user"

    private lateinit var fakeClient: FakeFirebaseAdminClient
    private lateinit var holder: DashboardStateHolder

    @Before
    fun setUp() {
        fakeClient = FakeFirebaseAdminClient(testConfig)
        holder = DashboardStateHolder(fakeClient, userId)
    }

    @After
    fun tearDown() {
        holder.close()
    }

    // -- completeHabit -------------------------------------------------------

    @Test
    fun completeHabit_optimisticUpdate_appearsInStateImmediately() {
        val habitId = UUID.randomUUID()

        holder.completeHabit(habitId)

        // The optimistic update inside _state.update is synchronous, so the
        // completion should be visible right away.
        assertTrue(
            "Habit should appear in completedHabitIds immediately after completeHabit",
            holder.state.value.completedHabitIds.contains(habitId),
        )
    }

    @Test
    fun completeHabit_firestoreFailure_rollsBackOptimisticUpdate() {
        val habitId = UUID.randomUUID()
        fakeClient.writeResult = Result.failure(RuntimeException("Firestore unavailable"))

        holder.completeHabit(habitId)

        // Optimistic update is present immediately
        assertTrue(holder.state.value.completedHabitIds.contains(habitId))

        // The write coroutine runs on Dispatchers.IO; give it time to execute
        // and process the failure rollback.
        Thread.sleep(500)

        assertFalse(
            "Habit should be rolled back from completedHabitIds after Firestore failure",
            holder.state.value.completedHabitIds.contains(habitId),
        )
    }

    @Test
    fun completeHabit_duplicateCall_isNoOp() {
        val habitId = UUID.randomUUID()

        holder.completeHabit(habitId)
        holder.completeHabit(habitId)

        // Only one completion should exist in state
        val completionsForHabit = holder.state.value.completions.filter { it.habitId == habitId }
        assertEquals(
            "Duplicate completeHabit should not add a second completion",
            1,
            completionsForHabit.size,
        )

        // Allow the first write to land, then verify only one write was dispatched
        Thread.sleep(300)
        assertEquals(
            "Only one Firestore write should have been dispatched",
            1,
            fakeClient.writtenCompletions.size,
        )
    }

    // -- setDisplayMode ------------------------------------------------------

    @Test
    fun setDisplayMode_updatesState() {
        holder.setDisplayMode(DisplayMode.Standby)

        assertEquals(DisplayMode.Standby, holder.state.value.displayMode)
    }

    @Test
    fun setDisplayMode_backToActive() {
        holder.setDisplayMode(DisplayMode.Standby)
        assertEquals(DisplayMode.Standby, holder.state.value.displayMode)

        holder.setDisplayMode(DisplayMode.Active)
        assertEquals(DisplayMode.Active, holder.state.value.displayMode)
    }
}
