package com.getaltair.kairos.notification

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RecoveryNotificationBuilder] text content generation.
 *
 * Uses an actual builder instance with a mocked Context.
 * Verifies compliance with FR-6 (shame-free messaging):
 * no "streak", "broke", "failed", "failure", "missed", "try harder",
 * "give up", "should have".
 */
class RecoveryNotificationBuilderTest {

    private val mockContext: Context = mockk(relaxed = true) {
        every { applicationInfo } returns ApplicationInfo()
        every { packageName } returns "com.getaltair.kairos"
        every { packageManager } returns mockk(relaxed = true)
    }
    private val builder = RecoveryNotificationBuilder(mockContext)

    /**
     * Forbidden phrases per FR-6 and the PRD messaging reference.
     * None of these may appear in any notification text, case-insensitively.
     */
    private val forbiddenPhrases = listOf(
        "streak",
        "broke",
        "failed",
        "failure",
        "missed",
        "try harder",
        "give up",
        "should have",
        "you need to",
        "disappointed",
        "shame",
        "guilt",
        "behind"
    )

    // -- Lapse notification tests --

    @Test
    fun `lapse title contains habit name`() {
        val title = builder.lapseTitle("Take medication")
        assertTrue(
            "Title should contain habit name, got: $title",
            title.contains("Take medication")
        )
    }

    @Test
    fun `lapse title uses Still here for prefix`() {
        val title = builder.lapseTitle("Exercise")
        assertTrue(
            "Title should start with 'Still here for', got: $title",
            title.startsWith("Still here for")
        )
    }

    @Test
    fun `lapse body short for 3 days`() {
        val body = builder.lapseBody(3)
        assertEquals(RecoveryNotificationBuilder.LAPSE_BODY_SHORT, body)
    }

    @Test
    fun `lapse body long for 4+ days`() {
        val body = builder.lapseBody(5)
        assertEquals(RecoveryNotificationBuilder.LAPSE_BODY_LONG, body)
    }

    // -- Relapse notification tests --

    @Test
    fun `relapse title contains habit name`() {
        val title = builder.relapseTitle("Meditation")
        assertTrue(
            "Title should contain habit name, got: $title",
            title.contains("Meditation")
        )
    }

    @Test
    fun `relapse body is supportive`() {
        assertEquals(RecoveryNotificationBuilder.RELAPSE_BODY, "It has been a little while. Ready for a fresh start?")
    }

    // -- Fresh start notification tests --

    @Test
    fun `fresh start title is correct`() {
        assertEquals("A fresh start awaits", RecoveryNotificationBuilder.FRESH_START_TITLE)
    }

    @Test
    fun `fresh start body singular for 1 habit`() {
        val body = builder.freshStartBody(1)
        assertEquals(RecoveryNotificationBuilder.FRESH_START_BODY_SINGLE, body)
    }

    @Test
    fun `fresh start body plural for multiple habits`() {
        val body = builder.freshStartBody(3)
        assertTrue(
            "Body should mention count, got: $body",
            body.contains("3")
        )
        assertTrue(
            "Body should mention habits, got: $body",
            body.contains("habits")
        )
    }

    // -- FR-6 compliance: all constant strings --

    @Test
    fun `lapse body short contains no forbidden phrases`() {
        assertNoForbiddenPhrases(RecoveryNotificationBuilder.LAPSE_BODY_SHORT, "LAPSE_BODY_SHORT")
    }

    @Test
    fun `lapse body long contains no forbidden phrases`() {
        assertNoForbiddenPhrases(RecoveryNotificationBuilder.LAPSE_BODY_LONG, "LAPSE_BODY_LONG")
    }

    @Test
    fun `relapse body contains no forbidden phrases`() {
        assertNoForbiddenPhrases(RecoveryNotificationBuilder.RELAPSE_BODY, "RELAPSE_BODY")
    }

    @Test
    fun `fresh start title contains no forbidden phrases`() {
        assertNoForbiddenPhrases(RecoveryNotificationBuilder.FRESH_START_TITLE, "FRESH_START_TITLE")
    }

    @Test
    fun `fresh start body single contains no forbidden phrases`() {
        assertNoForbiddenPhrases(RecoveryNotificationBuilder.FRESH_START_BODY_SINGLE, "FRESH_START_BODY_SINGLE")
    }

    @Test
    fun `lapse title contains no forbidden phrases`() {
        assertNoForbiddenPhrases(builder.lapseTitle("Read a book"), "lapse title")
    }

    @Test
    fun `relapse title contains no forbidden phrases`() {
        assertNoForbiddenPhrases(builder.relapseTitle("Exercise"), "relapse title")
    }

    @Test
    fun `fresh start body plural contains no forbidden phrases`() {
        assertNoForbiddenPhrases(builder.freshStartBody(5), "fresh start body plural")
    }

    @Test
    fun `all constant strings are FR-6 compliant`() {
        val allStrings = listOf(
            RecoveryNotificationBuilder.LAPSE_BODY_SHORT,
            RecoveryNotificationBuilder.LAPSE_BODY_LONG,
            RecoveryNotificationBuilder.RELAPSE_BODY,
            RecoveryNotificationBuilder.FRESH_START_TITLE,
            RecoveryNotificationBuilder.FRESH_START_BODY_SINGLE
        )
        allStrings.forEach { text ->
            assertNoForbiddenPhrases(text, "constant string")
        }
    }

    // -- Helpers --

    private fun assertNoForbiddenPhrases(text: String, label: String) {
        val lower = text.lowercase()
        forbiddenPhrases.forEach { phrase ->
            assertFalse(
                "$label contains forbidden phrase '$phrase': $text",
                lower.contains(phrase)
            )
        }
    }
}
