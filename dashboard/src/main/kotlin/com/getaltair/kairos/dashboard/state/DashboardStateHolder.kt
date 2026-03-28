package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.dashboard.data.AdminFirestoreMapper
import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Owns the [DashboardState] and coordinates Firestore snapshot collection.
 *
 * Responsibilities:
 * - Collects habits (active-only) and completions (for today) via
 *   [FirebaseAdminClient] flows and merges updates into a single
 *   [StateFlow].
 * - Handles midnight rollover: when the date changes, the completions
 *   listener is restarted for the new day so the dashboard resets
 *   automatically.
 */
class DashboardStateHolder(private val client: FirebaseAdminClient, private val userId: String,) {
    private val log = LoggerFactory.getLogger(DashboardStateHolder::class.java)

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.error("Uncaught coroutine exception", throwable)
        _state.update { it.copy(connectionStatus = ConnectionStatus.Offline) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var started = false
    private var currentDate: LocalDate = LocalDate.now()
    private var completionsJob: Job? = null

    /**
     * Starts observing Firestore.
     * Call once after [FirebaseAdminClient.initialize].
     */
    fun start() {
        check(!started) { "DashboardStateHolder.start() must be called exactly once" }
        started = true
        log.info("Starting dashboard state collection for user {}", userId)
        collectHabits()
        collectCompletions(currentDate)
        startMidnightWatcher()
    }

    // ----- internal collection plumbing ------------------------------------

    private fun collectHabits() {
        scope.launch {
            try {
                client.habitsFlow(userId).collect { habits ->
                    _state.update {
                        it.copy(
                            habits = habits,
                            connectionStatus = ConnectionStatus.Connected,
                            lastUpdated = Instant.now(),
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("Habits collection failed", e)
                _state.update { it.copy(connectionStatus = ConnectionStatus.Offline) }
            }
        }
    }

    private fun collectCompletions(date: LocalDate) {
        completionsJob?.cancel()
        completionsJob = scope.launch {
            try {
                client.completionsFlow(userId, date).collect { completions ->
                    _state.update {
                        it.copy(
                            completions = completions,
                            connectionStatus = ConnectionStatus.Connected,
                            lastUpdated = Instant.now(),
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("Completions collection failed", e)
                _state.update { it.copy(connectionStatus = ConnectionStatus.Offline) }
            }
        }
    }

    /**
     * Checks every 60 seconds whether the date has rolled over.
     * When it has, restarts the completions listener for the new day and
     * clears stale completion data from the previous day.
     */
    private fun startMidnightWatcher() {
        scope.launch {
            while (true) {
                delay(60_000)
                val today = LocalDate.now()
                if (today != currentDate) {
                    log.info("Date rolled over from {} to {}", currentDate, today)
                    currentDate = today
                    // Clear yesterday's completions immediately
                    _state.update { it.copy(completions = emptyList()) }
                    collectCompletions(today)
                }
            }
        }
    }

    // ----- public actions ---------------------------------------------------

    /** Switches the dashboard between [DisplayMode.Active] and [DisplayMode.Standby]. */
    fun setDisplayMode(mode: DisplayMode) {
        _state.update { it.copy(displayMode = mode) }
        log.info("Display mode changed to: {}", mode)
    }

    /**
     * Records a full completion for the given [habitId].
     *
     * Applies an optimistic update to the local state immediately, then
     * writes to Firestore in the background. If the write fails the
     * optimistic completion is rolled back.
     */
    fun completeHabit(habitId: UUID) {
        // Guard: already completed today
        if (_state.value.completedHabitIds.contains(habitId)) {
            log.debug("Habit {} already completed today, ignoring", habitId)
            return
        }

        // Build completion
        val completionId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()
        val completion = Completion(
            id = completionId,
            habitId = habitId,
            date = today,
            completedAt = now,
            type = CompletionType.Full,
            partialPercent = null,
            skipReason = null,
            energyLevel = null,
            note = null,
            createdAt = now,
            updatedAt = now,
        )

        // Optimistic update
        _state.update { it.copy(completions = it.completions + completion) }

        // Write to Firestore
        scope.launch(Dispatchers.IO) {
            val map = AdminFirestoreMapper.completionToMap(completionId.toString(), completion)
            val result = client.writeCompletion(userId, completionId.toString(), map)
            result.onFailure { e ->
                log.error("Failed to write completion for habit {}, rolling back", habitId, e)
                _state.update { st ->
                    st.copy(completions = st.completions.filter { c -> c.id != completionId })
                }
            }
        }
    }

    /** Tears down coroutines and the underlying Firestore client. */
    fun close() {
        log.info("Closing dashboard state holder")
        scope.coroutineContext[Job]?.cancel()
        client.close()
    }
}
