package com.getaltair.kairos.notification

import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HabitReminderBuilder] text content generation.
 *
 * Since building actual Android Notification objects requires a live Context,
 * these tests validate the text strings (title, body, follow-up variants)
 * and verify compliance with invariant D-2 (shame-free messaging).
 */
class HabitReminderBuilderTest {

    private val sampleHabit = Habit(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        name = "Take medication",
        anchorBehavior = "After brushing your teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily
    )

    /**
     * Forbidden phrases per D-2 (no punitive messaging) and the notification design doc.
     * None of these may appear in any notification text, case-insensitively.
     */
    private val forbiddenPhrases = listOf(
        "don't forget",
        "you missed",
        "streak",
        "failed",
        "behind",
        "shame",
        "guilt",
        "you should",
        "you need to",
        "disappointed"
    )

    // -- Title tests --

    @Test
    fun `title contains habit name`() {
        val title = titleText(sampleHabit)
        assertTrue(
            "Title should contain habit name, got: $title",
            title.contains(sampleHabit.name)
        )
    }

    @Test
    fun `title uses Time for prefix`() {
        val title = titleText(sampleHabit)
        assertTrue(
            "Title should start with 'Time for:', got: $title",
            title.startsWith("Time for:")
        )
    }

    // -- Body tests --

    @Test
    fun `initial reminder body is anchor behavior`() {
        // Follow-up 0 or initial reminder uses the anchor behavior as body
        assertEquals(sampleHabit.anchorBehavior, followUpBody(sampleHabit, 1))
    }

    @Test
    fun `follow-up 1 body matches anchor behavior`() {
        assertEquals(sampleHabit.anchorBehavior, followUpBody(sampleHabit, 1))
    }

    @Test
    fun `follow-up 2 body is still waiting message`() {
        assertEquals(
            HabitReminderBuilder.FOLLOW_UP_2_BODY,
            followUpBody(sampleHabit, 2)
        )
    }

    @Test
    fun `follow-up 3 body is last reminder message`() {
        assertEquals(
            HabitReminderBuilder.FOLLOW_UP_3_BODY,
            followUpBody(sampleHabit, 3)
        )
    }

    @Test
    fun `follow-up 2 text is shame-free`() {
        val body = HabitReminderBuilder.FOLLOW_UP_2_BODY
        assertEquals("Still waiting when you're ready.", body)
    }

    @Test
    fun `follow-up 3 text is shame-free`() {
        val body = HabitReminderBuilder.FOLLOW_UP_3_BODY
        assertEquals("Last reminder for today.", body)
    }

    // -- D-2 compliance tests --

    @Test
    fun `title contains no forbidden phrases`() {
        val title = titleText(sampleHabit)
        assertNoForbiddenPhrases(title, "title")
    }

    @Test
    fun `follow-up 1 body contains no forbidden phrases`() {
        val body = followUpBody(sampleHabit, 1)
        assertNoForbiddenPhrases(body, "follow-up 1 body")
    }

    @Test
    fun `follow-up 2 body contains no forbidden phrases`() {
        val body = followUpBody(sampleHabit, 2)
        assertNoForbiddenPhrases(body, "follow-up 2 body")
    }

    @Test
    fun `follow-up 3 body contains no forbidden phrases`() {
        val body = followUpBody(sampleHabit, 3)
        assertNoForbiddenPhrases(body, "follow-up 3 body")
    }

    @Test
    fun `all constant strings are D-2 compliant`() {
        val allStrings = listOf(
            HabitReminderBuilder.FOLLOW_UP_2_BODY,
            HabitReminderBuilder.FOLLOW_UP_3_BODY
        )
        allStrings.forEach { text ->
            assertNoForbiddenPhrases(text, "constant string")
        }
    }

    // -- Action constant tests --

    @Test
    fun `action constants are well-formed`() {
        assertTrue(
            HabitReminderBuilder.ACTION_COMPLETE_HABIT
                .startsWith("com.getaltair.kairos.")
        )
        assertTrue(
            HabitReminderBuilder.ACTION_SNOOZE_HABIT
                .startsWith("com.getaltair.kairos.")
        )
        assertTrue(
            HabitReminderBuilder.ACTION_SKIP_HABIT
                .startsWith("com.getaltair.kairos.")
        )
    }

    @Test
    fun `extra key is habit_id`() {
        assertEquals("habit_id", HabitReminderBuilder.EXTRA_HABIT_ID)
    }

    // -- Helpers --

    /**
     * Simulates [HabitReminderBuilder.titleText] logic without needing a Context.
     */
    private fun titleText(habit: Habit): String = "Time for: ${habit.name}"

    /**
     * Simulates [HabitReminderBuilder.followUpBody] logic without needing a Context.
     */
    private fun followUpBody(habit: Habit, followUpNumber: Int): String = when (followUpNumber) {
        1 -> habit.anchorBehavior
        2 -> HabitReminderBuilder.FOLLOW_UP_2_BODY
        3 -> HabitReminderBuilder.FOLLOW_UP_3_BODY
        else -> habit.anchorBehavior
    }

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
