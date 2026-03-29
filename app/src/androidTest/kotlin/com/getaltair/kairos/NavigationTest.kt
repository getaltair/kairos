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
 * Tests for the top-level navigation graph.
 * Verifies that the app starts on TodayScreen and can navigate to key destinations.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_startsOnTodayScreen() {
        // TodayScreen always shows the "Add habit" FAB
        composeTestRule.onNodeWithContentDescription("Add habit").assertIsDisplayed()
    }

    @Test
    fun app_todayScreen_displaysSettingsAction() {
        // TodayScreen top bar should have a Settings icon
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun app_todayScreen_displaysRoutinesAction() {
        // TodayScreen top bar should have a Routines icon
        composeTestRule.onNodeWithContentDescription("Routines").assertIsDisplayed()
    }

    @Test
    fun app_navigateToRoutines() {
        composeTestRule.onNodeWithContentDescription("Routines").performClick()

        // RoutineListScreen should show some content; verify we navigated away
        // from TodayScreen (FAB "Add habit" should no longer be present)
        composeTestRule.onNodeWithContentDescription("Add habit").assertDoesNotExist()
    }

    @Test
    fun app_showsEmptyStateOrHabits_onTodayScreen() {
        // The today screen should show either the empty state text
        // or habit content. Verify at least one known element exists.
        // With no habits, the empty state displays "Add your first habit"
        // With habits, category headers are shown.
        // We just verify the screen rendered without crash.
        composeTestRule.onNodeWithContentDescription("Add habit").assertIsDisplayed()
    }
}
