package com.getaltair.kairos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the Create Habit wizard flow.
 * Uses the full activity since CreateHabitScreen depends on a ViewModel from Koin.
 * We navigate to CreateHabitScreen via the FAB on the TodayScreen.
 */
@RunWith(AndroidJUnit4::class)
class CreateHabitScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createHabitScreen_displaysNameStep_afterFabClick() {
        // The FAB has contentDescription "Add habit"
        composeTestRule.onNodeWithContentDescription("Add habit").performClick()

        // First step of the wizard should show "Name your habit"
        composeTestRule.onNodeWithText("Name your habit (1/4)").assertIsDisplayed()
    }

    @Test
    fun createHabitScreen_backButton_returnsToTodayScreen() {
        // Navigate to create habit
        composeTestRule.onNodeWithContentDescription("Add habit").performClick()
        composeTestRule.onNodeWithText("Name your habit (1/4)").assertIsDisplayed()

        // Press back via the navigation icon
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Should be back on the today screen -- FAB should be visible again
        composeTestRule.onNodeWithContentDescription("Add habit").assertIsDisplayed()
    }
}
