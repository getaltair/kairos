package com.getaltair.kairos.dashboard.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.dashboard.state.DashboardState
import com.getaltair.kairos.dashboard.state.DisplayMode
import com.getaltair.kairos.dashboard.ui.components.ComingUpBar
import com.getaltair.kairos.dashboard.ui.components.DeparturePanel
import com.getaltair.kairos.dashboard.ui.components.HabitsPanel
import com.getaltair.kairos.dashboard.ui.components.HeaderBar
import com.getaltair.kairos.dashboard.ui.components.StandbyScreen
import com.getaltair.kairos.dashboard.ui.components.StatusIndicator
import java.util.UUID

/**
 * Root dashboard layout with Active/Standby mode switching.
 *
 * In **Active** mode the three-zone habit-tracking layout is shown:
 * ```
 * +---------------------------------------------------+
 * |  HeaderBar (full width, ~80dp)                     |
 * |  StatusIndicator (full width, conditional)         |
 * +------------------------+--------------------------+
 * |  DeparturePanel (35%)  |  HabitsPanel (65%)       |
 * +------------------------+--------------------------+
 * |  ComingUpBar (full width, ~80dp)                   |
 * +---------------------------------------------------+
 * ```
 *
 * In **Standby** mode a minimal clock screen is displayed to prevent
 * burn-in during idle periods.
 *
 * Tapping a habit row or departure item invokes [onComplete] with the
 * habit's UUID to record a completion.
 *
 * The optional [offset] parameter is driven by the screen-saver utility
 * to shift the entire layout by a few pixels and prevent burn-in.
 */
@Composable
fun DashboardScreen(
    state: DashboardState,
    onComplete: (UUID) -> Unit,
    offset: DpOffset = DpOffset.Zero,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state.displayMode,
        modifier = modifier.fillMaxSize(),
        label = "dashboardMode",
    ) { mode ->
        when (mode) {
            DisplayMode.Standby -> StandbyScreen(offset = offset)

            DisplayMode.Active -> ActiveDashboard(
                state = state,
                onComplete = onComplete,
                offset = offset,
            )
        }
    }
}

@Composable
private fun ActiveDashboard(state: DashboardState, onComplete: (UUID) -> Unit, offset: DpOffset,) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .offset(offset.x, offset.y),
    ) {
        HeaderBar(
            connectionStatus = state.connectionStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )

        StatusIndicator(
            lastUpdated = state.lastUpdated,
            isStale = state.isStale,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            DeparturePanel(
                departureHabits = state.departureHabits,
                completedHabitIds = state.completedHabitIds,
                onComplete = onComplete,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f)
                    .padding(8.dp),
            )

            HabitsPanel(
                habitsByCategory = state.habitsByCategory,
                completedHabitIds = state.completedHabitIds,
                completedCount = state.completedCount,
                totalHabits = state.totalHabits,
                onComplete = onComplete,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.65f)
                    .padding(8.dp),
            )
        }

        ComingUpBar(
            comingUpHabits = state.comingUpHabits,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )
    }
}
