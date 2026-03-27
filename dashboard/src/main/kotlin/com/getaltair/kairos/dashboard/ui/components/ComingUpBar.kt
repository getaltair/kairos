package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType

/**
 * Bottom bar showing the next pending habits or an "all done" message.
 *
 * When habits remain, displays "COMING UP: name (anchor context)" for each,
 * separated by a bullet. When everything is complete, shows a celebratory
 * message in the primary color.
 */
@Composable
fun ComingUpBar(comingUpHabits: List<Habit>, modifier: Modifier = Modifier,) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (comingUpHabits.isEmpty()) {
            Text(
                text = "\u2713 All habits complete for today!",
                style = MaterialTheme.typography.bodyLarge,
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
                        }
                        if (index < comingUpHabits.lastIndex) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append("  \u2022  ")
                            }
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Builds a short anchor context string like "After brushing teeth" or "At 8:00 AM".
 */
private fun formatAnchorContext(habit: Habit): String? {
    val prefix = when (habit.anchorType) {
        is AnchorType.AfterBehavior -> "After"
        is AnchorType.BeforeBehavior -> "Before"
        is AnchorType.AtLocation -> "At"
        is AnchorType.AtTime -> "At"
    }
    val anchor = habit.anchorBehavior
    return if (anchor.isNotBlank()) "$prefix $anchor" else null
}
