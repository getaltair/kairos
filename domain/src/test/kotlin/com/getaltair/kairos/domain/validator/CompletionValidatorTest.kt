package com.getaltair.kairos.domain.validator

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionValidatorTest {

    private val today = LocalDate.of(2025, 6, 15)
    private val habitId = UUID.randomUUID()

    /**
     * Builds a valid Completion that passes the entity init block.
     */
    private fun validCompletion(
        type: CompletionType = CompletionType.Full,
        partialPercent: Int? = null,
        date: LocalDate = today
    ) = Completion(
        habitId = habitId,
        date = date,
        type = type,
        partialPercent = partialPercent
    )

    // --- C-2: Partial completion percent constraints ---

    @Test
    fun `C-2 PARTIAL with null partialPercent returns error`() {
        // The init block forces partialPercent to null for Partial type (due to bug),
        // so the validator correctly rejects this as invalid
        val completion = validCompletion(type = CompletionType.Partial, partialPercent = null)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Error)
        assertEquals(
            "partialPercent must be in 1..99 for PARTIAL completions",
            (result as Result.Error).message
        )
    }

    @Test
    fun `C-2 PARTIAL with valid partialPercent passes`() {
        val completion = validCompletion(type = CompletionType.Partial, partialPercent = 50)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `C-2 PARTIAL with partialPercent 0 rejects at init block`() {
        // The init block enforces partialPercent in 1..99, so 0 is rejected
        // before the validator is ever reached.
        try {
            validCompletion(type = CompletionType.Partial, partialPercent = 0)
            assertTrue("Expected IllegalArgumentException from init block", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("partialPercent") == true)
        }
    }

    @Test
    fun `C-2 PARTIAL with partialPercent 100 rejects at init block`() {
        // The init block enforces partialPercent in 1..99, so 100 is rejected
        // before the validator is ever reached.
        try {
            validCompletion(type = CompletionType.Partial, partialPercent = 100)
            assertTrue("Expected IllegalArgumentException from init block", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("partialPercent") == true)
        }
    }

    @Test
    fun `C-2 FULL with non-null partialPercent rejects at init block`() {
        // The init block also prevents FULL type with non-null partialPercent
        // via the first require: `type == Partial || partialPercent == null`
        try {
            validCompletion(type = CompletionType.Full, partialPercent = 50)
            assertTrue("Expected IllegalArgumentException from init block", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("partialPercent") == true)
        }
    }

    @Test
    fun `C-2 FULL with null partialPercent passes validator`() {
        val completion = validCompletion(type = CompletionType.Full, partialPercent = null)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }

    // --- C-4: No future completions ---

    @Test
    fun `C-4 future date returns error`() {
        val futureDate = today.plusDays(1)
        val completion = validCompletion(date = futureDate)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Error)
        assertEquals("Completion date must not be in the future", (result as Result.Error).message)
    }

    // --- C-5: Limited backdating (7 days max) ---

    @Test
    fun `C-5 date older than 7 days returns error`() {
        val oldDate = today.minusDays(8)
        val completion = validCompletion(date = oldDate)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Error)
        assertEquals(
            "Completion date must be within the last 7 days",
            (result as Result.Error).message
        )
    }

    @Test
    fun `C-5 date exactly 7 days ago passes`() {
        val sevenDaysAgo = today.minusDays(7)
        val completion = validCompletion(date = sevenDaysAgo)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `C-5 date 6 days ago passes`() {
        val sixDaysAgo = today.minusDays(6)
        val completion = validCompletion(date = sixDaysAgo)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }

    // --- Valid completion ---

    @Test
    fun `valid FULL completion returns Success`() {
        val completion = validCompletion()
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid completion on today returns Success`() {
        val completion = validCompletion(date = today)
        val result = CompletionValidator.validate(completion, today)
        assertTrue(result is Result.Success)
    }
}
