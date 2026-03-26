package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitValidatorTest {

    private fun validHabit(
        anchorBehavior: String = "After brushing teeth",
        allowPartialCompletion: Boolean = true,
        lapseThresholdDays: Int = 3,
        relapseThresholdDays: Int = 7,
        createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        pausedAt: Instant? = null,
        archivedAt: Instant? = null
    ) = Habit(
        name = "Meditate",
        anchorBehavior = anchorBehavior,
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        allowPartialCompletion = allowPartialCompletion,
        lapseThresholdDays = lapseThresholdDays,
        relapseThresholdDays = relapseThresholdDays,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pausedAt = pausedAt,
        archivedAt = archivedAt
    )

    @Test
    fun `H-1 blank anchorBehavior returns error`() {
        val habit = validHabit(anchorBehavior = "   ")
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals("anchorBehavior must not be blank", (result as Result.Error).message)
    }

    @Test
    fun `H-1 valid anchorBehavior passes`() {
        val habit = validHabit(anchorBehavior = "After morning coffee")
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `H-4 allowPartialCompletion false returns error`() {
        val habit = validHabit(allowPartialCompletion = false)
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals("allowPartialCompletion must be true", (result as Result.Error).message)
    }

    @Test
    fun `H-5 relapseThresholdDays less than or equal to lapseThresholdDays returns error`() {
        val habit = validHabit(lapseThresholdDays = 5, relapseThresholdDays = 5)
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("relapseThresholdDays"))
    }

    @Test
    fun `H-5 relapseThresholdDays less than lapseThresholdDays returns error`() {
        val habit = validHabit(lapseThresholdDays = 7, relapseThresholdDays = 3)
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("relapseThresholdDays"))
    }

    @Test
    fun `H-5 relapseThresholdDays greater than lapseThresholdDays passes`() {
        val habit = validHabit(lapseThresholdDays = 3, relapseThresholdDays = 7)
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `H-6 updatedAt before createdAt returns error`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-10T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-05T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals("createdAt must be <= updatedAt", (result as Result.Error).message)
    }

    @Test
    fun `H-6 valid timestamp ordering passes`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-10T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `H-6 equal createdAt and updatedAt passes`() {
        val timestamp = Instant.parse("2025-01-01T00:00:00Z")
        val habit = validHabit(createdAt = timestamp, updatedAt = timestamp)
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `fully valid habit returns Success`() {
        val habit = validHabit()
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `pausedAt before createdAt returns validation error`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-10T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-15T00:00:00Z"),
            pausedAt = Instant.parse("2025-01-05T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals("pausedAt must be >= createdAt", (result as Result.Error).message)
    }

    @Test
    fun `archivedAt before createdAt returns validation error`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-10T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-15T00:00:00Z"),
            archivedAt = Instant.parse("2025-01-05T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals("archivedAt must be >= createdAt", (result as Result.Error).message)
    }

    @Test
    fun `pausedAt after archivedAt returns validation error when both set`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-20T00:00:00Z"),
            pausedAt = Instant.parse("2025-01-15T00:00:00Z"),
            archivedAt = Instant.parse("2025-01-10T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Error)
        assertEquals(
            "pausedAt must be <= archivedAt when both are set",
            (result as Result.Error).message
        )
    }

    @Test
    fun `valid createdAt less than pausedAt less than archivedAt passes validation`() {
        val habit = validHabit(
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-20T00:00:00Z"),
            pausedAt = Instant.parse("2025-01-10T00:00:00Z"),
            archivedAt = Instant.parse("2025-01-15T00:00:00Z")
        )
        val result = HabitValidator.validate(habit)
        assertTrue(result is Result.Success)
    }
}
