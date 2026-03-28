package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardStateTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun makeHabit(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Habit",
        category: HabitCategory = HabitCategory.Morning,
    ): Habit = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking",
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = HabitFrequency.Daily,
    )

    private fun makeCompletion(habitId: UUID, type: CompletionType = CompletionType.Full,): Completion = Completion(
        habitId = habitId,
        date = LocalDate.now(),
        type = type,
    )

    // -----------------------------------------------------------------------
    // departureHabits
    // -----------------------------------------------------------------------

    @Test
    fun departureHabits_returnsOnlyDepartureCategory() {
        val departure1 = makeHabit(name = "Keys", category = HabitCategory.Departure)
        val departure2 = makeHabit(name = "Wallet", category = HabitCategory.Departure)
        val morning = makeHabit(name = "Meditate", category = HabitCategory.Morning)

        val state = DashboardState(habits = listOf(departure1, departure2, morning))

        assertEquals(2, state.departureHabits.size)
        assertTrue(state.departureHabits.all { it.category == HabitCategory.Departure })
    }

    // -----------------------------------------------------------------------
    // habitsByCategory
    // -----------------------------------------------------------------------

    @Test
    fun habitsByCategory_excludesDepartureHabits() {
        val departure = makeHabit(name = "Keys", category = HabitCategory.Departure)
        val morning = makeHabit(name = "Meditate", category = HabitCategory.Morning)

        val state = DashboardState(habits = listOf(departure, morning))

        assertFalse(state.habitsByCategory.containsKey(HabitCategory.Departure))
    }

    @Test
    fun habitsByCategory_groupsByCategory() {
        val morning1 = makeHabit(name = "Meditate", category = HabitCategory.Morning)
        val morning2 = makeHabit(name = "Stretch", category = HabitCategory.Morning)
        val evening = makeHabit(name = "Journal", category = HabitCategory.Evening)
        val anytime = makeHabit(name = "Water", category = HabitCategory.Anytime)

        val state = DashboardState(habits = listOf(morning1, morning2, evening, anytime))

        assertEquals(3, state.habitsByCategory.size)
        assertEquals(2, state.habitsByCategory[HabitCategory.Morning]?.size)
        assertEquals(1, state.habitsByCategory[HabitCategory.Evening]?.size)
        assertEquals(1, state.habitsByCategory[HabitCategory.Anytime]?.size)
    }

    // -----------------------------------------------------------------------
    // completedHabitIds
    // -----------------------------------------------------------------------

    @Test
    fun completedHabitIds_returnsCorrectUUIDs() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val c1 = makeCompletion(habitId = id1)
        val c2 = makeCompletion(habitId = id2)

        val state = DashboardState(completions = listOf(c1, c2))

        assertEquals(setOf(id1, id2), state.completedHabitIds)
    }

    @Test
    fun completedHabitIds_deduplicatesMultipleCompletions() {
        val id1 = UUID.randomUUID()
        val c1 = makeCompletion(habitId = id1)
        val c2 = makeCompletion(habitId = id1)

        val state = DashboardState(completions = listOf(c1, c2))

        assertEquals(1, state.completedHabitIds.size)
        assertTrue(state.completedHabitIds.contains(id1))
    }

    // -----------------------------------------------------------------------
    // comingUpHabits
    // -----------------------------------------------------------------------

    @Test
    fun comingUpHabits_excludesCompletedHabits() {
        val completedId = UUID.randomUUID()
        val pendingId = UUID.randomUUID()
        val completed = makeHabit(id = completedId, name = "Done", category = HabitCategory.Morning)
        val pending = makeHabit(id = pendingId, name = "Pending", category = HabitCategory.Morning)
        val completion = makeCompletion(habitId = completedId)

        val state = DashboardState(
            habits = listOf(completed, pending),
            completions = listOf(completion),
        )

        assertEquals(1, state.comingUpHabits.size)
        assertEquals(pendingId, state.comingUpHabits[0].id)
    }

    @Test
    fun comingUpHabits_excludesDepartureHabits() {
        val departure = makeHabit(name = "Keys", category = HabitCategory.Departure)
        val morning = makeHabit(name = "Meditate", category = HabitCategory.Morning)

        val state = DashboardState(habits = listOf(departure, morning))

        assertEquals(1, state.comingUpHabits.size)
        assertEquals("Meditate", state.comingUpHabits[0].name)
    }

    @Test
    fun comingUpHabits_limitsToThree() {
        val habits = (1..5).map {
            makeHabit(name = "Habit $it", category = HabitCategory.Morning)
        }

        val state = DashboardState(habits = habits)

        assertEquals(3, state.comingUpHabits.size)
    }

    @Test
    fun comingUpHabits_emptyWhenAllCompleted() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val h1 = makeHabit(id = id1, name = "One", category = HabitCategory.Morning)
        val h2 = makeHabit(id = id2, name = "Two", category = HabitCategory.Evening)
        val c1 = makeCompletion(habitId = id1)
        val c2 = makeCompletion(habitId = id2)

        val state = DashboardState(
            habits = listOf(h1, h2),
            completions = listOf(c1, c2),
        )

        assertTrue(state.comingUpHabits.isEmpty())
    }

    // -----------------------------------------------------------------------
    // totalHabits
    // -----------------------------------------------------------------------

    @Test
    fun totalHabits_equalsHabitsListSize() {
        val habits = listOf(
            makeHabit(name = "A"),
            makeHabit(name = "B"),
            makeHabit(name = "C"),
        )

        val state = DashboardState(habits = habits)

        assertEquals(3, state.totalHabits)
    }

    // -----------------------------------------------------------------------
    // completedCount
    // -----------------------------------------------------------------------

    @Test
    fun completedCount_countsUniqueHabits_notRawCompletions() {
        val id1 = UUID.randomUUID()
        val c1 = makeCompletion(habitId = id1)
        val c2 = makeCompletion(habitId = id1)

        val state = DashboardState(completions = listOf(c1, c2))

        assertEquals(1, state.completedCount)
    }

    // -----------------------------------------------------------------------
    // isStale
    // -----------------------------------------------------------------------

    @Test
    fun isStale_trueWhenLastUpdatedIsNull() {
        val state = DashboardState(lastUpdated = null)

        assertTrue(state.isStale)
    }

    @Test
    fun isStale_falseWithinThreshold() {
        val recentUpdate = Instant.now().minusSeconds(30)

        val state = DashboardState(lastUpdated = recentUpdate)

        assertFalse(state.isStale)
    }

    @Test
    fun isStale_trueAfterThreshold() {
        // The threshold is 2 minutes; set lastUpdated to 3 minutes ago
        val oldUpdate = Instant.now().minus(Duration.ofMinutes(3))

        val state = DashboardState(lastUpdated = oldUpdate)

        assertTrue(state.isStale)
    }

    // -----------------------------------------------------------------------
    // Default state
    // -----------------------------------------------------------------------

    @Test
    fun defaultState_hasCorrectInitialValues() {
        val state = DashboardState()

        assertTrue(state.habits.isEmpty())
        assertTrue(state.completions.isEmpty())
        assertEquals(ConnectionStatus.Connecting, state.connectionStatus)
        assertEquals(null, state.lastUpdated)
        assertEquals(0, state.totalHabits)
        assertEquals(0, state.completedCount)
        assertTrue(state.completedHabitIds.isEmpty())
        assertTrue(state.comingUpHabits.isEmpty())
        assertTrue(state.departureHabits.isEmpty())
        assertTrue(state.habitsByCategory.isEmpty())
        assertTrue(state.isStale)
    }

    // -----------------------------------------------------------------------
    // displayMode / isStandby
    // -----------------------------------------------------------------------

    @Test
    fun displayMode_defaultIsActive() {
        val state = DashboardState()

        assertEquals(DisplayMode.Active, state.displayMode)
        assertFalse(state.isStandby)
    }

    @Test
    fun displayMode_standbyMakesIsStandbyTrue() {
        val state = DashboardState(displayMode = DisplayMode.Standby)

        assertEquals(DisplayMode.Standby, state.displayMode)
        assertTrue(state.isStandby)
    }

    @Test
    fun displayMode_activeAfterStandbyResetsIsStandby() {
        val standby = DashboardState(displayMode = DisplayMode.Standby)
        assertTrue(standby.isStandby)

        val active = standby.copy(displayMode = DisplayMode.Active)
        assertFalse(active.isStandby)
    }

    @Test
    fun completeHabit_duplicateCompletionDoesNotAddTwice() {
        val habitId = UUID.randomUUID()
        val c1 = makeCompletion(habitId = habitId)

        val state = DashboardState(completions = listOf(c1))

        // Verify the habit is already marked complete
        assertTrue(state.completedHabitIds.contains(habitId))
        assertEquals(1, state.completedHabitIds.size)

        // Adding a second completion for the same habit should still yield 1 unique ID
        val c2 = makeCompletion(habitId = habitId)
        val state2 = state.copy(completions = state.completions + c2)
        assertEquals(1, state2.completedHabitIds.size)
    }
}
