package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.RoutineExecutionDao
import com.getaltair.kairos.data.entity.RoutineExecutionEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.domain.testutil.HabitFactory
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineExecutionRepositoryImplTest {

    private lateinit var routineExecutionDao: RoutineExecutionDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: RoutineExecutionRepositoryImpl

    private val userId = "test-user-id"

    @Before
    fun setup() {
        routineExecutionDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = RoutineExecutionRepositoryImpl(
            routineExecutionDao = routineExecutionDao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    // ---- insert ----

    @Test
    fun `insert delegates to DAO and returns success`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { routineExecutionDao.insert(any<RoutineExecutionEntity>()) } just Runs

        val result = repository.insert(execution)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { routineExecutionDao.insert(any<RoutineExecutionEntity>()) }
    }

    @Test
    fun `insert triggers sync push when user is signed in`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { routineExecutionDao.insert(any<RoutineExecutionEntity>()) } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.insert(execution)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "ROUTINE_EXECUTION", execution.id.toString(), execution)
        }
    }

    @Test
    fun `insert does not trigger sync when user is not signed in`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { routineExecutionDao.insert(any<RoutineExecutionEntity>()) } just Runs

        val result = repository.insert(execution)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `insert succeeds even when sync throws exception`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { routineExecutionDao.insert(any<RoutineExecutionEntity>()) } just Runs
        coEvery {
            syncTrigger.triggerPush(any(), any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val result = repository.insert(execution)

        assertTrue("Local insert should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    @Test
    fun `insert returns error when DAO throws`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineExecutionDao.insert(any<RoutineExecutionEntity>())
        } throws RuntimeException("DB write failed")

        val result = repository.insert(execution)

        assertTrue(result is Result.Error)
    }

    // ---- update ----

    @Test
    fun `update delegates to DAO and returns success`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineExecutionDao.update(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        val result = repository.update(execution)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            routineExecutionDao.update(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `update triggers sync push when user is signed in`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            routineExecutionDao.update(any(), any(), any(), any(), any(), any(), any())
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(execution)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "ROUTINE_EXECUTION", execution.id.toString(), execution)
        }
    }

    @Test
    fun `update does not trigger sync when user is not signed in`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineExecutionDao.update(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        val result = repository.update(execution)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `update returns error when DAO throws`() = testScope.runTest {
        val execution = HabitFactory.routineExecution()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineExecutionDao.update(any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("DB failure")

        val result = repository.update(execution)

        assertTrue(result is Result.Error)
    }

    private fun testExecutionEntity(id: UUID = UUID.randomUUID()) = RoutineExecutionEntity(
        id = id,
        routineId = HabitFactory.DEFAULT_ROUTINE_ID,
        startedAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
        status = ExecutionStatus.InProgress,
        createdAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
        updatedAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
    )

    // ---- getById ----

    @Test
    fun `getById returns execution when found`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineExecutionDao.getById(id) } returns testExecutionEntity(id)

        val result = repository.getById(id)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `getById returns null when not found`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineExecutionDao.getById(id) } returns null

        val result = repository.getById(id)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `getById returns error when DAO throws`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineExecutionDao.getById(id) } throws RuntimeException("DB failure")

        val result = repository.getById(id)

        assertTrue(result is Result.Error)
    }

    // ---- getActiveForRoutine ----

    @Test
    fun `getActiveForRoutine returns execution when active one exists`() = testScope.runTest {
        val routineId = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery {
            routineExecutionDao.getActiveForRoutine(routineId)
        } returns testExecutionEntity()

        val result = repository.getActiveForRoutine(routineId)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `getActiveForRoutine returns null when no active execution`() = testScope.runTest {
        val routineId = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery { routineExecutionDao.getActiveForRoutine(routineId) } returns null

        val result = repository.getActiveForRoutine(routineId)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `getActiveForRoutine returns error when DAO throws`() = testScope.runTest {
        val routineId = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery {
            routineExecutionDao.getActiveForRoutine(routineId)
        } throws RuntimeException("DB failure")

        val result = repository.getActiveForRoutine(routineId)

        assertTrue(result is Result.Error)
    }
}
