package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.CompletionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoCompletionUseCaseTest {

    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: UndoCompletionUseCase

    @Before
    fun setup() {
        completionRepository = mockk()
        useCase = UndoCompletionUseCase(completionRepository)
    }

    @Test
    fun `successful deletion returns success`() = runTest {
        val completionId = UUID.randomUUID()
        coEvery { completionRepository.delete(completionId) } returns Result.Success(Unit)

        val result = useCase(completionId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionRepository.delete(completionId) }
    }

    @Test
    fun `repository delete failure returns error`() = runTest {
        val completionId = UUID.randomUUID()
        coEvery {
            completionRepository.delete(completionId)
        } throws RuntimeException("DB delete failed")

        val result = useCase(completionId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Failed to undo completion")
        )
    }
}
