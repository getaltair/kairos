package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineHabit
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineValidatorTest {

    private val routineId = UUID.randomUUID()

    private fun routineHabit(orderIndex: Int) = RoutineHabit(
        routineId = routineId,
        habitId = UUID.randomUUID(),
        orderIndex = orderIndex,
    )

    // --- validateCreate: name validation ---

    @Test
    fun `validateCreate rejects blank name`() {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val result = RoutineValidator.validateCreate("   ", habitIds)
        assertTrue(result is Result.Error)
        assertEquals(
            "Routine name must not be blank",
            (result as Result.Error).message
        )
    }

    @Test
    fun `validateCreate rejects empty name`() {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val result = RoutineValidator.validateCreate("", habitIds)
        assertTrue(result is Result.Error)
        assertEquals(
            "Routine name must not be blank",
            (result as Result.Error).message
        )
    }

    @Test
    fun `validateCreate rejects name longer than 50 characters`() {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val longName = "A".repeat(51)
        val result = RoutineValidator.validateCreate(longName, habitIds)
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("50 characters")
        )
    }

    @Test
    fun `validateCreate accepts name at exactly 50 characters`() {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val maxName = "A".repeat(50)
        val result = RoutineValidator.validateCreate(maxName, habitIds)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validateCreate accepts valid name of 1 character`() {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val result = RoutineValidator.validateCreate("M", habitIds)
        assertTrue(result is Result.Success)
    }

    // --- validateCreate: R-1 minimum habit count ---

    @Test
    fun `R-1 validateCreate rejects 0 habits`() {
        val result = RoutineValidator.validateCreate("Morning", emptyList())
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("at least 2 habits")
        )
    }

    @Test
    fun `R-1 validateCreate rejects 1 habit`() {
        val result = RoutineValidator.validateCreate(
            "Morning",
            listOf(UUID.randomUUID())
        )
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("at least 2 habits"))
        assertTrue(error.message.contains("got 1"))
    }

    @Test
    fun `R-1 validateCreate accepts exactly 2 habits`() {
        val result = RoutineValidator.validateCreate(
            "Morning",
            listOf(UUID.randomUUID(), UUID.randomUUID())
        )
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-1 validateCreate accepts 3 or more habits`() {
        val result = RoutineValidator.validateCreate(
            "Morning",
            listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )
        assertTrue(result is Result.Success)
    }

    // --- validateOrderIndices: R-2 sequential order ---

    @Test
    fun `R-2 validateOrderIndices accepts valid sequential indices`() {
        val habits = listOf(
            routineHabit(0),
            routineHabit(1),
            routineHabit(2),
        )
        val result = RoutineValidator.validateOrderIndices(habits)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-2 validateOrderIndices accepts empty list`() {
        val result = RoutineValidator.validateOrderIndices(emptyList())
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-2 validateOrderIndices rejects indices not starting at 0`() {
        val habits = listOf(
            routineHabit(1),
            routineHabit(2),
            routineHabit(3),
        )
        val result = RoutineValidator.validateOrderIndices(habits)
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("sequential starting at 0")
        )
    }

    @Test
    fun `R-2 validateOrderIndices rejects gap in sequence`() {
        val habits = listOf(
            routineHabit(0),
            routineHabit(2),
            routineHabit(3),
        )
        val result = RoutineValidator.validateOrderIndices(habits)
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("R-2")
        )
    }

    @Test
    fun `R-2 validateOrderIndices rejects duplicate indices`() {
        val habits = listOf(
            routineHabit(0),
            routineHabit(0),
            routineHabit(1),
        )
        val result = RoutineValidator.validateOrderIndices(habits)
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("R-2")
        )
    }

    @Test
    fun `R-2 validateOrderIndices accepts single habit at index 0`() {
        val habits = listOf(routineHabit(0))
        val result = RoutineValidator.validateOrderIndices(habits)
        assertTrue(result is Result.Success)
    }

    // --- validateDuration: R-4 positive duration ---

    @Test
    fun `R-4 validateDuration accepts null`() {
        val result = RoutineValidator.validateDuration(null)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-4 validateDuration accepts positive seconds`() {
        val result = RoutineValidator.validateDuration(120)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-4 validateDuration accepts 1 second`() {
        val result = RoutineValidator.validateDuration(1)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `R-4 validateDuration rejects 0 seconds`() {
        val result = RoutineValidator.validateDuration(0)
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("positive"))
        assertTrue(error.message.contains("R-4"))
    }

    @Test
    fun `R-4 validateDuration rejects negative seconds`() {
        val result = RoutineValidator.validateDuration(-5)
        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("positive")
        )
    }
}
