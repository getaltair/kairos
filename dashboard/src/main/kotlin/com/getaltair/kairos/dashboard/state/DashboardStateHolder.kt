package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import java.time.Instant
import java.time.LocalDate
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var currentDate: LocalDate = LocalDate.now()
    private var completionsJob: Job? = null

    /**
     * Starts observing Firestore.
     * Call once after [FirebaseAdminClient.initialize].
     */
    fun start() {
        log.info("Starting dashboard state collection for user {}", userId)
        collectHabits()
        collectCompletions(currentDate)
        startMidnightWatcher()
    }

    // ----- internal collection plumbing ------------------------------------

    private fun collectHabits() {
        scope.launch {
            client.habitsFlow(userId).collect { habits ->
                _state.update {
                    it.copy(
                        habits = habits,
                        connectionStatus = ConnectionStatus.Connected,
                        lastUpdated = Instant.now(),
                    )
                }
            }
        }
    }

    private fun collectCompletions(date: LocalDate) {
        // Cancel any prior completions collector before starting a new one
        completionsJob?.cancel()
        completionsJob = scope.launch {
            client.completionsFlow(userId, date).collect { completions ->
                _state.update {
                    it.copy(
                        completions = completions,
                        connectionStatus = ConnectionStatus.Connected,
                        lastUpdated = Instant.now(),
                    )
                }
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

    /** Tears down coroutines and the underlying Firestore client. */
    fun close() {
        log.info("Closing dashboard state holder")
        scope.coroutineContext[Job]?.cancel()
        client.close()
    }
}
