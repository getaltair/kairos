package com.getaltair.kairos.feature.settings

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.UserPreferences
import com.getaltair.kairos.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private val preferencesRepository: PreferencesRepository = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val samplePrefs = UserPreferences(
        notificationEnabled = true,
        quietHoursEnabled = true,
        quietHoursStart = LocalTime.of(22, 0),
        quietHoursEnd = LocalTime.of(7, 0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        getResult: Result<UserPreferences> = Result.Success(samplePrefs),
    ): NotificationSettingsViewModel {
        coEvery { preferencesRepository.get() } returns getResult
        return NotificationSettingsViewModel(preferencesRepository)
    }

    // -------------------------------------------------------------------------
    // 1. Initial state has isLoading true
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has isLoading true`() {
        // Default UI state before any preferences load
        val defaultState = NotificationSettingsUiState()
        assertTrue(defaultState.isLoading)
    }

    // -------------------------------------------------------------------------
    // 2. loadPreferences success updates UI state correctly
    // -------------------------------------------------------------------------

    @Test
    fun `loadPreferences success updates UI state correctly`() = runTest {
        val prefs = UserPreferences(
            notificationEnabled = false,
            quietHoursEnabled = false,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(6, 0),
        )
        val viewModel = createViewModel(Result.Success(prefs))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.notificationsEnabled)
        assertFalse(state.quietHoursEnabled)
        assertEquals(LocalTime.of(23, 0), state.quietHoursStart)
        assertEquals(LocalTime.of(6, 0), state.quietHoursEnd)
        assertNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 3. loadPreferences error sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `loadPreferences error sets error message`() = runTest {
        val viewModel = createViewModel(Result.Error("Database unavailable"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Unable to load notification settings.", state.error)
    }

    // -------------------------------------------------------------------------
    // 4. toggleNotifications updates preferences
    // -------------------------------------------------------------------------

    @Test
    fun `toggleNotifications updates preferences`() = runTest {
        val updatedPrefs = samplePrefs.copy(notificationEnabled = false)
        coEvery { preferencesRepository.update(any()) } returns Result.Success(updatedPrefs)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleNotifications(false)
        advanceUntilIdle()

        coVerify { preferencesRepository.update(match { !it.notificationEnabled }) }
        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    // -------------------------------------------------------------------------
    // 5. toggleNotifications with null prefs sets error
    // -------------------------------------------------------------------------

    @Test
    fun `toggleNotifications with null prefs sets error`() = runTest {
        // Force loadPreferences to fail so currentPreferences remains null
        val viewModel = createViewModel(Result.Error("Load failed"))
        advanceUntilIdle()

        // Clear the loading error to isolate the toggle error
        viewModel.onErrorConsumed()

        viewModel.toggleNotifications(true)

        assertEquals("Settings not loaded yet. Please wait.", viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // 6. toggleQuietHours updates preferences
    // -------------------------------------------------------------------------

    @Test
    fun `toggleQuietHours updates preferences`() = runTest {
        val updatedPrefs = samplePrefs.copy(quietHoursEnabled = false)
        coEvery { preferencesRepository.update(any()) } returns Result.Success(updatedPrefs)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleQuietHours(false)
        advanceUntilIdle()

        coVerify { preferencesRepository.update(match { !it.quietHoursEnabled }) }
        assertFalse(viewModel.uiState.value.quietHoursEnabled)
    }

    // -------------------------------------------------------------------------
    // 7. setQuietHoursStart updates preferences
    // -------------------------------------------------------------------------

    @Test
    fun `setQuietHoursStart updates preferences`() = runTest {
        val newStart = LocalTime.of(21, 0)
        val updatedPrefs = samplePrefs.copy(quietHoursStart = newStart)
        coEvery { preferencesRepository.update(any()) } returns Result.Success(updatedPrefs)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setQuietHoursStart(newStart)
        advanceUntilIdle()

        coVerify { preferencesRepository.update(match { it.quietHoursStart == newStart }) }
        assertEquals(newStart, viewModel.uiState.value.quietHoursStart)
    }

    // -------------------------------------------------------------------------
    // 8. setQuietHoursEnd updates preferences
    // -------------------------------------------------------------------------

    @Test
    fun `setQuietHoursEnd updates preferences`() = runTest {
        val newEnd = LocalTime.of(8, 0)
        val updatedPrefs = samplePrefs.copy(quietHoursEnd = newEnd)
        coEvery { preferencesRepository.update(any()) } returns Result.Success(updatedPrefs)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setQuietHoursEnd(newEnd)
        advanceUntilIdle()

        coVerify { preferencesRepository.update(match { it.quietHoursEnd == newEnd }) }
        assertEquals(newEnd, viewModel.uiState.value.quietHoursEnd)
    }

    // -------------------------------------------------------------------------
    // 9. updatePreferences error sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `updatePreferences error sets error message`() = runTest {
        coEvery { preferencesRepository.update(any()) } returns Result.Error("Save failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleNotifications(false)
        advanceUntilIdle()

        assertEquals("Unable to save settings. Please try again.", viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // 10. onErrorConsumed clears error
    // -------------------------------------------------------------------------

    @Test
    fun `onErrorConsumed clears error`() = runTest {
        coEvery { preferencesRepository.update(any()) } returns Result.Error("Save failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger an error
        viewModel.toggleNotifications(false)
        advanceUntilIdle()
        assertEquals("Unable to save settings. Please try again.", viewModel.uiState.value.error)

        // Clear the error
        viewModel.onErrorConsumed()

        assertNull(viewModel.uiState.value.error)
    }
}
