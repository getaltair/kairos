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

class SignInUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: SignInUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = SignInUseCase(authRepository)
    }

    @Test
    fun `invoke returns success when credentials are valid`() = runTest {
        // given
        coEvery { authRepository.signIn("user@test.com", "password123") } returns Result.Success(Unit)

        // when
        val result = useCase("user@test.com", "password123")

        // then
        assertTrue(result.isSuccess)
        assertEquals(Unit, (result as Result.Success).value)
        coVerify(exactly = 1) { authRepository.signIn("user@test.com", "password123") }
    }

    @Test
    fun `invoke returns error when repository returns error`() = runTest {
        // given
        coEvery {
            authRepository.signIn("user@test.com", "wrong")
        } returns Result.Error("Invalid credentials")

        // when
        val result = useCase("user@test.com", "wrong")

        // then
        assertTrue(result.isError)
        assertEquals("Invalid credentials", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery {
            authRepository.signIn(any(), any())
        } throws RuntimeException("Network error")

        // when
        val result = useCase("user@test.com", "password123")

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to sign in"))
        assertTrue(result.message.contains("Network error"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery {
            authRepository.signIn(any(), any())
        } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase("user@test.com", "password123")
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
