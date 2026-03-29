package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.DataCleanup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var dataCleanup: DataCleanup
    private lateinit var useCase: DeleteAccountUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        dataCleanup = mockk()
        useCase = DeleteAccountUseCase(authRepository, dataCleanup)
    }

    // -------------------------------------------------------------------------
    // 1. Happy path: all steps succeed, Result.Success returned
    // -------------------------------------------------------------------------

    @Test
    fun `happy path deletes all data and returns success`() = runTest {
        every { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { authRepository.reauthenticate("password") } returns Result.Success(Unit)
        every { dataCleanup.stopSyncListeners() } just runs
        coEvery { dataCleanup.deleteCloudData("user-123") } just runs
        coEvery { authRepository.deleteAccount("password") } returns Result.Success(Unit)
        every { dataCleanup.clearLocalData() } just runs

        val result = useCase("password")

        assertTrue(result is Result.Success)

        // Verify all steps were called in expected order
        verify(exactly = 1) { authRepository.getCurrentUserId() }
        coVerify(exactly = 1) { authRepository.reauthenticate("password") }
        verify(exactly = 1) { dataCleanup.stopSyncListeners() }
        coVerify(exactly = 1) { dataCleanup.deleteCloudData("user-123") }
        coVerify(exactly = 1) { authRepository.deleteAccount("password") }
        verify(exactly = 1) { dataCleanup.clearLocalData() }
    }

    // -------------------------------------------------------------------------
    // 2. Not signed in: returns error immediately
    // -------------------------------------------------------------------------

    @Test
    fun `returns error when user is not signed in (null userId)`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        val result = useCase("password")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("No user is currently signed in"))

        // Nothing else should be called
        coVerify(exactly = 0) { authRepository.reauthenticate(any()) }
        verify(exactly = 0) { dataCleanup.stopSyncListeners() }
        coVerify(exactly = 0) { dataCleanup.deleteCloudData(any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount(any()) }
        verify(exactly = 0) { dataCleanup.clearLocalData() }
    }

    @Test
    fun `returns error when user is not signed in (empty userId)`() = runTest {
        every { authRepository.getCurrentUserId() } returns ""

        val result = useCase("password")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("No user is currently signed in"))

        // Nothing else should be called
        coVerify(exactly = 0) { authRepository.reauthenticate(any()) }
        verify(exactly = 0) { dataCleanup.stopSyncListeners() }
    }

    // -------------------------------------------------------------------------
    // 3. Re-auth failure: returns error, no data deleted
    // -------------------------------------------------------------------------

    @Test
    fun `reauth failure returns error without deleting any data`() = runTest {
        every { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { authRepository.reauthenticate("wrong-password") } returns
            Result.Error("Incorrect password")

        val result = useCase("wrong-password")

        assertTrue(result is Result.Error)
        assertEquals("Incorrect password", (result as Result.Error).message)

        // Data operations must NOT be called
        verify(exactly = 0) { dataCleanup.stopSyncListeners() }
        coVerify(exactly = 0) { dataCleanup.deleteCloudData(any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount(any()) }
        verify(exactly = 0) { dataCleanup.clearLocalData() }
    }

    // -------------------------------------------------------------------------
    // 4. Firestore deletion failure: returns error
    // -------------------------------------------------------------------------

    @Test
    fun `cloud data deletion failure returns error`() = runTest {
        every { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { authRepository.reauthenticate("password") } returns Result.Success(Unit)
        every { dataCleanup.stopSyncListeners() } just runs
        coEvery { dataCleanup.deleteCloudData("user-123") } throws
            RuntimeException("Firestore timeout")

        val result = useCase("password")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Account deletion failed"))
        assertTrue(error.message.contains("Firestore timeout"))

        // Auth deletion and local cleanup should NOT proceed
        coVerify(exactly = 0) { authRepository.deleteAccount(any()) }
        verify(exactly = 0) { dataCleanup.clearLocalData() }
    }

    // -------------------------------------------------------------------------
    // 5. Auth deletion failure after cloud data deleted: returns error
    // -------------------------------------------------------------------------

    @Test
    fun `auth deletion failure returns error`() = runTest {
        every { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { authRepository.reauthenticate("password") } returns Result.Success(Unit)
        every { dataCleanup.stopSyncListeners() } just runs
        coEvery { dataCleanup.deleteCloudData("user-123") } just runs
        coEvery { authRepository.deleteAccount("password") } returns
            Result.Error("Firebase Auth error")

        val result = useCase("password")

        assertTrue(result is Result.Error)
        assertEquals("Firebase Auth error", (result as Result.Error).message)

        // Cloud data was already deleted (acceptable), but local data should NOT be cleared
        verify(exactly = 0) { dataCleanup.clearLocalData() }
    }

    // -------------------------------------------------------------------------
    // 6. CancellationException is rethrown, not wrapped
    // -------------------------------------------------------------------------

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        every { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { authRepository.reauthenticate("password") } returns Result.Success(Unit)
        every { dataCleanup.stopSyncListeners() } just runs
        coEvery { dataCleanup.deleteCloudData("user-123") } throws
            kotlinx.coroutines.CancellationException("Job cancelled")

        useCase("password")
    }
}
