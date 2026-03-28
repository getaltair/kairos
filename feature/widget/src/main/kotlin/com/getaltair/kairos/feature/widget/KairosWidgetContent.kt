package com.getaltair.kairos.feature.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.model.HabitWithStatus

/** Maximum number of habits displayed in the widget list. */
internal const val MAX_WIDGET_HABITS = 5

/**
 * Root composable for the Kairos home-screen widget.
 *
 * Displays three possible states:
 * 1. Empty -- no habits configured
 * 2. All done -- every habit completed
 * 3. Normal -- header with progress, list of up to [MAX_WIDGET_HABITS] habits
 */
@Composable
internal fun KairosWidgetContent(habits: List<HabitWithStatus>) {
    val sorted = sortByCategory(habits)
    val completedCount = countCompleted(sorted)
    val total = sorted.size
    val progress = computeProgress(completedCount, total)
    val allDone = total > 0 && completedCount == total

    val context = LocalContext.current
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent()

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity(launchIntent))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            when {
                total == 0 -> EmptyState()
                allDone -> AllDoneState(total)
                else -> NormalState(sorted, completedCount, total, progress)
            }
        }
    }
}

// -- States ------------------------------------------------------------------

@Composable
private fun EmptyState() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No habits yet",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Tap to get started",
            style = TextStyle(
                color = GlanceTheme.colors.secondary,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun AllDoneState(total: Int) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\u2728",
            style = TextStyle(fontSize = 24.sp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "All $total habits done!",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun NormalState(habits: List<HabitWithStatus>, completedCount: Int, total: Int, progress: Float) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Header
        Text(
            text = "Kairos \u2014 $completedCount/$total done",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Progress bar
        ProgressBar(progress)

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Habit rows (max 5)
        val display = habits.take(MAX_WIDGET_HABITS)
        display.forEach { habitWithStatus ->
            HabitRow(habitWithStatus)
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
    }
}

// -- Components --------------------------------------------------------------

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(6.dp)
            .cornerRadius(3.dp)
            .background(GlanceTheme.colors.secondaryContainer)
    ) {
        if (progress > 0f) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier
                        .height(6.dp)
                        .width((progress * 200).dp) // approximate visual width
                        .cornerRadius(3.dp)
                        .background(GlanceTheme.colors.primary)
                ) {}
            }
        }
    }
}

@Composable
private fun HabitRow(habitWithStatus: HabitWithStatus) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusIcon(habitWithStatus.todayCompletion?.type),
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = habitWithStatus.habit.name,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp
            ),
            maxLines = 1
        )
    }
}

// -- Pure helpers (visible for testing) --------------------------------------

/** Returns a status icon character based on the completion type. */
internal fun statusIcon(type: CompletionType?): String = when (type) {
    is CompletionType.Full -> "\u2713"

    // check mark
    is CompletionType.Partial -> "\u25D1"

    // half circle
    is CompletionType.Skipped -> "\u2298"

    // circled slash
    is CompletionType.Missed -> "\u2717"

    // ballot x
    null -> "\u25CB" // empty circle (pending)
}

/** Category sort order: Morning -> Afternoon -> Evening -> Anytime. */
internal fun categoryOrder(category: HabitCategory): Int = when (category) {
    is HabitCategory.Morning -> 0
    is HabitCategory.Afternoon -> 1
    is HabitCategory.Evening -> 2
    is HabitCategory.Anytime -> 3
    is HabitCategory.Departure -> 4
}

/** Sorts habits by category order. */
internal fun sortByCategory(habits: List<HabitWithStatus>): List<HabitWithStatus> =
    habits.sortedBy { categoryOrder(it.habit.category) }

/** Counts habits that have a Full completion. */
internal fun countCompleted(habits: List<HabitWithStatus>): Int =
    habits.count { it.todayCompletion?.type is CompletionType.Full }

/** Computes progress fraction (0.0 to 1.0). */
internal fun computeProgress(completedCount: Int, total: Int): Float =
    if (total == 0) 0f else completedCount.toFloat() / total.toFloat()
