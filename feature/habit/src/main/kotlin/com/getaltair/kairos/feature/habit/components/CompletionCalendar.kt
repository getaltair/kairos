package com.getaltair.kairos.feature.habit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CompletionCalendar(
    completions: List<Completion>,
    onDateTapped: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    // 30 days in chronological order (oldest first)
    val days = remember(today) {
        (0L..29L).map { today.minusDays(29 - it) }
    }
    val completionMap = remember(completions) {
        completions.associateBy { it.date }
    }
    // 7-day backdate window (today minus 6 = 7 days inclusive)
    val backdateLimit = remember(today) { today.minusDays(6) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("d") }
    val rows = remember(days) { days.chunked(7) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Last 30 days",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { date ->
                        val completion = completionMap[date]
                        val isTappable = date >= backdateLimit &&
                            date <= today &&
                            completion == null
                        val cellColor = completionCellColor(completion?.type)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor)
                                .then(
                                    if (isTappable) {
                                        Modifier.clickable { onDateTapped(date) }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.format(dayFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = if (completion != null) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    // Pad the last row to maintain 7-column grid alignment
                    repeat(7 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun completionCellColor(type: CompletionType?): Color = when (type) {
    is CompletionType.Full -> MaterialTheme.colorScheme.primary
    is CompletionType.Partial -> MaterialTheme.colorScheme.secondary
    is CompletionType.Skipped -> MaterialTheme.colorScheme.outline
    is CompletionType.Missed -> MaterialTheme.colorScheme.errorContainer
    null -> MaterialTheme.colorScheme.surfaceVariant
}
