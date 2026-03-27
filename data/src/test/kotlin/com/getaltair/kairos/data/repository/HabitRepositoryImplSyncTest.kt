package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.HabitDao
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncTrigger
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitRepositoryImplSyncTest {

    private lateinit var habitDao: HabitDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: HabitRepositoryImpl

    private val userId = "test-user-id"

    private fun testHabit(id: UUID = UUID.randomUUID()) = Habit(
        id = id,
        name = "Test Habit",
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    @Before
    fun setup() {
        habitDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = HabitRepositoryImpl(
            habitDao = habitDao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    @Test
    fun `insert triggers sync push when user is signed in`() = testScope.runTest {
        val habit = testHabit()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { habitDao.insert(any<HabitEntity>()) } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.insert(habit)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "HABIT", habit.id.toString(), habit)
        }
    }

    @Test
    fun `insert does not trigger sync when user is not signed in`() = testScope.runTest {
        val habit = testHabit()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { habitDao.insert(any<HabitEntity>()) } just Runs

        val result = repository.insert(habit)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `insert succeeds even when sync throws exception`() = testScope.runTest {
        val habit = testHabit()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { habitDao.insert(any<HabitEntity>()) } just Runs
        coEvery {
            syncTrigger.triggerPush(any(), any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val result = repository.insert(habit)

        assertTrue("Local insert should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    @Test
    fun `update triggers sync push when user is signed in`() = testScope.runTest {
        val habit = testHabit()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            habitDao.update(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(),
            )
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(habit)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "HABIT", habit.id.toString(), habit)
        }
    }

    @Test
    fun `delete triggers sync deletion when user is signed in`() = testScope.runTest {
        val habitId = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { habitDao.delete(habitId) } just Runs
        coEvery { syncTrigger.triggerDeletion(any(), any(), any()) } just Runs

        val result = repository.delete(habitId)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerDeletion(userId, "HABIT", habitId.toString())
        }
    }

    @Test
    fun `delete succeeds even when sync deletion throws exception`() = testScope.runTest {
        val habitId = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { habitDao.delete(habitId) } just Runs
        coEvery {
            syncTrigger.triggerDeletion(any(), any(), any())
        } throws RuntimeException("Network error")

        val result = repository.delete(habitId)

        assertTrue("Local delete should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }
}
