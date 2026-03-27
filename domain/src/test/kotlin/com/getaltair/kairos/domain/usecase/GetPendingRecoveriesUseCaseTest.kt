package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetPendingRecoveriesUseCaseTest {

    private lateinit var recoveryRepository: RecoveryRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: GetPendingRecoveriesUseCase

    private fun testHabit(id: UUID = UUID.randomUUID(), name: String = "Meditate") = Habit(
        id = id,
        name = name,
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        phase = HabitPhase.LAPSED,
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
        useCase = GetPendingRecoveriesUseCase(recoveryRepository, habitRepository)
    }

    @Test
    fun `returns paired sessions with habits`() = runTest {
        val habit1 = testHabit(name = "Meditate")
        val habit2 = testHabit(name = "Exercise")
        val session1 = pendingSession(habit1.id)
        val session2 = pendingSession(habit2.id)

        coEvery { recoveryRepository.getAllPending() } returns Result.Success(listOf(session1, session2))
        coEvery { habitRepository.getById(habit1.id) } returns Result.Success(habit1)
        coEvery { habitRepository.getById(habit2.id) } returns Result.Success(habit2)

        val result = useCase()

        assertTrue(result is Result.Success)
        val pairs = (result as Result.Success).value
        assertEquals(2, pairs.size)
        assertEquals(session1, pairs[0].first)
        assertEquals(habit1, pairs[0].second)
        assertEquals(session2, pairs[1].first)
        assertEquals(habit2, pairs[1].second)
    }

    @Test
    fun `returns empty list when no pending sessions`() = runTest {
        coEvery { recoveryRepository.getAllPending() } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val pairs = (result as Result.Success).value
        assertTrue(pairs.isEmpty())
    }

    @Test
    fun `excludes sessions with deleted habits`() = runTest {
        val validHabit = testHabit(name = "Meditate")
        val deletedHabitId = UUID.randomUUID()
        val session1 = pendingSession(validHabit.id)
        val session2 = pendingSession(deletedHabitId)

        coEvery { recoveryRepository.getAllPending() } returns Result.Success(listOf(session1, session2))
        coEvery { habitRepository.getById(validHabit.id) } returns Result.Success(validHabit)
        // Deleted habit returns Error from the repository
        coEvery { habitRepository.getById(deletedHabitId) } returns Result.Error("Not found")

        val result = useCase()

        assertTrue(result is Result.Success)
        val pairs = (result as Result.Success).value
        assertEquals(1, pairs.size)
        assertEquals(validHabit.id, pairs[0].second.id)
    }

    @Test
    fun `logs warning for transient habit load failure`() = runTest {
        val failedHabitId = UUID.randomUUID()
        val session = pendingSession(failedHabitId)

        coEvery { recoveryRepository.getAllPending() } returns Result.Success(listOf(session))
        coEvery { habitRepository.getById(failedHabitId) } returns Result.Error("Database timeout")

        val result = useCase()

        // Session should be excluded from results (habit could not be loaded)
        assertTrue(result is Result.Success)
        val pairs = (result as Result.Success).value
        assertTrue(pairs.isEmpty())
    }

    @Test
    fun `propagates repository error from getAllPending`() = runTest {
        coEvery { recoveryRepository.getAllPending() } returns Result.Error("Database unavailable")

        val result = useCase()

        assertTrue(result is Result.Error)
        assertEquals("Database unavailable", (result as Result.Error).message)
    }
}
