package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitIsDueTodayTest {

    private fun habitWith(frequency: HabitFrequency, activeDays: Set<DayOfWeek>? = null) = Habit(
        name = "Test Habit",
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = frequency,
        activeDays = activeDays
    )

    // -- Daily frequency --

    @Test
    fun `Daily habit is due on a weekday`() {
        val monday = LocalDate.of(2026, 3, 23) // Monday
        assertTrue(habitWith(HabitFrequency.Daily).isDueToday(monday))
    }

    @Test
    fun `Daily habit is due on a weekend`() {
        val saturday = LocalDate.of(2026, 3, 28) // Saturday
        assertTrue(habitWith(HabitFrequency.Daily).isDueToday(saturday))
    }

    // -- Weekdays frequency --

    @Test
    fun `Weekdays habit is due on Monday`() {
        val monday = LocalDate.of(2026, 3, 23)
        assertTrue(habitWith(HabitFrequency.Weekdays).isDueToday(monday))
    }

    @Test
    fun `Weekdays habit is due on Tuesday`() {
        val tuesday = LocalDate.of(2026, 3, 24)
        assertTrue(habitWith(HabitFrequency.Weekdays).isDueToday(tuesday))
    }

    @Test
    fun `Weekdays habit is due on Wednesday`() {
        val wednesday = LocalDate.of(2026, 3, 25)
        assertTrue(habitWith(HabitFrequency.Weekdays).isDueToday(wednesday))
    }

    @Test
    fun `Weekdays habit is due on Thursday`() {
        val thursday = LocalDate.of(2026, 3, 26)
        assertTrue(habitWith(HabitFrequency.Weekdays).isDueToday(thursday))
    }

    @Test
    fun `Weekdays habit is due on Friday`() {
        val friday = LocalDate.of(2026, 3, 27)
        assertTrue(habitWith(HabitFrequency.Weekdays).isDueToday(friday))
    }

    @Test
    fun `Weekdays habit is not due on Saturday`() {
        val saturday = LocalDate.of(2026, 3, 28)
        assertFalse(habitWith(HabitFrequency.Weekdays).isDueToday(saturday))
    }

    @Test
    fun `Weekdays habit is not due on Sunday`() {
        val sunday = LocalDate.of(2026, 3, 29)
        assertFalse(habitWith(HabitFrequency.Weekdays).isDueToday(sunday))
    }

    // -- Weekends frequency --

    @Test
    fun `Weekends habit is due on Saturday`() {
        val saturday = LocalDate.of(2026, 3, 28)
        assertTrue(habitWith(HabitFrequency.Weekends).isDueToday(saturday))
    }

    @Test
    fun `Weekends habit is due on Sunday`() {
        val sunday = LocalDate.of(2026, 3, 29)
        assertTrue(habitWith(HabitFrequency.Weekends).isDueToday(sunday))
    }

    @Test
    fun `Weekends habit is not due on Monday`() {
        val monday = LocalDate.of(2026, 3, 23)
        assertFalse(habitWith(HabitFrequency.Weekends).isDueToday(monday))
    }

    @Test
    fun `Weekends habit is not due on Friday`() {
        val friday = LocalDate.of(2026, 3, 27)
        assertFalse(habitWith(HabitFrequency.Weekends).isDueToday(friday))
    }

    // -- Custom frequency --

    @Test
    fun `Custom habit is due on a matching day`() {
        val habit = habitWith(
            frequency = HabitFrequency.Custom,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val wednesday = LocalDate.of(2026, 3, 25)
        assertTrue(habit.isDueToday(wednesday))
    }

    @Test
    fun `Custom habit is not due on a non-matching day`() {
        val habit = habitWith(
            frequency = HabitFrequency.Custom,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val tuesday = LocalDate.of(2026, 3, 24)
        assertFalse(habit.isDueToday(tuesday))
    }

    @Test
    fun `Custom habit with null activeDays is never due`() {
        val habit = habitWith(
            frequency = HabitFrequency.Custom,
            activeDays = null
        )
        val monday = LocalDate.of(2026, 3, 23)
        assertFalse(habit.isDueToday(monday))
    }

    @Test
    fun `Custom habit with empty activeDays is never due`() {
        val habit = habitWith(
            frequency = HabitFrequency.Custom,
            activeDays = emptySet()
        )
        val monday = LocalDate.of(2026, 3, 23)
        assertFalse(habit.isDueToday(monday))
    }

    // -- Weekdays and Weekends do not rely on activeDays --

    @Test
    fun `Weekdays habit is due even when activeDays is null`() {
        val habit = habitWith(
            frequency = HabitFrequency.Weekdays,
            activeDays = null
        )
        val wednesday = LocalDate.of(2026, 3, 25)
        assertTrue(habit.isDueToday(wednesday))
    }

    @Test
    fun `Weekends habit is due even when activeDays is null`() {
        val habit = habitWith(
            frequency = HabitFrequency.Weekends,
            activeDays = null
        )
        val saturday = LocalDate.of(2026, 3, 28)
        assertTrue(habit.isDueToday(saturday))
    }
}
