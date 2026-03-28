package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Connection status for the Firestore listener.
 */
enum class ConnectionStatus {
    Connected,
    Connecting,
    Offline,
}

/**
 * Display mode for the dashboard UI.
 *
 * [Active] shows the full habit-tracking layout.
 * [Standby] shows a minimal clock screen to prevent burn-in during idle periods.
 */
enum class DisplayMode {
    Active,
    Standby;

    companion object {
        fun fromString(value: String): DisplayMode? = when (value.lowercase()) {
            "active" -> Active
            "standby" -> Standby
            else -> null
        }
    }
}

/**
 * Immutable snapshot of dashboard state.
 *
 * Derived properties are computed on access from the raw [habits] and [completions] lists
 * so the data layer only needs to push two flat collections.
 */
data class DashboardState(
    val habits: List<Habit> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connecting,
    val lastUpdated: Instant? = null,
    val displayMode: DisplayMode = DisplayMode.Active,
) {
    /** True when the dashboard is in standby (clock-only) mode. */
    val isStandby: Boolean
        get() = displayMode == DisplayMode.Standby

    /** Habits in the Departure category (shown in the "Don't Forget" panel). */
    val departureHabits: List<Habit>
        get() = habits.filter { it.isDeparture }

    /** All non-departure habits grouped by their [HabitCategory]. */
    val habitsByCategory: Map<HabitCategory, List<Habit>>
        get() = habits
            .filterNot { it.isDeparture }
            .groupBy { it.category }

    /** Set of habit IDs that have a completion record for today. */
    val completedHabitIds: Set<UUID>
        get() = completions.map { it.habitId }.toSet()

    /** Next [COMING_UP_LIMIT] pending (not yet completed) non-departure habits. */
    val comingUpHabits: List<Habit>
        get() = habits
            .filterNot { it.isDeparture }
            .filter { it.id !in completedHabitIds }
            .take(COMING_UP_LIMIT)

    /** Total number of trackable habits (all categories). */
    val totalHabits: Int
        get() = habits.size

    /** Number of distinct habits completed today. */
    val completedCount: Int
        get() = completedHabitIds.size

    /** True when the last snapshot is older than [STALE_THRESHOLD_MINUTES] or never received. */
    val isStale: Boolean
        get() {
            val last = lastUpdated ?: return true
            return Duration.between(last, Instant.now()).toMinutes() >= STALE_THRESHOLD_MINUTES
        }

    private companion object {
        const val STALE_THRESHOLD_MINUTES = 2L
        const val COMING_UP_LIMIT = 3
    }
}
