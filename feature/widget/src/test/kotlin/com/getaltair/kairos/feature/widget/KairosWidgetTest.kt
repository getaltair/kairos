package com.getaltair.kairos.feature.widget

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.HabitWithStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KairosWidgetTest {

    private val fixedInstant = Instant.parse("2025-01-01T00:00:00Z")

    private fun makeHabit(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Habit",
        category: HabitCategory = HabitCategory.Morning
    ) = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = HabitFrequency.Daily,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun makeCompletion(habitId: UUID, type: CompletionType = CompletionType.Full) = Completion(
        habitId = habitId,
        date = LocalDate.now(),
        type = type,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun makeHabitWithStatus(
        habit: Habit = makeHabit(),
        todayCompletion: Completion? = null,
        weekCompletionRate: Float = 0.5f
    ) = HabitWithStatus(
        habit = habit,
        todayCompletion = todayCompletion,
        weekCompletionRate = weekCompletionRate
    )

    // -- computeProgress tests -----------------------------------------------

    @Test
    fun `progress calculation returns correct fraction`() {
        assertEquals(0.6f, computeProgress(3, 5), 0.001f)
    }

    @Test
    fun `progress calculation returns zero for zero total`() {
        assertEquals(0f, computeProgress(0, 0), 0.001f)
    }

    @Test
    fun `progress calculation returns 1 when all completed`() {
        assertEquals(1f, computeProgress(5, 5), 0.001f)
    }

    // -- countCompleted tests ------------------------------------------------

    @Test
    fun `empty habit list maps to zero completed`() {
        assertEquals(0, countCompleted(emptyList()))
    }

    @Test
    fun `countCompleted counts only Full completions`() {
        val habit1Id = UUID.randomUUID()
        val habit2Id = UUID.randomUUID()
        val habit3Id = UUID.randomUUID()
        val habits = listOf(
            makeHabitWithStatus(
                habit = makeHabit(id = habit1Id, name = "H1"),
                todayCompletion = makeCompletion(habitId = habit1Id, type = CompletionType.Full)
            ),
            makeHabitWithStatus(
                habit = makeHabit(id = habit2Id, name = "H2"),
                todayCompletion = makeCompletion(habitId = habit2Id, type = CompletionType.Partial)
            ),
            makeHabitWithStatus(
                habit = makeHabit(id = habit3Id, name = "H3"),
                todayCompletion = null
            )
        )
        assertEquals(1, countCompleted(habits))
    }

    @Test
    fun `all completed habits maps to all-done state`() {
        val habit1Id = UUID.randomUUID()
        val habit2Id = UUID.randomUUID()
        val habits = listOf(
            makeHabitWithStatus(
                habit = makeHabit(id = habit1Id, name = "H1"),
                todayCompletion = makeCompletion(habitId = habit1Id, type = CompletionType.Full)
            ),
            makeHabitWithStatus(
                habit = makeHabit(id = habit2Id, name = "H2"),
                todayCompletion = makeCompletion(habitId = habit2Id, type = CompletionType.Full)
            )
        )
        val completed = countCompleted(habits)
        val total = habits.size
        assertTrue(total > 0 && completed == total)
    }

    // -- sortByCategory tests ------------------------------------------------

    @Test
    fun `habits are ordered by category Morning then Afternoon then Evening then Anytime`() {
        val habits = listOf(
            makeHabitWithStatus(habit = makeHabit(name = "Anytime", category = HabitCategory.Anytime)),
            makeHabitWithStatus(habit = makeHabit(name = "Evening", category = HabitCategory.Evening)),
            makeHabitWithStatus(habit = makeHabit(name = "Morning", category = HabitCategory.Morning)),
            makeHabitWithStatus(habit = makeHabit(name = "Afternoon", category = HabitCategory.Afternoon))
        )

        val sorted = sortByCategory(habits)
        assertEquals("Morning", sorted[0].habit.name)
        assertEquals("Afternoon", sorted[1].habit.name)
        assertEquals("Evening", sorted[2].habit.name)
        assertEquals("Anytime", sorted[3].habit.name)
    }

    @Test
    fun `sortByCategory preserves order within same category`() {
        val habits = listOf(
            makeHabitWithStatus(habit = makeHabit(name = "B Morning", category = HabitCategory.Morning)),
            makeHabitWithStatus(habit = makeHabit(name = "A Morning", category = HabitCategory.Morning))
        )

        val sorted = sortByCategory(habits)
        assertEquals("B Morning", sorted[0].habit.name)
        assertEquals("A Morning", sorted[1].habit.name)
    }

    // -- max 5 habits test ---------------------------------------------------

    @Test
    fun `max 5 habits are shown`() {
        val habits = (1..8).map { i ->
            makeHabitWithStatus(habit = makeHabit(name = "Habit $i"))
        }
        val display = sortByCategory(habits).take(MAX_WIDGET_HABITS)
        assertEquals(5, display.size)
    }

    // -- statusIcon tests ----------------------------------------------------

    @Test
    fun `statusIcon returns correct icon for each completion type`() {
        assertEquals("\u2713", statusIcon(CompletionType.Full))
        assertEquals("\u25D1", statusIcon(CompletionType.Partial))
        assertEquals("\u2298", statusIcon(CompletionType.Skipped))
        assertEquals("\u2717", statusIcon(CompletionType.Missed))
        assertEquals("\u25CB", statusIcon(null))
    }

    // -- categoryOrder tests -------------------------------------------------

    @Test
    fun `categoryOrder returns correct order values`() {
        assertEquals(0, categoryOrder(HabitCategory.Morning))
        assertEquals(1, categoryOrder(HabitCategory.Afternoon))
        assertEquals(2, categoryOrder(HabitCategory.Evening))
        assertEquals(3, categoryOrder(HabitCategory.Anytime))
        assertEquals(4, categoryOrder(HabitCategory.Departure))
    }
}
