package com.getaltair.kairos.wear.data

import com.getaltair.kairos.domain.wear.WearAction
import com.getaltair.kairos.domain.wear.WearDataPaths
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    // ------------------------------------------------------------------
    // Actions queue when phone disconnected
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Connected-path: messages sent via messageClient
    // ------------------------------------------------------------------

    @Test
    fun `completeHabit when phone connected sends message via messageClient with correct path`() = runTest {
        val node = mockk<Node>(relaxed = true)
        every { node.id } returns "phone-node-123"
        val capabilityInfo = mockk<CapabilityInfo>(relaxed = true)
        every { capabilityInfo.nodes } returns setOf(node)
        every {
            capabilityClient.getCapability(WearDataPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
        } returns Tasks.forResult(capabilityInfo)
        every {
            messageClient.sendMessage(any(), any(), any())
        } returns Tasks.forResult(0)

        repository.updatePhoneConnected(true)
        repository.completeHabit("habit-1", "FULL")

        coVerify {
            messageClient.sendMessage(
                "phone-node-123",
                WearDataPaths.MESSAGE_HABIT_COMPLETED,
                any(),
            )
        }
        coVerify(exactly = 0) { actionQueue.enqueue(any()) }
    }

    // ------------------------------------------------------------------
    // flushQueue behavior
    // ------------------------------------------------------------------

    @Test
    fun `flushQueue checks nodeId before dequeuing - when no phone nodeId returns without dequeuing`() = runTest {
        coEvery {
            capabilityClient.getCapability(any(), any())
        } throws RuntimeException("No phone node")
        repository.flushQueue()
        coVerify(exactly = 0) { actionQueue.dequeueAll() }
    }

    @Test
    fun `flushQueue skips when queue is empty after dequeue`() = runTest {
        val node = mockk<Node>(relaxed = true)
        every { node.id } returns "phone-node-123"
        val capabilityInfo = mockk<CapabilityInfo>(relaxed = true)
        every { capabilityInfo.nodes } returns setOf(node)
        every {
            capabilityClient.getCapability(WearDataPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
        } returns Tasks.forResult(capabilityInfo)
        coEvery { actionQueue.dequeueAll() } returns emptyList()

        repository.flushQueue()

        coVerify { actionQueue.dequeueAll() }
        coVerify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `flushQueue re-enqueues failed sends`() = runTest {
        val node = mockk<Node>(relaxed = true)
        every { node.id } returns "phone-node-123"
        val capabilityInfo = mockk<CapabilityInfo>(relaxed = true)
        every { capabilityInfo.nodes } returns setOf(node)
        every {
            capabilityClient.getCapability(WearDataPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
        } returns Tasks.forResult(capabilityInfo)

        val successAction = WearAction.CompleteHabit("h1", "FULL")
        val failAction = WearAction.SkipHabit("h2")
        coEvery { actionQueue.dequeueAll() } returns listOf(successAction, failAction)

        // First sendMessage succeeds, second throws
        every {
            messageClient.sendMessage(
                "phone-node-123",
                WearDataPaths.MESSAGE_HABIT_COMPLETED,
                any(),
            )
        } returns Tasks.forResult(0)
        every {
            messageClient.sendMessage(
                "phone-node-123",
                WearDataPaths.MESSAGE_HABIT_SKIPPED,
                any(),
            )
        } throws RuntimeException("Network error")

        repository.flushQueue()

        // The failed action should be re-enqueued
        coVerify { actionQueue.enqueue(failAction) }
        // The successful action should NOT be re-enqueued
        coVerify(exactly = 0) { actionQueue.enqueue(successAction) }
    }

    // ------------------------------------------------------------------
    // State management
    // ------------------------------------------------------------------

    @Test
    fun `updatePhoneConnected updates state`() {
        repository.updatePhoneConnected(true)
        assert(repository.isPhoneConnected.value)
        repository.updatePhoneConnected(false)
        assert(!repository.isPhoneConnected.value)
    }
}
