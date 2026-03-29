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
 * Tests for navigating to and displaying the Settings screen.
 * Uses the full activity since SettingsScreen depends on a ViewModel from Koin.
 */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsScreen_displaysAfterNavigation() {
        // TodayScreen has a settings icon button with contentDescription "Settings"
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Settings screen title bar shows "Settings"
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAccountSection() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysPreferencesSection() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSyncSection() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Sync").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSignInPrompt_whenNotSignedIn() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // When not signed in, the account section shows the sign-in prompt
        composeTestRule
            .onNodeWithText("Sign in to sync your habits across devices")
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSignInButton_whenNotSignedIn() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysNotificationsRow() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backNavigatesToTodayScreen() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Press back via the navigation icon
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Should be back on the today screen -- FAB should be visible
        composeTestRule.onNodeWithContentDescription("Add habit").assertIsDisplayed()
    }
}
