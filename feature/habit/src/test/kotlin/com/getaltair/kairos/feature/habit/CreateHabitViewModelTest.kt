package com.getaltair.kairos.feature.habit

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.usecase.CreateHabitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.DayOfWeek
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
class CreateHabitViewModelTest {

    private val createHabitUseCase: CreateHabitUseCase = mockk()
    private lateinit var viewModel: CreateHabitViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateHabitViewModel(createHabitUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state - currentStep is NAME`() {
        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `initial state - name is empty`() {
        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun `initial state - anchorType is AfterBehavior`() {
        assertEquals(AnchorType.AfterBehavior, viewModel.uiState.value.anchorType)
    }

    @Test
    fun `initial state - creationStatus is Idle`() {
        assertEquals(CreationStatus.Idle, viewModel.uiState.value.creationStatus)
    }

    @Test
    fun `initial state - category is null`() {
        assertNull(viewModel.uiState.value.category)
    }

    @Test
    fun `initial state - frequency is Daily`() {
        assertEquals(HabitFrequency.Daily, viewModel.uiState.value.frequency)
    }

    @Test
    fun `initial state - estimatedSeconds is 300`() {
        assertEquals(300, viewModel.uiState.value.estimatedSeconds)
    }

    @Test
    fun `initial state - creationStatus is not Created`() {
        assertTrue(viewModel.uiState.value.creationStatus is CreationStatus.Idle)
    }

    // -------------------------------------------------------------------------
    // 2. onNameChanged
    // -------------------------------------------------------------------------

    @Test
    fun `onNameChanged updates name in state`() {
        viewModel.onNameChanged("My New Habit")

        assertEquals("My New Habit", viewModel.uiState.value.name)
    }

    @Test
    fun `onNameChanged clears nameError`() {
        // First trigger a nameError by attempting to advance with blank name
        viewModel.goToNextStep()
        assertNotNull(viewModel.uiState.value.nameError)

        // Now changing the name should clear the error
        viewModel.onNameChanged("Valid Name")
        assertNull(viewModel.uiState.value.nameError)
    }

    // -------------------------------------------------------------------------
    // 3. goToNextStep from NAME - validation
    // -------------------------------------------------------------------------

    @Test
    fun `goToNextStep from NAME with blank name sets nameError and stays on NAME`() {
        viewModel.onNameChanged("")
        viewModel.goToNextStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.nameError)
        assertEquals("Name is required", viewModel.uiState.value.nameError)
    }

    @Test
    fun `goToNextStep from NAME with whitespace only name sets nameError and stays on NAME`() {
        viewModel.onNameChanged("   ")
        viewModel.goToNextStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `goToNextStep from NAME with name over 100 chars sets nameError and stays on NAME`() {
        val longName = "a".repeat(101)
        viewModel.onNameChanged(longName)
        viewModel.goToNextStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
        assertEquals("Name must be 100 characters or fewer", viewModel.uiState.value.nameError)
    }

    @Test
    fun `goToNextStep from NAME with valid name advances to ANCHOR`() {
        viewModel.onNameChanged("Morning Meditation")
        viewModel.goToNextStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `goToNextStep from NAME with exactly 100 chars advances to ANCHOR`() {
        val maxName = "a".repeat(100)
        viewModel.onNameChanged(maxName)
        viewModel.goToNextStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
    }

    // -------------------------------------------------------------------------
    // 4. goToNextStep from ANCHOR - validation
    // -------------------------------------------------------------------------

    private fun advanceToAnchorStep() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
    }

    @Test
    fun `goToNextStep from ANCHOR with default anchorType and blank behavior sets anchorError`() {
        advanceToAnchorStep()
        // anchorType defaults to AfterBehavior and anchorBehavior is blank
        viewModel.goToNextStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
        assertEquals("Please describe the anchor behavior", viewModel.uiState.value.anchorError)
    }

    @Test
    fun `goToNextStep from ANCHOR with AfterBehavior but blank anchorBehavior sets anchorError`() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        // anchorBehavior is cleared to "" by onAnchorTypeSelected
        viewModel.goToNextStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
        assertEquals("Please describe the anchor behavior", viewModel.uiState.value.anchorError)
    }

    @Test
    fun `goToNextStep from ANCHOR with AfterBehavior and non-blank anchorBehavior advances to CATEGORY`() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        viewModel.onAnchorBehaviorChanged("After brushing teeth")
        viewModel.goToNextStep()

        assertEquals(WizardStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.anchorError)
    }

    @Test
    fun `goToNextStep from ANCHOR with AtTime but null anchorTime sets anchorError`() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.AtTime)
        // anchorTime is null after selecting AtTime (cleared by onAnchorTypeSelected)
        viewModel.goToNextStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
        assertEquals("Please set a time", viewModel.uiState.value.anchorError)
    }

    @Test
    fun `goToNextStep from ANCHOR with AtTime and non-blank anchorTime advances to CATEGORY`() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.AtTime)
        viewModel.onAnchorTimeChanged("07:30")
        viewModel.goToNextStep()

        assertEquals(WizardStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.anchorError)
    }

    @Test
    fun `goToNextStep from ANCHOR with BeforeBehavior and non-blank anchorBehavior advances to CATEGORY`() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.BeforeBehavior)
        viewModel.onAnchorBehaviorChanged("Before lunch")
        viewModel.goToNextStep()

        assertEquals(WizardStep.CATEGORY, viewModel.uiState.value.currentStep)
    }

    // -------------------------------------------------------------------------
    // 5. goToNextStep from CATEGORY - validation
    // -------------------------------------------------------------------------

    private fun advanceToCategoryStep() {
        advanceToAnchorStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        viewModel.onAnchorBehaviorChanged("After waking up")
        viewModel.goToNextStep()
    }

    @Test
    fun `goToNextStep from CATEGORY with no category sets categoryError and stays on CATEGORY`() {
        advanceToCategoryStep()
        viewModel.goToNextStep()

        assertEquals(WizardStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertEquals("Please select a category", viewModel.uiState.value.categoryError)
    }

    @Test
    fun `goToNextStep from CATEGORY with category selected advances to OPTIONS`() {
        advanceToCategoryStep()
        viewModel.onCategorySelected(HabitCategory.Morning)
        viewModel.goToNextStep()

        assertEquals(WizardStep.OPTIONS, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.categoryError)
    }

    // -------------------------------------------------------------------------
    // 6. goToPreviousStep
    // -------------------------------------------------------------------------

    @Test
    fun `goToPreviousStep from NAME stays on NAME`() {
        viewModel.goToPreviousStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `goToPreviousStep from NAME preserves existing name`() {
        viewModel.onNameChanged("My Habit")
        viewModel.goToPreviousStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
        assertEquals("My Habit", viewModel.uiState.value.name)
    }

    @Test
    fun `goToPreviousStep from ANCHOR goes back to NAME`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)

        viewModel.goToPreviousStep()

        assertEquals(WizardStep.NAME, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `goToPreviousStep from ANCHOR preserves name value`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        viewModel.goToPreviousStep()

        assertEquals("Valid Habit", viewModel.uiState.value.name)
    }

    @Test
    fun `goToPreviousStep from CATEGORY goes back to ANCHOR`() {
        advanceToCategoryStep()

        viewModel.goToPreviousStep()

        assertEquals(WizardStep.ANCHOR, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `goToPreviousStep from OPTIONS goes back to CATEGORY`() {
        advanceToCategoryStep()
        viewModel.onCategorySelected(HabitCategory.Evening)
        viewModel.goToNextStep()
        assertEquals(WizardStep.OPTIONS, viewModel.uiState.value.currentStep)

        viewModel.goToPreviousStep()

        assertEquals(WizardStep.CATEGORY, viewModel.uiState.value.currentStep)
    }

    // -------------------------------------------------------------------------
    // 7. createHabit success
    // -------------------------------------------------------------------------

    private fun setupForCreate(
        name: String = "Morning Meditation",
        anchorBehavior: String = "After brushing teeth",
        category: HabitCategory = HabitCategory.Morning
    ) {
        viewModel.onNameChanged(name)
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        viewModel.onAnchorBehaviorChanged(anchorBehavior)
        viewModel.goToNextStep()
        viewModel.onCategorySelected(category)
        viewModel.goToNextStep()
    }

    @Test
    fun `createHabit success sets creationStatus to Created`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(CreationStatus.Created, viewModel.uiState.value.creationStatus)
    }

    @Test
    fun `createHabit success sets creationStatus to Created not Creating`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.creationStatus is CreationStatus.Creating)
    }

    @Test
    fun `createHabit passes habit with correct phase to use case`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(HabitPhase.ONBOARD, habitSlot.captured.phase)
    }

    @Test
    fun `createHabit passes habit with correct status to use case`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(HabitStatus.Active, habitSlot.captured.status)
    }

    @Test
    fun `createHabit passes habit with allowPartialCompletion true`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertTrue(habitSlot.captured.allowPartialCompletion)
    }

    @Test
    fun `createHabit passes habit with lapseThresholdDays 3`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(3, habitSlot.captured.lapseThresholdDays)
    }

    @Test
    fun `createHabit passes habit with relapseThresholdDays 7`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(7, habitSlot.captured.relapseThresholdDays)
    }

    @Test
    fun `createHabit passes habit with correct name`() = runTest {
        setupForCreate(name = "Evening Walk")
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals("Evening Walk", habitSlot.captured.name)
    }

    @Test
    fun `createHabit passes habit with correct category`() = runTest {
        setupForCreate(category = HabitCategory.Evening)
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(HabitCategory.Evening, habitSlot.captured.category)
    }

    @Test
    fun `createHabit passes habit with correct anchorBehavior`() = runTest {
        setupForCreate(anchorBehavior = "After lunch")
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals("After lunch", habitSlot.captured.anchorBehavior)
    }

    @Test
    fun `createHabit passes habit with default Daily frequency`() = runTest {
        setupForCreate()
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(HabitFrequency.Daily, habitSlot.captured.frequency)
    }

    @Test
    fun `createHabit calls use case exactly once`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        coVerify(exactly = 1) { createHabitUseCase(any()) }
    }

    // -------------------------------------------------------------------------
    // 8. createHabit failure
    // -------------------------------------------------------------------------

    @Test
    fun `createHabit failure maps technical error to user-friendly message`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Error("Database write failed")

        viewModel.createHabit()
        advanceUntilIdle()

        val status = viewModel.uiState.value.creationStatus
        assertTrue(status is CreationStatus.Failed)
        assertEquals("Something went wrong. Please try again.", (status as CreationStatus.Failed).message)
    }

    @Test
    fun `createHabit failure does not set creationStatus to Created`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Error("Save failed")

        viewModel.createHabit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.creationStatus is CreationStatus.Created)
    }

    @Test
    fun `createHabit failure does not leave creationStatus as Creating`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Error("Save failed")

        viewModel.createHabit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.creationStatus is CreationStatus.Creating)
    }

    @Test
    fun `createHabit failure with known error maps through ErrorMapper`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } returns Result.Error("anchorBehavior must not be blank")

        viewModel.createHabit()
        advanceUntilIdle()

        val status = viewModel.uiState.value.creationStatus
        assertTrue(status is CreationStatus.Failed)
        assertEquals("Please describe when you'll do this habit.", (status as CreationStatus.Failed).message)
    }

    // -------------------------------------------------------------------------
    // 8b. createHabit unexpected exception
    // -------------------------------------------------------------------------

    @Test
    fun `createHabit unexpected exception sets Failed with generic message`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } throws RuntimeException("Unexpected DB error")

        viewModel.createHabit()
        advanceUntilIdle()

        val status = viewModel.uiState.value.creationStatus
        assertTrue(status is CreationStatus.Failed)
        assertEquals("Something went wrong. Please try again.", (status as CreationStatus.Failed).message)
    }

    @Test
    fun `createHabit rethrows CancellationException without setting Failed`() = runTest {
        setupForCreate()
        coEvery { createHabitUseCase(any()) } throws kotlin.coroutines.cancellation.CancellationException("Cancelled")

        viewModel.createHabit()
        advanceUntilIdle()

        // CancellationException is rethrown, status should not be Failed
        assertFalse(viewModel.uiState.value.creationStatus is CreationStatus.Failed)
    }

    // -------------------------------------------------------------------------
    // 9. Custom frequency
    // -------------------------------------------------------------------------

    @Test
    fun `onFrequencySelected Custom sets frequency to Custom`() {
        viewModel.onFrequencySelected(HabitFrequency.Custom)

        assertEquals(HabitFrequency.Custom, viewModel.uiState.value.frequency)
    }

    @Test
    fun `onActiveDaysChanged sets activeDays`() {
        viewModel.onFrequencySelected(HabitFrequency.Custom)
        viewModel.onActiveDaysChanged(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), viewModel.uiState.value.activeDays)
    }

    @Test
    fun `createHabit with Custom frequency passes activeDays to use case`() = runTest {
        setupForCreate()
        viewModel.onFrequencySelected(HabitFrequency.Custom)
        viewModel.onActiveDaysChanged(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertEquals(HabitFrequency.Custom, habitSlot.captured.frequency)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            habitSlot.captured.activeDays
        )
    }

    @Test
    fun `createHabit with Daily frequency passes null activeDays to use case`() = runTest {
        setupForCreate()
        // Frequency is Daily by default
        val habitSlot = slot<Habit>()
        coEvery { createHabitUseCase(capture(habitSlot)) } returns Result.Success(mockk(relaxed = true))

        viewModel.createHabit()
        advanceUntilIdle()

        assertNull(habitSlot.captured.activeDays)
    }

    // -------------------------------------------------------------------------
    // 10. Anchor type switching clears anchorBehavior and anchorTime
    // -------------------------------------------------------------------------

    @Test
    fun `switching anchorType clears anchorBehavior`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        viewModel.onAnchorBehaviorChanged("After coffee")

        // Switch to a different anchor type
        viewModel.onAnchorTypeSelected(AnchorType.BeforeBehavior)

        assertEquals("", viewModel.uiState.value.anchorBehavior)
    }

    @Test
    fun `switching anchorType clears anchorTime`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AtTime)
        viewModel.onAnchorTimeChanged("08:00")
        assertNotNull(viewModel.uiState.value.anchorTime)

        // Switch to a different anchor type
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)

        assertNull(viewModel.uiState.value.anchorTime)
    }

    @Test
    fun `switching anchorType clears anchorError`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        // Leave anchorBehavior blank and advance to trigger error
        viewModel.goToNextStep()
        assertNotNull(viewModel.uiState.value.anchorError)

        // Switching type clears the error
        viewModel.onAnchorTypeSelected(AnchorType.AtTime)

        assertNull(viewModel.uiState.value.anchorError)
    }

    @Test
    fun `switching anchorType updates anchorType in state`() {
        viewModel.onNameChanged("Valid Habit")
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        assertEquals(AnchorType.AfterBehavior, viewModel.uiState.value.anchorType)

        viewModel.onAnchorTypeSelected(AnchorType.AtLocation)

        assertEquals(AnchorType.AtLocation, viewModel.uiState.value.anchorType)
    }

    // -------------------------------------------------------------------------
    // 11. createHabit with null category sets Failed status
    // -------------------------------------------------------------------------

    @Test
    fun `createHabit with null category sets Failed status`() = runTest {
        // Set up state with all fields except category
        viewModel.onNameChanged("Morning Meditation")
        viewModel.goToNextStep()
        viewModel.onAnchorTypeSelected(AnchorType.AfterBehavior)
        viewModel.onAnchorBehaviorChanged("After brushing teeth")
        viewModel.goToNextStep()
        // Do NOT select a category -- it remains null
        viewModel.goToNextStep() // advances to OPTIONS only if category is set; stays on CATEGORY
        // Force to OPTIONS by selecting category then clearing -- instead, just call createHabit directly
        // The ViewModel's createHabit checks category == null independently of wizard step
        // Reset ViewModel and call createHabit without setting category
        val freshViewModel = CreateHabitViewModel(createHabitUseCase)
        freshViewModel.onNameChanged("Morning Meditation")

        freshViewModel.createHabit()
        advanceUntilIdle()

        coVerify(exactly = 0) { createHabitUseCase(any()) }
        assertTrue(freshViewModel.uiState.value.creationStatus is CreationStatus.Failed)
        val status = freshViewModel.uiState.value.creationStatus as CreationStatus.Failed
        assertEquals("Please select a category before creating your habit.", status.message)
    }

    // -------------------------------------------------------------------------
    // 12. onAnchorTimeChanged sets both anchorTime and anchorBehavior
    // -------------------------------------------------------------------------

    @Test
    fun `onAnchorTimeChanged sets both anchorTime and anchorBehavior`() {
        viewModel.onAnchorTimeChanged("07:30")

        assertEquals("07:30", viewModel.uiState.value.anchorTime)
        assertEquals("07:30", viewModel.uiState.value.anchorBehavior)
    }

    // -------------------------------------------------------------------------
    // 13. goToNextStep from OPTIONS stays on OPTIONS
    // -------------------------------------------------------------------------

    @Test
    fun `goToNextStep from OPTIONS stays on OPTIONS`() {
        advanceToCategoryStep()
        viewModel.onCategorySelected(HabitCategory.Morning)
        viewModel.goToNextStep()
        assertEquals(WizardStep.OPTIONS, viewModel.uiState.value.currentStep)

        viewModel.goToNextStep()

        assertEquals(WizardStep.OPTIONS, viewModel.uiState.value.currentStep)
    }

    // -------------------------------------------------------------------------
    // 14. clearCreationError resets Failed status to Idle
    // -------------------------------------------------------------------------

    @Test
    fun `clearCreationError resets creationStatus to Idle`() = runTest {
        // Trigger a Failed status by calling createHabit with null category
        val freshViewModel = CreateHabitViewModel(createHabitUseCase)
        freshViewModel.onNameChanged("Some Habit")
        freshViewModel.createHabit()
        advanceUntilIdle()
        assertTrue(freshViewModel.uiState.value.creationStatus is CreationStatus.Failed)

        freshViewModel.clearCreationError()

        assertEquals(CreationStatus.Idle, freshViewModel.uiState.value.creationStatus)
    }

    // -------------------------------------------------------------------------
    // 15. Double-submission guard
    // -------------------------------------------------------------------------

    @Test
    fun `createHabit called twice rapidly invokes use case only once`() = runTest {
        setupForCreate()
        val deferred = kotlinx.coroutines.CompletableDeferred<Result<Habit>>()
        coEvery { createHabitUseCase(any()) } coAnswers { deferred.await() }

        viewModel.createHabit()
        // creationStatus is now Creating because the coroutine is suspended
        viewModel.createHabit()
        // Complete the deferred so the first call finishes
        deferred.complete(Result.Success(mockk(relaxed = true)))
        advanceUntilIdle()

        coVerify(exactly = 1) { createHabitUseCase(any()) }
    }

    // -------------------------------------------------------------------------
    // 16. Custom frequency with empty activeDays sets Failed status
    // -------------------------------------------------------------------------

    @Test
    fun `createHabit with Custom frequency and empty activeDays sets Failed status`() = runTest {
        setupForCreate()
        viewModel.onFrequencySelected(HabitFrequency.Custom)
        // Do NOT select any active days

        viewModel.createHabit()
        advanceUntilIdle()

        coVerify(exactly = 0) { createHabitUseCase(any()) }
        assertTrue(viewModel.uiState.value.creationStatus is CreationStatus.Failed)
        val status = viewModel.uiState.value.creationStatus as CreationStatus.Failed
        assertEquals("Please select at least one day for custom frequency", status.message)
    }
}
