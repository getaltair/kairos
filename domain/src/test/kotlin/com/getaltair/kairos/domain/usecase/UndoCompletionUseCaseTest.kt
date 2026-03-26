package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.CompletionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoCompletionUseCaseTest {

    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: UndoCompletionUseCase

    private val completionId = UUID.randomUUID()

    @Before
    fun setup() {
        completionRepository = mockk()
        useCase = UndoCompletionUseCase(completionRepository)
    }

    @Test
    fun `undo within 30 seconds succeeds`() = runTest {
        val completedAt = Instant.now().minusSeconds(10)
        coEvery { completionRepository.delete(completionId) } returns Result.Success(Unit)

        val result = useCase(completionId, completedAt)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionRepository.delete(completionId) }
    }

    @Test
    fun `undo at boundary succeeds`() = runTest {
        // Use 29 seconds to provide a 1-second buffer against test execution time,
        // avoiding flakiness from the wall-clock gap between minusSeconds and
        // the Instant.now() call inside the use case.
        val completedAt = Instant.now().minusSeconds(29)
        coEvery { completionRepository.delete(completionId) } returns Result.Success(Unit)

        val result = useCase(completionId, completedAt)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionRepository.delete(completionId) }
    }

    @Test
    fun `undo after 30 seconds returns error`() = runTest {
        val completedAt = Instant.now().minusSeconds(60)

        val result = useCase(completionId, completedAt)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Undo window has expired"))
        coVerify(exactly = 0) { completionRepository.delete(any()) }
    }

    @Test
    fun `repository error is propagated`() = runTest {
        val completedAt = Instant.now().minusSeconds(5)
        coEvery {
            completionRepository.delete(completionId)
        } returns Result.Error("Not found")

        val result = useCase(completionId, completedAt)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Not found"))
    }

    @Test
    fun `repository exception is caught`() = runTest {
        val completedAt = Instant.now().minusSeconds(5)
        coEvery {
            completionRepository.delete(completionId)
        } throws RuntimeException("IO error")

        val result = useCase(completionId, completedAt)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("IO error"))
    }
}
