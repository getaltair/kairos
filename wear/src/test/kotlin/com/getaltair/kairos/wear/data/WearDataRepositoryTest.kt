package com.getaltair.kairos.wear.data

import com.getaltair.kairos.domain.wear.WearAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class WearDataRepositoryTest {
    private val dataClient =
        mockk<com.google.android.gms.wearable.DataClient>(relaxed = true)
    private val messageClient =
        mockk<com.google.android.gms.wearable.MessageClient>(relaxed = true)
    private val capabilityClient =
        mockk<com.google.android.gms.wearable.CapabilityClient>(relaxed = true)
    private val localCache = mockk<LocalCache>(relaxed = true)
    private val actionQueue = mockk<ActionQueue>(relaxed = true)

    private lateinit var repository: WearDataRepository

    @Before
    fun setUp() {
        repository = WearDataRepository(
            dataClient,
            messageClient,
            capabilityClient,
            localCache,
            actionQueue,
        )
    }

    @Test
    fun `completeHabit queues action when phone disconnected`() = runTest {
        repository.updatePhoneConnected(false)
        repository.completeHabit("habit-1", "FULL")
        coVerify {
            actionQueue.enqueue(
                WearAction.CompleteHabit("habit-1", "FULL", null),
            )
        }
    }

    @Test
    fun `skipHabit queues action when phone disconnected`() = runTest {
        repository.updatePhoneConnected(false)
        repository.skipHabit("habit-1", "not today")
        coVerify {
            actionQueue.enqueue(
                WearAction.SkipHabit("habit-1", "not today"),
            )
        }
    }

    @Test
    fun `startRoutine queues action when phone disconnected`() = runTest {
        repository.updatePhoneConnected(false)
        repository.startRoutine("routine-1")
        coVerify {
            actionQueue.enqueue(
                WearAction.StartRoutine("routine-1"),
            )
        }
    }

    @Test
    fun `advanceRoutineStep queues action when phone disconnected`() = runTest {
        repository.updatePhoneConnected(false)
        repository.advanceRoutineStep("exec-1")
        coVerify {
            actionQueue.enqueue(
                WearAction.AdvanceRoutineStep("exec-1"),
            )
        }
    }

    @Test
    fun `pauseRoutine queues action when phone disconnected`() = runTest {
        repository.updatePhoneConnected(false)
        repository.pauseRoutine("exec-1")
        coVerify {
            actionQueue.enqueue(
                WearAction.PauseRoutine("exec-1"),
            )
        }
    }

    @Test
    fun `flushQueue skips when queue is empty`() = runTest {
        coEvery { actionQueue.isEmpty() } returns true
        repository.flushQueue()
        coVerify(exactly = 0) { actionQueue.dequeueAll() }
    }

    @Test
    fun `flushQueue dequeues all when queue is not empty`() = runTest {
        val pendingActions = listOf(
            WearAction.CompleteHabit("h1", "FULL"),
            WearAction.SkipHabit("h2"),
        )
        coEvery { actionQueue.isEmpty() } returns false
        coEvery { actionQueue.dequeueAll() } returns pendingActions
        // Mock getCapability to throw so getPhoneNodeId returns null,
        // which causes flush to stop after dequeue without hanging.
        coEvery {
            capabilityClient.getCapability(any(), any())
        } throws RuntimeException("No phone node")
        repository.flushQueue()
        coVerify { actionQueue.dequeueAll() }
    }

    @Test
    fun `updatePhoneConnected updates state`() {
        repository.updatePhoneConnected(true)
        assert(repository.isPhoneConnected.value)
        repository.updatePhoneConnected(false)
        assert(!repository.isPhoneConnected.value)
    }
}
