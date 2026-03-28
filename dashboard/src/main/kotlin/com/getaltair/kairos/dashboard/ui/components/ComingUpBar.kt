package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows the current time followed by upcoming habit names with anchor
 * context, time windows, and estimated durations, designed for
 * readability at 3-4 ft.
 */
@Composable
fun ComingUpBar(comingUpHabits: List<Habit>, modifier: Modifier = Modifier,) {
    val now = rememberCurrentTime()

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Prominent current time on the left
        Text(
            text = now.format(timeFormatter),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(24.dp))

        if (comingUpHabits.isEmpty()) {
            Text(
                text = "\u2713 All habits complete for today!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append("COMING UP:  ")
                    }
                    comingUpHabits.forEachIndexed { index, habit ->
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                            append(habit.name)
                            val context = formatAnchorContext(habit)
                            if (context != null) {
                                append(" ($context)")
                            }
                            val duration = formatEstimatedDuration(habit)
                            if (duration != null) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append("  $duration")
                                }
                            }
                        }
                        if (index < comingUpHabits.lastIndex) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append("  \u2022  ")
                            }
                        }
                    }
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * Builds a short anchor context string.
 *
 * For AT_TIME habits with a [Habit.timeWindowStart], displays the formatted
 * time (e.g. "At 9:00 AM"). Otherwise falls back to the anchor behavior
 * string (e.g. "After brushing teeth").
 */
private fun formatAnchorContext(habit: Habit): String? {
    val timeStart = habit.timeWindowStart
    if (habit.anchorType is AnchorType.AtTime && !timeStart.isNullOrBlank()) {
        val formatted = formatTimeWindow(timeStart)
        if (formatted != null) return "At $formatted"
    }
    val prefix = when (habit.anchorType) {
        is AnchorType.AfterBehavior -> "After"
        is AnchorType.BeforeBehavior -> "Before"
        is AnchorType.AtLocation -> "At"
        is AnchorType.AtTime -> "At"
    }
    val anchor = habit.anchorBehavior
    return if (anchor.isNotBlank()) "$prefix $anchor" else null
}

/**
 * Formats a "HH:mm" time string to a display-friendly "h:mm a" format.
 * Returns null if the input is malformed.
 */
private fun formatTimeWindow(time: String): String? = try {
    val parsed = LocalTime.parse(time)
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    parsed.format(formatter)
} catch (_: java.time.format.DateTimeParseException) {
    null
}

/**
 * Returns an estimated duration string like "~5 min", or null when the
 * habit has no meaningful duration (zero, negative, or under one minute).
 */
private fun formatEstimatedDuration(habit: Habit): String? {
    val seconds = habit.estimatedSeconds
    if (seconds <= 0) return null
    val minutes = seconds / 60
    return if (minutes > 0) "~$minutes min" else null
}
