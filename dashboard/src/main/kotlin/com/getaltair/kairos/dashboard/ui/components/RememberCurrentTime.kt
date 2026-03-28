package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Remembers the current [LocalDateTime], updating every second.
 *
 * Uses a single [LaunchedEffect] with a 1-second delay loop.
 * Cancellation is handled by Compose's structured concurrency.
 */
@Composable
fun rememberCurrentTime(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = LocalDateTime.now()
        }
    }
    return now
}
