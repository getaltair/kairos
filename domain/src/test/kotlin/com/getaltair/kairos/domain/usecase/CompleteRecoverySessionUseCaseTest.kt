package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompleteRecoverySessionUseCaseTest {

    private lateinit var recoveryRepository: RecoveryRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: CompleteRecoverySessionUseCase

    private fun lapsedHabit(microVersion: String? = null) = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        phase = HabitPhase.LAPSED,
        microVersion = microVersion,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    private fun pendingSession(habitId: UUID) = RecoverySession(
        habitId = habitId,
        type = RecoveryType.Lapse,
        status = SessionStatus.Pending,
        blockers = setOf(Blocker.NoEnergy)
    )

    @Before
    fun setup() {
        recoveryRepository = mockk()
        habitRepository = mockk()
        useCase = CompleteRecoverySessionUseCase(recoveryRepository, habitRepository)
    }

    private fun stubSessionAndHabit(
        session: RecoverySession,
        habit: Habit
    ): Pair<io.mockk.CapturingSlot<Habit>, io.mockk.CapturingSlot<RecoverySession>> {
        coEvery { recoveryRepository.getById(session.id) } returns Result.Success(session)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.update(capture(habitSlot)) } answers { Result.Success(habitSlot.captured) }

        val sessionSlot = slot<RecoverySession>()
        coEvery { recoveryRepository.update(capture(sessionSlot)) } answers { Result.Success(sessionSlot.captured) }

        return habitSlot to sessionSlot
    }

    @Test
    fun `Resume action sets phase to FORMING`() = runTest {
        val habit = lapsedHabit()
        val session = pendingSession(habit.id)
        val (habitSlot, sessionSlot) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.Resume)

        assertTrue(result is Result.Success)
        assertTrue(habitSlot.captured.phase is HabitPhase.FORMING)
        assertTrue(sessionSlot.captured.status is SessionStatus.Completed)
        assertTrue(sessionSlot.captured.action is RecoveryAction.Resume)
        assertNotNull(sessionSlot.captured.completedAt)
    }

    @Test
    fun `Simplify action with microVersion activates it and sets FORMING`() = runTest {
        val habit = lapsedHabit(microVersion = "5-min meditation")
        val session = pendingSession(habit.id)
        val (habitSlot, sessionSlot) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.Simplify)

        assertTrue(result is Result.Success)
        assertEquals("5-min meditation", habitSlot.captured.name)
        assertTrue(habitSlot.captured.phase is HabitPhase.FORMING)
        assertTrue(sessionSlot.captured.action is RecoveryAction.Simplify)
    }

    @Test
    fun `Simplify action without microVersion acts like Resume`() = runTest {
        val habit = lapsedHabit(microVersion = null)
        val session = pendingSession(habit.id)
        val (habitSlot, _) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.Simplify)

        assertTrue(result is Result.Success)
        assertEquals("Meditate", habitSlot.captured.name) // name unchanged
        assertTrue(habitSlot.captured.phase is HabitPhase.FORMING)
    }

    @Test
    fun `Pause action sets status to PAUSED`() = runTest {
        val habit = lapsedHabit()
        val session = pendingSession(habit.id)
        val (habitSlot, sessionSlot) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.Pause)

        assertTrue(result is Result.Success)
        assertTrue(habitSlot.captured.status is HabitStatus.Paused)
        assertNotNull(habitSlot.captured.pausedAt)
        assertTrue(sessionSlot.captured.action is RecoveryAction.Pause)
    }

    @Test
    fun `Archive action sets status to ARCHIVED`() = runTest {
        val habit = lapsedHabit()
        val session = pendingSession(habit.id)
        val (habitSlot, sessionSlot) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.Archive)

        assertTrue(result is Result.Success)
        assertTrue(habitSlot.captured.status is HabitStatus.Archived)
        assertNotNull(habitSlot.captured.archivedAt)
        assertTrue(sessionSlot.captured.action is RecoveryAction.Archive)
    }

    @Test
    fun `FreshStart action sets phase to FORMING`() = runTest {
        val habit = lapsedHabit().copy(phase = HabitPhase.RELAPSED)
        val session = RecoverySession(
            habitId = habit.id,
            type = RecoveryType.Relapse,
            status = SessionStatus.Pending,
            blockers = setOf(Blocker.NoEnergy)
        )
        val (habitSlot, sessionSlot) = stubSessionAndHabit(session, habit)

        val result = useCase(session.id, RecoveryAction.FreshStart)

        assertTrue(result is Result.Success)
        assertTrue(habitSlot.captured.phase is HabitPhase.FORMING)
        assertTrue(sessionSlot.captured.action is RecoveryAction.FreshStart)
    }

    @Test
    fun `REC-3 rejects completing a non-pending session`() = runTest {
        val habit = lapsedHabit()
        val completedSession = RecoverySession(
            habitId = habit.id,
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            action = RecoveryAction.Resume,
            blockers = setOf(Blocker.NoEnergy),
            completedAt = Instant.now()
        )
        coEvery { recoveryRepository.getById(completedSession.id) } returns Result.Success(completedSession)

        val result = useCase(completedSession.id, RecoveryAction.Resume)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Pending"))
        coVerify(exactly = 0) { habitRepository.update(any()) }
        coVerify(exactly = 0) { recoveryRepository.update(any()) }
    }

    @Test
    fun `REC-3 rejects completing an abandoned session`() = runTest {
        val habit = lapsedHabit()
        val abandonedSession = RecoverySession(
            habitId = habit.id,
            type = RecoveryType.Lapse,
            status = SessionStatus.Abandoned,
            blockers = setOf(Blocker.Other)
        )
        coEvery { recoveryRepository.getById(abandonedSession.id) } returns Result.Success(abandonedSession)

        val result = useCase(abandonedSession.id, RecoveryAction.Resume)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Pending"))
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `returns error when session not found`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { recoveryRepository.getById(unknownId) } returns Result.Success(null)

        val result = useCase(unknownId, RecoveryAction.Resume)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("not found"))
    }

    @Test
    fun `returns error when associated habit not found`() = runTest {
        val habit = lapsedHabit()
        val session = pendingSession(habit.id)
        coEvery { recoveryRepository.getById(session.id) } returns Result.Success(session)
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(session.id, RecoveryAction.Resume)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("habit not found"))
    }

    @Test
    fun `returns error when habit update fails`() = runTest {
        val habit = lapsedHabit()
        val session = pendingSession(habit.id)
        coEvery { recoveryRepository.getById(session.id) } returns Result.Success(session)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { habitRepository.update(any()) } returns Result.Error("DB error")

        val result = useCase(session.id, RecoveryAction.Resume)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to update habit"))
        coVerify(exactly = 0) { recoveryRepository.update(any()) }
    }
}
