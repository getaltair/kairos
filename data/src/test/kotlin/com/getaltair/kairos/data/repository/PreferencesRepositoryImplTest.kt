package com.getaltair.kairos.data.repository

import com.getaltair.kairos.data.dao.UserPreferencesDao
import com.getaltair.kairos.data.entity.UserPreferencesEntity
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryImplTest {

    private lateinit var userPreferencesDao: UserPreferencesDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var authRepository: AuthRepository
    private lateinit var testScope: TestScope
    private lateinit var repository: PreferencesRepositoryImpl

    private val userId = "test-user-id"

    @Before
    fun setup() {
        userPreferencesDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)
        authRepository = mockk()
        testScope = TestScope(UnconfinedTestDispatcher())
        repository = PreferencesRepositoryImpl(
            userPreferencesDao = userPreferencesDao,
            syncTrigger = syncTrigger,
            authRepository = authRepository,
            syncScope = testScope,
        )
    }

    // ---- get ----

    @Test
    fun `get returns existing preferences when found`() = testScope.runTest {
        val entity = mockk<UserPreferencesEntity>(relaxed = true)
        coEvery { userPreferencesDao.get() } returns entity

        val result = repository.get()

        assertTrue(result is Result.Success)
    }

    @Test
    fun `get creates default preferences when none exist`() = testScope.runTest {
        every { authRepository.getCurrentUserId() } returns null
        coEvery { userPreferencesDao.get() } returns null
        coEvery { userPreferencesDao.insert(any<UserPreferencesEntity>()) } just Runs

        val result = repository.get()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { userPreferencesDao.insert(any<UserPreferencesEntity>()) }
    }

    @Test
    fun `get creates default preferences and triggers sync when user is signed in`() = testScope.runTest {
        every { authRepository.getCurrentUserId() } returns userId
        coEvery { userPreferencesDao.get() } returns null
        coEvery { userPreferencesDao.insert(any<UserPreferencesEntity>()) } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.get()

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "USER_PREFERENCE", any(), any())
        }
    }

    @Test
    fun `get default preferences have expected default values`() = testScope.runTest {
        every { authRepository.getCurrentUserId() } returns null
        coEvery { userPreferencesDao.get() } returns null
        coEvery { userPreferencesDao.insert(any<UserPreferencesEntity>()) } just Runs

        val result = repository.get()

        assertTrue(result is Result.Success)
        val prefs = (result as Result.Success).value
        assertTrue(prefs.notificationEnabled)
        assertTrue(prefs.quietHoursEnabled)
        assertEquals(com.getaltair.kairos.domain.enums.Theme.System, prefs.theme)
    }

    @Test
    fun `get returns error when DAO throws`() = testScope.runTest {
        coEvery { userPreferencesDao.get() } throws RuntimeException("DB failure")

        val result = repository.get()

        assertTrue(result is Result.Error)
    }

    // ---- update ----

    @Test
    fun `update delegates to DAO and returns success`() = testScope.runTest {
        val preferences = HabitFactory.userPreferences()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            userPreferencesDao.update(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } just Runs

        val result = repository.update(preferences)

        assertTrue(result is Result.Success)
        assertEquals(preferences, (result as Result.Success).value)
    }

    @Test
    fun `update triggers sync push when user is signed in`() = testScope.runTest {
        val preferences = HabitFactory.userPreferences()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            userPreferencesDao.update(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } just Runs
        coEvery { syncTrigger.triggerPush(any(), any(), any(), any()) } just Runs

        val result = repository.update(preferences)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            syncTrigger.triggerPush(userId, "USER_PREFERENCE", preferences.id.toString(), preferences)
        }
    }

    @Test
    fun `update does not trigger sync when user is not signed in`() = testScope.runTest {
        val preferences = HabitFactory.userPreferences()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            userPreferencesDao.update(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } just Runs

        val result = repository.update(preferences)

        assertTrue(result is Result.Success)
        advanceUntilIdle()
        coVerify(exactly = 0) { syncTrigger.triggerPush(any(), any(), any(), any()) }
    }

    @Test
    fun `update succeeds even when sync throws exception`() = testScope.runTest {
        val preferences = HabitFactory.userPreferences()
        every { authRepository.getCurrentUserId() } returns userId
        coEvery {
            userPreferencesDao.update(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } just Runs
        coEvery {
            syncTrigger.triggerPush(any(), any(), any(), any())
        } throws RuntimeException("Firestore unavailable")

        val result = repository.update(preferences)

        assertTrue("Local update should succeed regardless of sync failure", result is Result.Success)
        advanceUntilIdle()
    }

    @Test
    fun `update returns error when DAO throws`() = testScope.runTest {
        val preferences = HabitFactory.userPreferences()
        every { authRepository.getCurrentUserId() } returns null
        coEvery {
            userPreferencesDao.update(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } throws RuntimeException("DB failure")

        val result = repository.update(preferences)

        assertTrue(result is Result.Error)
    }
}
