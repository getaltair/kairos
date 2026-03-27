package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Displays how recently data was fetched and shows a stale-data warning when
 * the last update is older than 2 minutes.
 */
@Composable
fun StatusIndicator(lastUpdated: Instant?, isStale: Boolean, modifier: Modifier = Modifier,) {
    // Re-render the relative timestamp every 10 seconds so it stays fresh
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            tick++
        }
    }

    // Suppress the unused-variable warning; tick is read to trigger recomposition
    @Suppress("UNUSED_EXPRESSION")
    tick

    if (isStale) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Offline -- showing cached data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else if (lastUpdated != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Last updated: ${formatElapsed(lastUpdated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Returns a human-friendly relative timestamp like "just now", "25 seconds ago",
 * or "3 minutes ago".
 */
private fun formatElapsed(since: Instant): String {
    val elapsed = Duration.between(since, Instant.now())
    val seconds = elapsed.seconds
    return when {
        seconds < 10 -> "just now"

        seconds < 60 -> "$seconds seconds ago"

        else -> {
            val minutes = elapsed.toMinutes()
            if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
    }
}
