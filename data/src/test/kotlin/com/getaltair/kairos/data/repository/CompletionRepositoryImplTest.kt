package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.CompletionDao
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.domain.testutil.HabitFactory
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompletionRepositoryImplTest {

    private lateinit var completionDao: CompletionDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: CompletionRepositoryImpl

    private val userId = "test-user-id"

    @Before
    fun setup() {
        completionDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = CompletionRepositoryImpl(
            completionDao = completionDao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    // ---- insert ----

    @Test
    fun `insert delegates to DAO and returns success`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { completionDao.insert(any<CompletionEntity>()) } just Runs

        val result = repository.insert(completion)

        assertTrue(result is Result.Success)
        assertEquals(completion, (result as Result.Success).value)
        coVerify(exactly = 1) { completionDao.insert(any<CompletionEntity>()) }
    }

    @Test
    fun `insert triggers sync push when user is signed in`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { completionDao.insert(any<CompletionEntity>()) } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.insert(completion)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "COMPLETION", completion.id.toString(), completion)
        }
    }

    @Test
    fun `insert does not trigger sync when user is not signed in`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { completionDao.insert(any<CompletionEntity>()) } just Runs

        val result = repository.insert(completion)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `insert succeeds even when sync throws exception`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { completionDao.insert(any<CompletionEntity>()) } just Runs
        coEvery {
            syncTrigger.triggerPush(any(), any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val result = repository.insert(completion)

        assertTrue("Local insert should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    @Test
    fun `insert returns error when DAO throws`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            completionDao.insert(any<CompletionEntity>())
        } throws RuntimeException("DB write failed")

        val result = repository.insert(completion)

        assertTrue(result is Result.Error)
    }

    // ---- getForHabitOnDate ----

    @Test
    fun `getForHabitOnDate returns completion when found`() = testScope.runTest {
        val habitId = HabitFactory.DEFAULT_HABIT_ID
        val date = LocalDate.of(2025, 1, 1)
        val entity = CompletionEntity(
            habitId = habitId,
            date = date.toString(),
            completedAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
            type = "Full",
            createdAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
            updatedAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
        )
        coEvery { completionDao.getForHabitOnDate(habitId, date.toString()) } returns entity

        val result = repository.getForHabitOnDate(habitId, date)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `getForHabitOnDate returns null when not found`() = testScope.runTest {
        val habitId = HabitFactory.DEFAULT_HABIT_ID
        val date = LocalDate.of(2025, 1, 1)
        coEvery { completionDao.getForHabitOnDate(habitId, date.toString()) } returns null

        val result = repository.getForHabitOnDate(habitId, date)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `getForHabitOnDate returns error when DAO throws`() = testScope.runTest {
        val habitId = HabitFactory.DEFAULT_HABIT_ID
        val date = LocalDate.of(2025, 1, 1)
        coEvery {
            completionDao.getForHabitOnDate(habitId, date.toString())
        } throws RuntimeException("DB read failed")

        val result = repository.getForHabitOnDate(habitId, date)

        assertTrue(result is Result.Error)
    }

    // ---- getForDateRange ----

    @Test
    fun `getForDateRange returns list of completions`() = testScope.runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 7)
        coEvery {
            completionDao.getForDateRange(startDate.toString(), endDate.toString())
        } returns emptyList()

        val result = repository.getForDateRange(startDate, endDate)

        assertTrue(result is Result.Success)
        assertEquals(emptyList<Any>(), (result as Result.Success).value)
    }

    @Test
    fun `getForDateRange returns error when DAO throws`() = testScope.runTest {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 7)
        coEvery {
            completionDao.getForDateRange(startDate.toString(), endDate.toString())
        } throws RuntimeException("DB failure")

        val result = repository.getForDateRange(startDate, endDate)

        assertTrue(result is Result.Error)
    }

    // ---- delete ----

    @Test
    fun `delete delegates to DAO and returns success`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { completionDao.delete(id) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionDao.delete(id) }
    }

    @Test
    fun `delete triggers sync deletion when user is signed in`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { completionDao.delete(id) } just Runs
        coEvery { syncTrigger.triggerDeletion(any(), any(), any()) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerDeletion(userId, "COMPLETION", id.toString())
        }
    }

    @Test
    fun `delete does not trigger sync when user is not signed in`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { completionDao.delete(id) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerDeletion(any(), any(), any()) }
    }

    @Test
    fun `delete succeeds even when sync deletion throws exception`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { completionDao.delete(id) } just Runs
        coEvery {
            syncTrigger.triggerDeletion(any(), any(), any())
        } throws RuntimeException("Network error")

        val result = repository.delete(id)

        assertTrue("Local delete should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    // ---- deleteForHabit ----

    @Test
    fun `deleteForHabit delegates to DAO and does not trigger sync`() = testScope.runTest {
        val habitId = HabitFactory.DEFAULT_HABIT_ID
        coEvery { completionDao.deleteForHabit(habitId) } just Runs

        val result = repository.deleteForHabit(habitId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionDao.deleteForHabit(habitId) }
        coVerify(exactly = 0) { syncTrigger.triggerDeletion(any(), any(), any()) }
    }

    // ---- update ----

    @Test
    fun `update triggers sync push when user is signed in`() = testScope.runTest {
        val completion = HabitFactory.completion()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            completionDao.update(any(), any(), any(), any(), any(), any(), any(), any())
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(completion)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "COMPLETION", completion.id.toString(), completion)
        }
    }

    // ---- getLatestForHabit ----

    @Test
    fun `getLatestForHabit returns null when no completions exist`() = testScope.runTest {
        val habitId = HabitFactory.DEFAULT_HABIT_ID
        coEvery { completionDao.getForHabit(habitId) } returns emptyList()

        val result = repository.getLatestForHabit(habitId)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }
}
