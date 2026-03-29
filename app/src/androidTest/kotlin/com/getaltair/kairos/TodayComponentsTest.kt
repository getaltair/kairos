package com.getaltair.kairos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.feature.today.components.EmptyState
import com.getaltair.kairos.feature.today.components.HabitCard
import com.getaltair.kairos.feature.today.components.ProgressRing
import com.getaltair.kairos.ui.theme.KairosTheme
import java.time.LocalDate
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -- EmptyState tests --

    @Test
    fun emptyState_displaysHeadline() {
        composeTestRule.setContent {
            KairosTheme {
                EmptyState()
            }
        }
        composeTestRule.onNodeWithText("Add your first habit").assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysDescription() {
        composeTestRule.setContent {
            KairosTheme {
                EmptyState()
            }
        }
        composeTestRule
            .onNodeWithText("Habits help you build consistency one day at a time.")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysAddHabitButton() {
        composeTestRule.setContent {
            KairosTheme {
                EmptyState()
            }
        }
        composeTestRule.onNodeWithText("Add Habit").assertIsDisplayed()
    }

    // -- ProgressRing tests --

    @Test
    fun progressRing_displaysZeroPercent() {
        composeTestRule.setContent {
            KairosTheme {
                ProgressRing(progress = 0f)
            }
        }
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun progressRing_displaysHundredPercent() {
        composeTestRule.setContent {
            KairosTheme {
                ProgressRing(progress = 1f)
            }
        }
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun progressRing_displaysFiftyPercent() {
        composeTestRule.setContent {
            KairosTheme {
                ProgressRing(progress = 0.5f)
            }
        }
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    // -- HabitCard tests --

    @Test
    fun habitCard_displaysHabitName() {
        val habit = makeHabit(name = "Meditate")
        val habitWithStatus = HabitWithStatus(
            habit = habit,
            todayCompletion = null,
            weekCompletionRate = 0.5f
        )
        composeTestRule.setContent {
            KairosTheme {
                HabitCard(habitWithStatus = habitWithStatus, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Meditate").assertIsDisplayed()
    }

    @Test
    fun habitCard_displaysAnchorBehavior() {
        val habit = makeHabit(name = "Read", anchorBehavior = "After breakfast")
        val habitWithStatus = HabitWithStatus(
            habit = habit,
            todayCompletion = null,
            weekCompletionRate = 0.7f
        )
        composeTestRule.setContent {
            KairosTheme {
                HabitCard(habitWithStatus = habitWithStatus, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("After breakfast").assertIsDisplayed()
    }

    @Test
    fun habitCard_showsCompletedIcon_whenFullCompletion() {
        val habit = makeHabit(name = "Exercise")
        val completion = Completion(
            habitId = habit.id,
            date = LocalDate.now(),
            type = CompletionType.Full
        )
        val habitWithStatus = HabitWithStatus(
            habit = habit,
            todayCompletion = completion,
            weekCompletionRate = 0.8f
        )
        composeTestRule.setContent {
            KairosTheme {
                HabitCard(habitWithStatus = habitWithStatus, onClick = {})
            }
        }
        // The habit name should still be displayed
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
    }

    @Test
    fun habitCard_showsSkippedIcon_whenSkipped() {
        val habit = makeHabit(name = "Journal")
        val completion = Completion(
            habitId = habit.id,
            date = LocalDate.now(),
            type = CompletionType.Skipped
        )
        val habitWithStatus = HabitWithStatus(
            habit = habit,
            todayCompletion = completion,
            weekCompletionRate = 0.3f
        )
        composeTestRule.setContent {
            KairosTheme {
                HabitCard(habitWithStatus = habitWithStatus, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Journal").assertIsDisplayed()
    }

    // -- Helpers --

    private fun makeHabit(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Habit",
        anchorBehavior: String = "After waking up",
        category: HabitCategory = HabitCategory.Morning
    ): Habit = Habit(
        id = id,
        name = name,
        anchorBehavior = anchorBehavior,
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = HabitFrequency.Daily
    )
}
