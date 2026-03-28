package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Minimal clock display for standby mode.
 *
 * Shows a large time and date on a near-black background. The clock
 * refreshes every second. An optional [offset] shifts the content to
 * prevent burn-in on always-on displays.
 */
@Composable
fun StandbyScreen(offset: DpOffset = DpOffset.Zero) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = LocalDateTime.now()
        }
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .offset(x = offset.x, y = offset.y),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = now.format(timeFormatter),
                fontSize = 96.sp,
                color = Color(0xFFE8E8E8),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
            )
            Text(
                text = now.format(dateFormatter),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF888888),
            )
        }
    }
}
