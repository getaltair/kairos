package com.getaltair.kairos.dashboard.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Returns a [DpOffset] that shifts by a few density-independent pixels every
 * 10 minutes, cycling through a small set of positions. This prevents OLED or
 * LCD burn-in on the kiosk display by ensuring static content never sits in
 * exactly the same spot for long.
 */
@Composable
fun rememberScreenSaverOffset(): DpOffset {
    val offsets = remember {
        listOf(
            DpOffset(0.dp, 0.dp),
            DpOffset(2.dp, 2.dp),
            DpOffset((-2).dp, 0.dp),
            DpOffset(0.dp, (-2).dp),
            DpOffset(1.dp, (-1).dp),
            DpOffset((-1).dp, 2.dp),
        )
    }

    var index by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10 * 60 * 1000L) // 10 minutes
            index = (index + 1) % offsets.size
        }
    }

    return offsets[index]
}
