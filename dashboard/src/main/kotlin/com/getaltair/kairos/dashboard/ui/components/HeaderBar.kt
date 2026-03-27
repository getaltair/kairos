package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.dashboard.state.ConnectionStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Top bar showing the app title, current date, time, and connection status.
 *
 * The clock refreshes once per minute to avoid unnecessary recompositions.
 */
@Composable
fun HeaderBar(connectionStatus: ConnectionStatus, modifier: Modifier = Modifier,) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = LocalDateTime.now()
        }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }

    val statusColor = when (connectionStatus) {
        ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        ConnectionStatus.Connecting -> MaterialTheme.colorScheme.tertiary
        ConnectionStatus.Offline -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "KAIROS",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = now.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = now.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
        }
    }
}
