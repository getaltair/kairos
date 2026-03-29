package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignOutUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: SignOutUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = SignOutUseCase(authRepository)
    }

    @Test
    fun `invoke returns success when sign out succeeds`() = runTest {
        // given
        coEvery { authRepository.signOut() } returns Result.Success(Unit)

        // when
        val result = useCase()

        // then
        assertTrue(result.isSuccess)
        assertEquals(Unit, (result as Result.Success).value)
        coVerify(exactly = 1) { authRepository.signOut() }
    }

    @Test
    fun `invoke returns error when repository returns error`() = runTest {
        // given
        coEvery { authRepository.signOut() } returns Result.Error("No active session")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertEquals("No active session", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery { authRepository.signOut() } throws RuntimeException("Service unavailable")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to sign out"))
        assertTrue(result.message.contains("Service unavailable"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery { authRepository.signOut() } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase()
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
