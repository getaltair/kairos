package com.getaltair.kairos.wear.tile

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.getaltair.kairos.wear.data.WearDataRepository
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class HabitTileService : SuspendingTileService() {
    private val repository: WearDataRepository by inject()

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest,): TileBuilders.Tile {
        val tileState = computeTileState()
        val layout = TileRenderer.renderTile(tileState)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .setFreshnessIntervalMillis(15 * 60 * 1000L) // Refresh every 15 minutes
            .build()
    }

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ResourceBuilders.Resources = ResourceBuilders.Resources.Builder()
        .setVersion("1")
        .build()

    private suspend fun computeTileState(): TileState = try {
        val habits = repository.todayHabits.first()
        val completions = repository.todayCompletions.first()

        // DEPARTURE habits are device-specific triggers, not meant for manual tracking on the watch
        val watchHabits = habits.filter { it.category != "DEPARTURE" }

        when {
            watchHabits.isEmpty() -> TileState.Empty

            watchHabits.all { habit ->
                completions.any { it.habitId == habit.id }
            } -> TileState.AllDone

            else -> TileState.HasHabits(
                habits = watchHabits,
                completions = completions,
            )
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Error computing tile state")
        TileState.Error("Could not load habits")
    }
}
