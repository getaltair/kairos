package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.AuthState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveAuthStateUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: ObserveAuthStateUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = ObserveAuthStateUseCase(authRepository)
    }

    @Test
    fun `invoke emits signed-in state`() = runTest {
        // given
        val signedIn = AuthState.SignedIn(userId = "user-123", email = "user@test.com")
        every { authRepository.observeAuthState() } returns flowOf(signedIn)

        // when
        val emissions = useCase().toList()

        // then
        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is AuthState.SignedIn)
        val state = emissions[0] as AuthState.SignedIn
        assertEquals("user-123", state.userId)
        assertEquals("user@test.com", state.email)
    }

    @Test
    fun `invoke emits signed-out state`() = runTest {
        // given
        every { authRepository.observeAuthState() } returns flowOf(AuthState.SignedOut)

        // when
        val emissions = useCase().toList()

        // then
        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is AuthState.SignedOut)
    }

    @Test
    fun `invoke emits multiple state transitions`() = runTest {
        // given
        val signedIn = AuthState.SignedIn(userId = "user-456", email = "user@test.com")
        every {
            authRepository.observeAuthState()
        } returns flowOf(AuthState.SignedOut, signedIn, AuthState.SignedOut)

        // when
        val emissions = useCase().toList()

        // then
        assertEquals(3, emissions.size)
        assertTrue(emissions[0] is AuthState.SignedOut)
        assertTrue(emissions[1] is AuthState.SignedIn)
        assertTrue(emissions[2] is AuthState.SignedOut)
        verify(exactly = 1) { authRepository.observeAuthState() }
    }

    @Test
    fun `invoke delegates directly to repository`() = runTest {
        // given
        val flow = flowOf(AuthState.SignedOut)
        every { authRepository.observeAuthState() } returns flow

        // when
        val result = useCase()

        // then -- the flow is the same object returned by the repository
        assertEquals(flow, result)
        verify(exactly = 1) { authRepository.observeAuthState() }
    }
}
