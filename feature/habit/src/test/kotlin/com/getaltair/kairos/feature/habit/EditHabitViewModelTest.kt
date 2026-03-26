package com.getaltair.kairos.feature.habit

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.usecase.EditHabitUseCase
import com.getaltair.kairos.domain.usecase.GetHabitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditHabitViewModelTest {

    private val editHabitUseCase: EditHabitUseCase = mockk()
    private val getHabitUseCase: GetHabitUseCase = mockk()

    private lateinit var viewModel: EditHabitViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testHabitId = UUID.randomUUID()
    private val testHabit = Habit(
        id = testHabitId,
        name = "Morning Meditation",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        status = HabitStatus.Active,
        estimatedSeconds = 600,
        microVersion = "Just sit still for 1 minute",
        icon = "meditation",
        color = "#4CAF50"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditHabitViewModel(editHabitUseCase, getHabitUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. loadHabit pre-fills all fields
    // -------------------------------------------------------------------------

    @Test
    fun `loadHabit pre-fills name`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals("Morning Meditation", viewModel.uiState.value.name)
    }

    @Test
    fun `loadHabit pre-fills anchorType and anchorBehavior`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals(AnchorType.AfterBehavior, viewModel.uiState.value.anchorType)
        assertEquals("After brushing teeth", viewModel.uiState.value.anchorBehavior)
    }

    @Test
    fun `loadHabit pre-fills category`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals(HabitCategory.Morning, viewModel.uiState.value.category)
    }

    @Test
    fun `loadHabit pre-fills estimatedSeconds`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals(600, viewModel.uiState.value.estimatedSeconds)
    }

    @Test
    fun `loadHabit pre-fills microVersion`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals("Just sit still for 1 minute", viewModel.uiState.value.microVersion)
    }

    @Test
    fun `loadHabit pre-fills icon and color`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals("meditation", viewModel.uiState.value.icon)
        assertEquals("#4CAF50", viewModel.uiState.value.color)
    }

    @Test
    fun `loadHabit pre-fills frequency`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals(HabitFrequency.Daily, viewModel.uiState.value.frequency)
    }

    @Test
    fun `loadHabit sets isLoading to false`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadHabit sets habitId`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertEquals(testHabitId, viewModel.uiState.value.habitId)
    }

    @Test
    fun `loadHabit error sets loadError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Error("Not found")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.loadError)
    }

    // -------------------------------------------------------------------------
    // 2. saveHabit calls EditHabitUseCase with correct values
    // -------------------------------------------------------------------------

    @Test
    fun `saveHabit calls editHabitUseCase with updated habit`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        val habitSlot = slot<Habit>()
        coEvery { editHabitUseCase(capture(habitSlot)) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        viewModel.onNameChanged("Updated Name")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertTrue(habitSlot.isCaptured)
        assertEquals("Updated Name", habitSlot.captured.name)
        assertEquals(testHabitId, habitSlot.captured.id)
    }

    @Test
    fun `saveHabit success sets isSaved`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        coEvery { editHabitUseCase(any()) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.saveHabit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    // -------------------------------------------------------------------------
    // 3. Validation errors
    // -------------------------------------------------------------------------

    @Test
    fun `saveHabit with blank name sets nameError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onNameChanged("")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Name is required", viewModel.uiState.value.nameError)
        coVerify(exactly = 0) { editHabitUseCase(any()) }
    }

    @Test
    fun `saveHabit with name over 100 chars sets nameError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onNameChanged("a".repeat(101))
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Name must be 100 characters or fewer", viewModel.uiState.value.nameError)
        coVerify(exactly = 0) { editHabitUseCase(any()) }
    }

    @Test
    fun `saveHabit with blank anchor behavior sets anchorError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        // anchorBehavior is cleared to "" by onAnchorTypeSelected
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Please describe the anchor behavior", viewModel.uiState.value.anchorError)
        coVerify(exactly = 0) { editHabitUseCase(any()) }
    }

    // -------------------------------------------------------------------------
    // 4. Save error handling
    // -------------------------------------------------------------------------

    @Test
    fun `saveHabit failure sets saveError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        coEvery { editHabitUseCase(any()) } returns Result.Error("Failed to update habit")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.saveHabit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveHabit unexpected exception sets saveError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        coEvery { editHabitUseCase(any()) } throws RuntimeException("Unexpected")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.saveError)
    }

    @Test
    fun `clearSaveError clears saveError`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        coEvery { editHabitUseCase(any()) } returns Result.Error("Failed")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.saveHabit()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.saveError)

        viewModel.clearSaveError()

        assertNull(viewModel.uiState.value.saveError)
    }

    // -------------------------------------------------------------------------
    // 5. Double-submission guard
    // -------------------------------------------------------------------------

    @Test
    fun `saveHabit called twice rapidly invokes use case only once`() = runTest {
        coEvery { getHabitUseCase(testHabitId) } returns Result.Success(testHabit)
        val deferred = kotlinx.coroutines.CompletableDeferred<Result<Habit>>()
        coEvery { editHabitUseCase(any()) } coAnswers { deferred.await() }

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.saveHabit()
        viewModel.saveHabit()
        deferred.complete(Result.Success(testHabit))
        advanceUntilIdle()

        coVerify(exactly = 1) { editHabitUseCase(any()) }
    }
}
