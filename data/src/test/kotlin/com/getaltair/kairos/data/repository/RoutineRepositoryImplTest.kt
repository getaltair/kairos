package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.RoutineDao
import com.getaltair.kairos.data.dao.RoutineHabitDao
import com.getaltair.kairos.data.dao.RoutineVariantDao
import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.data.entity.RoutineHabitEntity
import com.getaltair.kairos.data.entity.RoutineVariantEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineRepositoryImplTest {

    private lateinit var routineDao: RoutineDao
    private lateinit var routineHabitDao: RoutineHabitDao
    private lateinit var routineVariantDao: RoutineVariantDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: RoutineRepositoryImpl

    private val userId = "test-user-id"

    @Before
    fun setup() {
        routineDao = mockk(relaxed = true)
        routineHabitDao = mockk(relaxed = true)
        routineVariantDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = RoutineRepositoryImpl(
            routineDao = routineDao,
            routineHabitDao = routineHabitDao,
            routineVariantDao = routineVariantDao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    // ---- insert ----

    @Test
    fun `insert delegates to DAO with habits and returns success`() = testScope.runTest {
        val routine = HabitFactory.routine()
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        } just Runs

        val result = repository.insert(routine, habitIds)

        assertTrue(result is Result.Success)
        assertEquals(routine, (result as Result.Success).value)
        coVerify(exactly = 1) {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        }
    }

    @Test
    fun `insert triggers sync push for routine and each habit when user is signed in`() = testScope.runTest {
        val routine = HabitFactory.routine()
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.insert(routine, habitIds)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        // 1 sync for the routine + 2 syncs for the routine habits
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "ROUTINE", routine.id.toString(), routine)
        }
        coVerify(exactly = 2) {
            syncTrigger.triggerPush(userId, "ROUTINE_HABIT", any(), any())
        }
    }

    @Test
    fun `insert does not trigger sync when user is not signed in`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        } just Runs

        val result = repository.insert(routine, emptyList())

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `insert succeeds even when sync throws exception`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        } just Runs
        coEvery {
            syncTrigger.triggerPush(any(), any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val result = repository.insert(routine, emptyList())

        assertTrue("Local insert should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    @Test
    fun `insert returns error when DAO throws`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineDao.insertWithHabits(any<RoutineEntity>(), any<List<RoutineHabitEntity>>())
        } throws RuntimeException("DB write failed")

        val result = repository.insert(routine, emptyList())

        assertTrue(result is Result.Error)
    }

    // ---- update ----

    @Test
    fun `update triggers sync push when user is signed in`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            routineDao.update(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(routine)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "ROUTINE", routine.id.toString(), routine)
        }
    }

    @Test
    fun `update does not trigger sync when user is not signed in`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineDao.update(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } just Runs

        val result = repository.update(routine)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `update returns error when DAO throws`() = testScope.runTest {
        val routine = HabitFactory.routine()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            routineDao.update(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("DB failure")

        val result = repository.update(routine)

        assertTrue(result is Result.Error)
    }

    // ---- delete ----

    @Test
    fun `delete delegates to DAO and returns success`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { routineDao.delete(id) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { routineDao.delete(id) }
    }

    @Test
    fun `delete triggers sync deletion when user is signed in`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { routineDao.delete(id) } just Runs
        coEvery { syncTrigger.triggerDeletion(any(), any(), any()) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerDeletion(userId, "ROUTINE", id.toString())
        }
    }

    @Test
    fun `delete does not trigger sync when user is not signed in`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns null
        coEvery { routineDao.delete(id) } just Runs

        val result = repository.delete(id)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerDeletion(any(), any(), any()) }
    }

    @Test
    fun `delete succeeds even when sync deletion throws exception`() = testScope.runTest {
        val id = UUID.randomUUID()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { routineDao.delete(id) } just Runs
        coEvery {
            syncTrigger.triggerDeletion(any(), any(), any())
        } throws RuntimeException("Network error")

        val result = repository.delete(id)

        assertTrue("Local delete should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    // ---- getById ----

    private fun testRoutineEntity(id: UUID = HabitFactory.DEFAULT_ROUTINE_ID) = RoutineEntity(
        id = id,
        name = "Morning Routine",
        category = HabitCategory.Morning,
        status = RoutineStatus.Active,
        createdAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
        updatedAt = HabitFactory.DEFAULT_INSTANT.toEpochMilli(),
    )

    @Test
    fun `getById returns routine when found`() = testScope.runTest {
        val id = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery { routineDao.getById(id) } returns testRoutineEntity(id)

        val result = repository.getById(id)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `getById returns null when not found`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineDao.getById(id) } returns null

        val result = repository.getById(id)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `getById returns error when DAO throws`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineDao.getById(id) } throws RuntimeException("DB failure")

        val result = repository.getById(id)

        assertTrue(result is Result.Error)
    }

    // ---- getActiveRoutines ----

    @Test
    fun `getActiveRoutines returns list from DAO`() = testScope.runTest {
        coEvery { routineDao.getActiveRoutines() } returns emptyList()

        val result = repository.getActiveRoutines()

        assertTrue(result is Result.Success)
        assertEquals(emptyList<Any>(), (result as Result.Success).value)
    }

    @Test
    fun `getActiveRoutines returns error when DAO throws`() = testScope.runTest {
        coEvery { routineDao.getActiveRoutines() } throws RuntimeException("DB failure")

        val result = repository.getActiveRoutines()

        assertTrue(result is Result.Error)
    }

    // ---- getVariantsForRoutine ----

    @Test
    fun `getVariantsForRoutine returns variants from DAO`() = testScope.runTest {
        val routineId = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery { routineVariantDao.getByRoutineId(routineId) } returns emptyList()

        val result = repository.getVariantsForRoutine(routineId)

        assertTrue(result is Result.Success)
        assertEquals(emptyList<Any>(), (result as Result.Success).value)
    }

    // ---- getRoutineWithHabits ----

    @Test
    fun `getRoutineWithHabits returns null when routine not found`() = testScope.runTest {
        val id = UUID.randomUUID()
        coEvery { routineDao.getById(id) } returns null

        val result = repository.getRoutineWithHabits(id)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `getRoutineWithHabits returns routine with habits when found`() = testScope.runTest {
        val id = HabitFactory.DEFAULT_ROUTINE_ID
        coEvery { routineDao.getById(id) } returns testRoutineEntity(id)
        coEvery { routineHabitDao.getByRoutineId(id) } returns emptyList()

        val result = repository.getRoutineWithHabits(id)

        assertTrue(result is Result.Success)
    }
}
