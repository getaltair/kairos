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

class ResetPasswordUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: ResetPasswordUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = ResetPasswordUseCase(authRepository)
    }

    @Test
    fun `invoke returns success when reset email is sent`() = runTest {
        // given
        coEvery { authRepository.resetPassword("user@test.com") } returns Result.Success(Unit)

        // when
        val result = useCase("user@test.com")

        // then
        assertTrue(result.isSuccess)
        assertEquals(Unit, (result as Result.Success).value)
        coVerify(exactly = 1) { authRepository.resetPassword("user@test.com") }
    }

    @Test
    fun `invoke returns error when email is unknown`() = runTest {
        // given
        coEvery {
            authRepository.resetPassword("unknown@test.com")
        } returns Result.Error("No account found for this email")

        // when
        val result = useCase("unknown@test.com")

        // then
        assertTrue(result.isError)
        assertEquals("No account found for this email", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery {
            authRepository.resetPassword(any())
        } throws RuntimeException("SMTP failure")

        // when
        val result = useCase("user@test.com")

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to reset password"))
        assertTrue(result.message.contains("SMTP failure"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery {
            authRepository.resetPassword(any())
        } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase("user@test.com")
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
