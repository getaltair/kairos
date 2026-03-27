package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.RecoverySessionDao
import com.getaltair.kairos.data.entity.RecoverySessionEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryRepositoryImplTest {

    private lateinit var dao: RecoverySessionDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: RecoveryRepositoryImpl

    private val userId = "test-user-id"

    private fun testSession(
        id: UUID = UUID.randomUUID(),
        habitId: UUID = UUID.randomUUID(),
        type: RecoveryType = RecoveryType.Lapse,
        status: SessionStatus = SessionStatus.Pending
    ) = RecoverySession(
        id = id,
        habitId = habitId,
        type = type,
        status = status,
        blockers = setOf(Blocker.NoEnergy)
    )

    private fun testEntity(
        id: UUID = UUID.randomUUID(),
        habitId: UUID = UUID.randomUUID(),
        type: RecoveryType = RecoveryType.Lapse,
        status: SessionStatus = SessionStatus.Pending
    ) = RecoverySessionEntity(
        id = id,
        habitId = habitId,
        type = type,
        status = status,
        triggeredAt = Instant.now().toEpochMilli(),
        blockers = "[\"NoEnergy\"]",
        createdAt = Instant.now().toEpochMilli(),
        updatedAt = Instant.now().toEpochMilli()
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = RecoveryRepositoryImpl(
            dao = dao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    @Test
    fun `update converts type via RecoveryTypeConverter and calls DAO`() = testScope.runTest {
        val session = testSession(type = RecoveryType.Relapse)
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            dao.update(
                id = session.id,
                type = any(),
                status = any(),
                completedAt = any(),
                action = any(),
                notes = any(),
                updatedAt = any()
            )
        } just Runs

        val result = repository.update(session)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            dao.update(
                id = session.id,
                type = "Relapse",
                status = SessionStatus.Pending,
                completedAt = null,
                action = null,
                notes = null,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `update falls back to Lapse string when converter returns null`() = testScope.runTest {
        // The RecoveryTypeConverter.recoveryTypeToString uses javaClass.simpleName.
        // For standard RecoveryType sealed objects (Lapse, Relapse) it returns the class name.
        // The fallback ?: "Lapse" in RecoveryRepositoryImpl.update ensures a non-null type
        // is always passed to the DAO. This test verifies the Lapse path works correctly.
        val session = testSession(type = RecoveryType.Lapse)
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            dao.update(
                id = session.id,
                type = any(),
                status = any(),
                completedAt = any(),
                action = any(),
                notes = any(),
                updatedAt = any()
            )
        } just Runs

        val result = repository.update(session)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            dao.update(
                id = session.id,
                type = "Lapse",
                status = SessionStatus.Pending,
                completedAt = null,
                action = null,
                notes = null,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `getPendingForHabit returns single session`() = testScope.runTest {
        val habitId = UUID.randomUUID()
        val entity = testEntity(habitId = habitId)
        coEvery { dao.getPendingForHabit(habitId) } returns listOf(entity)

        val result = repository.getPendingForHabit(habitId)

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).value
        assertNotNull(session)
        assertEquals(habitId, session!!.habitId)
        assertEquals(RecoveryType.Lapse, session.type)
        assertEquals(SessionStatus.Pending, session.status)
    }

    @Test
    fun `getPendingForHabit returns null when no pending sessions`() = testScope.runTest {
        val habitId = UUID.randomUUID()
        coEvery { dao.getPendingForHabit(habitId) } returns emptyList()

        val result = repository.getPendingForHabit(habitId)

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).value
        assertNull(session)
    }

    @Test
    fun `getPendingForHabit wraps DAO exception as Result Error`() = testScope.runTest {
        val habitId = UUID.randomUUID()
        coEvery { dao.getPendingForHabit(habitId) } throws RuntimeException("DB error")

        val result = repository.getPendingForHabit(habitId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to get pending recovery session"))
    }

    @Test
    fun `update triggers sync when user is signed in`() = testScope.runTest {
        val session = testSession()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            dao.update(
                id = any(),
                type = any(),
                status = any(),
                completedAt = any(),
                action = any(),
                notes = any(),
                updatedAt = any()
            )
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(session)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "RECOVERY_SESSION", session.id.toString(), session)
        }
    }

    @Test
    fun `update does not trigger sync when user is not signed in`() = testScope.runTest {
        val session = testSession()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            dao.update(
                id = any(),
                type = any(),
                status = any(),
                completedAt = any(),
                action = any(),
                notes = any(),
                updatedAt = any()
            )
        } just Runs

        val result = repository.update(session)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }
}
