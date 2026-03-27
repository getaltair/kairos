package com.getaltair.kairos.dashboard.state

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import java.time.Duration
import java.time.Instant

/**
 * Connection status for the Firestore listener.
 */
enum class ConnectionStatus {
    Connected,
    Connecting,
    Offline,
}

/**
 * Immutable snapshot of dashboard state.
 *
 * Derived properties are computed lazily from the raw [habits] and [completions] lists
 * so the data layer only needs to push two flat collections.
 */
data class DashboardState(
    val habits: List<Habit> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connecting,
    val lastUpdated: Instant? = null,
) {
    /** Habits in the Departure category (shown in the "Don't Forget" panel). */
    val departureHabits: List<Habit>
        get() = habits.filter { it.isDeparture }

    /** All non-departure habits grouped by their [HabitCategory]. */
    val habitsByCategory: Map<HabitCategory, List<Habit>>
        get() = habits
            .filterNot { it.isDeparture }
            .groupBy { it.category }

    /** Set of habit IDs that have a completion record for today. */
    val completedHabitIds: Set<String>
        get() = completions.map { it.habitId.toString() }.toSet()

    /** Next three pending (not yet completed) non-departure habits. */
    val comingUpHabits: List<Habit>
        get() = habits
            .filterNot { it.isDeparture }
            .filter { it.id.toString() !in completedHabitIds }
            .take(3)

    /** Total number of trackable habits (all categories). */
    val totalHabits: Int
        get() = habits.size

    /** Number of habits completed today. */
    val completedCount: Int
        get() = completions.size

    /** True when the last snapshot is older than 2 minutes. */
    val isStale: Boolean
        get() {
            val last = lastUpdated ?: return false
            return Duration.between(last, Instant.now()).toMinutes() >= 2
        }
}
