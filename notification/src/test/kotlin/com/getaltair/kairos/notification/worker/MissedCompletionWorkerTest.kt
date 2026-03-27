package com.getaltair.kairos.notification.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.getaltair.kairos.domain.common.Result as DomainResult
import com.getaltair.kairos.domain.usecase.CreateMissedCompletionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

/**
 * Unit tests for [MissedCompletionWorker].
 *
 * Verifies the worker calls [CreateMissedCompletionsUseCase] and maps
 * domain results to WorkManager results correctly.
 */
class MissedCompletionWorkerTest : KoinTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockParams: WorkerParameters = mockk(relaxed = true) {
        every { runAttemptCount } returns 0
    }
    private val mockUseCase: CreateMissedCompletionsUseCase = mockk()

    private fun setupKoin() {
        stopKoin() // clean up any prior test state
        startKoin {
            modules(
                module {
                    single { mockUseCase }
                }
            )
        }
    }

    @Test
    fun `doWork calls CreateMissedCompletionsUseCase and returns success`() = runTest {
        setupKoin()
        coEvery { mockUseCase.invoke(any()) } returns DomainResult.Success(2)

        val worker = MissedCompletionWorker(mockContext, mockParams)
        val result = worker.doWork()

        coVerify(exactly = 1) { mockUseCase.invoke(any()) }
        assertEquals(ListenableWorker.Result.success(), result)
        stopKoin()
    }

    @Test
    fun `doWork returns retry on error with retries remaining`() = runTest {
        setupKoin()
        coEvery { mockUseCase.invoke(any()) } returns DomainResult.Error("db error")

        val worker = MissedCompletionWorker(mockContext, mockParams)
        val result = worker.doWork()

        coVerify(exactly = 1) { mockUseCase.invoke(any()) }
        assertEquals(ListenableWorker.Result.retry(), result)
        stopKoin()
    }

    @Test
    fun `doWork returns failure when max retries exceeded`() = runTest {
        val exhaustedParams: WorkerParameters = mockk(relaxed = true) {
            every { runAttemptCount } returns 3
        }
        setupKoin()
        coEvery { mockUseCase.invoke(any()) } returns DomainResult.Error("persistent error")

        val worker = MissedCompletionWorker(mockContext, exhaustedParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        stopKoin()
    }
}
