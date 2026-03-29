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

class SignUpUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: SignUpUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = SignUpUseCase(authRepository)
    }

    @Test
    fun `invoke returns success when sign up succeeds`() = runTest {
        // given
        coEvery {
            authRepository.signUp("new@test.com", "StrongPass1!")
        } returns Result.Success(Unit)

        // when
        val result = useCase("new@test.com", "StrongPass1!")

        // then
        assertTrue(result.isSuccess)
        assertEquals(Unit, (result as Result.Success).value)
        coVerify(exactly = 1) { authRepository.signUp("new@test.com", "StrongPass1!") }
    }

    @Test
    fun `invoke returns error when email already exists`() = runTest {
        // given
        coEvery {
            authRepository.signUp("existing@test.com", "StrongPass1!")
        } returns Result.Error("Email already in use")

        // when
        val result = useCase("existing@test.com", "StrongPass1!")

        // then
        assertTrue(result.isError)
        assertEquals("Email already in use", (result as Result.Error).message)
    }

    @Test
    fun `invoke returns error when password is weak`() = runTest {
        // given
        coEvery {
            authRepository.signUp("new@test.com", "123")
        } returns Result.Error("Password too weak")

        // when
        val result = useCase("new@test.com", "123")

        // then
        assertTrue(result.isError)
        assertEquals("Password too weak", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery {
            authRepository.signUp(any(), any())
        } throws RuntimeException("Connection refused")

        // when
        val result = useCase("new@test.com", "StrongPass1!")

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to sign up"))
        assertTrue(result.message.contains("Connection refused"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery {
            authRepository.signUp(any(), any())
        } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase("new@test.com", "StrongPass1!")
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
