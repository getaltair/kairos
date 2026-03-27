package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetectLapsesUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var recoveryRepository: RecoveryRepository
    private lateinit var useCase: DetectLapsesUseCase

    private fun formingHabit(lapseThreshold: Int = 3, relapseThreshold: Int = 7) = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        phase = HabitPhase.FORMING,
        lapseThresholdDays = lapseThreshold,
        relapseThresholdDays = relapseThreshold,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        completionRepository = mockk()
        recoveryRepository = mockk()
        useCase = DetectLapsesUseCase(habitRepository, completionRepository, recoveryRepository)
    }

    /**
     * Stubs completionRepository so that the N most recent due-days before today
     * return MISSED completions, and the day before that returns a FULL completion.
     */
    private fun stubConsecutiveMissed(habit: Habit, missedDays: Int) {
        var date = LocalDate.now().minusDays(1)
        var remaining = missedDays

        while (remaining > 0) {
            val missed = Completion(
                habitId = habit.id,
                date = date,
                type = CompletionType.Missed
            )
            coEvery { completionRepository.getForHabitOnDate(habit.id, date) } returns Result.Success(missed)
            date = date.minusDays(1)
            remaining--
        }

        // The day after the missed streak has a real completion (stop counting)
        val done = Completion(
            habitId = habit.id,
            date = date,
            type = CompletionType.Full
        )
        coEvery { completionRepository.getForHabitOnDate(habit.id, date) } returns Result.Success(done)
    }

    @Test
    fun `REC-1 detects lapse at threshold`() = runTest {
        val habit = formingHabit(lapseThreshold = 3)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 3)

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.update(capture(habitSlot)) } answers { Result.Success(habitSlot.captured) }
        coEvery { recoveryRepository.getPendingForHabit(habit.id) } returns Result.Success(null)

        val sessionSlot = slot<RecoverySession>()
        coEvery { recoveryRepository.insert(capture(sessionSlot)) } answers { Result.Success(sessionSlot.captured) }

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).value.size)
        assertEquals(habit.id, result.value[0].habitId)
        assertTrue(habitSlot.captured.phase is HabitPhase.LAPSED)
        assertTrue(sessionSlot.captured.type is RecoveryType.Lapse)
        assertTrue(sessionSlot.captured.status is SessionStatus.Pending)
    }

    @Test
    fun `does not trigger lapse below threshold`() = runTest {
        val habit = formingHabit(lapseThreshold = 3)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 2) // below threshold

        val result = useCase()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
        coVerify(exactly = 0) { habitRepository.update(any()) }
        coVerify(exactly = 0) { recoveryRepository.insert(any()) }
    }

    @Test
    fun `REC-2 does not create duplicate pending session`() = runTest {
        val habit = formingHabit(lapseThreshold = 3)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 3)

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.update(capture(habitSlot)) } answers { Result.Success(habitSlot.captured) }

        // Already has a pending session
        val existingSession = RecoverySession(
            habitId = habit.id,
            type = RecoveryType.Lapse,
            status = SessionStatus.Pending,
            blockers = setOf(Blocker.Other)
        )
        coEvery { recoveryRepository.getPendingForHabit(habit.id) } returns Result.Success(existingSession)

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).value.size)
        assertEquals(habit.id, result.value.first().habitId)
        // Phase should still be updated
        assertTrue(habitSlot.captured.phase is HabitPhase.LAPSED)
        // But no new session should be created
        coVerify(exactly = 0) { recoveryRepository.insert(any()) }
    }

    @Test
    fun `REC-4 escalates lapse to relapse at relapse threshold`() = runTest {
        val habit = formingHabit(lapseThreshold = 3, relapseThreshold = 7)
            .copy(phase = HabitPhase.LAPSED)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 7)

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.update(capture(habitSlot)) } answers { Result.Success(habitSlot.captured) }

        val existingSession = RecoverySession(
            habitId = habit.id,
            type = RecoveryType.Lapse,
            status = SessionStatus.Pending,
            blockers = setOf(Blocker.Other)
        )
        coEvery { recoveryRepository.getPendingForHabit(habit.id) } returns Result.Success(existingSession)

        val sessionSlot = slot<RecoverySession>()
        coEvery { recoveryRepository.update(capture(sessionSlot)) } answers { Result.Success(sessionSlot.captured) }

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).value.size)
        assertEquals(habit.id, result.value.first().habitId)
        assertTrue(habitSlot.captured.phase is HabitPhase.RELAPSED)
        assertTrue(sessionSlot.captured.type is RecoveryType.Relapse)
    }

    @Test
    fun `REC-4 does not de-escalate relapse to lapse`() = runTest {
        // Habit is already RELAPSED -- should not be re-detected or downgraded
        val habit = formingHabit(lapseThreshold = 3, relapseThreshold = 7)
            .copy(phase = HabitPhase.RELAPSED)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 5) // above lapse but below relapse

        val result = useCase()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
        // No updates should happen -- already RELAPSED
        coVerify(exactly = 0) { habitRepository.update(any()) }
        coVerify(exactly = 0) { recoveryRepository.insert(any()) }
        coVerify(exactly = 0) { recoveryRepository.update(any()) }
    }

    @Test
    fun `skips habits already in LAPSED phase for lapse detection`() = runTest {
        val habit = formingHabit(lapseThreshold = 3, relapseThreshold = 7)
            .copy(phase = HabitPhase.LAPSED)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 4) // above lapse threshold but below relapse

        val result = useCase()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `returns error when habit repository fails`() = runTest {
        coEvery { habitRepository.getActiveHabits() } returns Result.Error("DB error")

        val result = useCase()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("DB error"))
    }

    @Test
    fun `countConsecutiveMissed stops at habit creation date`() = runTest {
        // Habit created 2 days ago -- even with threshold 3, only 2 days can be counted
        val twoDaysAgo = Instant.now().minusSeconds(2 * 24 * 3600)
        val habit = formingHabit(lapseThreshold = 3).copy(
            createdAt = twoDaysAgo,
            updatedAt = twoDaysAgo
        )
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))

        // Stub all dates from yesterday back as missed (no completion)
        val earliestDate = twoDaysAgo.atZone(ZoneId.systemDefault()).toLocalDate()
        var date = LocalDate.now().minusDays(1)
        while (date >= earliestDate) {
            coEvery { completionRepository.getForHabitOnDate(habit.id, date) } returns Result.Success(null)
            date = date.minusDays(1)
        }

        val result = useCase()

        // Only 2 missed days possible, threshold is 3, so no lapse detected
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `does not add to affected list when habit update fails`() = runTest {
        val habit = formingHabit(lapseThreshold = 3)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 3)

        coEvery { habitRepository.update(any()) } returns Result.Error("DB error")

        val result = useCase()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
    }

    @Test
    fun `does not add to affected list when session insert fails`() = runTest {
        val habit = formingHabit(lapseThreshold = 3)
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        stubConsecutiveMissed(habit, 3)

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.update(capture(habitSlot)) } answers { Result.Success(habitSlot.captured) }
        coEvery { recoveryRepository.getPendingForHabit(habit.id) } returns Result.Success(null)
        coEvery { recoveryRepository.insert(any()) } returns Result.Error("Insert failed")

        val result = useCase()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
    }
}
