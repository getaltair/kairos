package com.getaltair.kairos.dashboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.dashboard.auth.AuthSessionManager
import com.getaltair.kairos.dashboard.auth.buildQrDataString
import com.getaltair.kairos.dashboard.auth.generateQrBitmap
import com.getaltair.kairos.dashboard.auth.getLocalIpAddress
import com.getaltair.kairos.dashboard.ui.theme.OnSurfaceVariant
import com.getaltair.kairos.dashboard.ui.theme.Primary
import com.getaltair.kairos.dashboard.ui.theme.SurfaceVariant
import kotlinx.coroutines.delay

private const val REFRESH_INTERVAL_MS = 120_000L
private const val COUNTDOWN_TICK_MS = 1_000L
private const val SESSION_TTL_SECONDS = 120L

/**
 * Full-screen login composable that displays a QR code for the mobile
 * app to scan. Designed for a kiosk viewed at 3-4 feet on a 1080p display.
 *
 * The QR code auto-refreshes every 2 minutes (matching the pending session
 * TTL) and a countdown timer is shown beneath the instructions.
 */
@Composable
fun LoginScreen(authSessionManager: AuthSessionManager, serverPort: Int, modifier: Modifier = Modifier,) {
    val localIp = remember { getLocalIpAddress() ?: "unknown" }

    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var secondsRemaining by remember { mutableLongStateOf(SESSION_TTL_SECONDS) }

    // Generate (or regenerate) the QR code and reset the countdown
    fun refreshQr() {
        val session = authSessionManager.createPendingSession(localIp, serverPort)
        val data = buildQrDataString(localIp, serverPort, session.sessionToken)
        qrBitmap = generateQrBitmap(data)
        secondsRemaining = SESSION_TTL_SECONDS
    }

    // Initial generation + periodic refresh
    LaunchedEffect(Unit) {
        refreshQr()
        while (true) {
            delay(REFRESH_INTERVAL_MS)
            refreshQr()
        }
    }

    // Countdown ticker (runs every second)
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0) {
            delay(COUNTDOWN_TICK_MS)
            secondsRemaining--
        } else {
            // Auto-refresh when timer reaches zero
            refreshQr()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App name
            Text(
                text = "Kairos",
                style = MaterialTheme.typography.displaySmall,
                color = Primary,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // QR code
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR code for dashboard linking",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(400.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Instructions
            Text(
                text = "Scan with Kairos app to sign in",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            Text(
                text = "Refreshes in %d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Network info (subtle)
            Text(
                text = "$localIp:$serverPort",
                style = MaterialTheme.typography.bodySmall,
                color = SurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
