package com.getaltair.kairos.feature.routine.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large circular countdown timer display.
 *
 * Shows an outer progress arc representing the percentage of time remaining,
 * a large center text showing MM:SS format, and secondary text with the habit name.
 * Animates smoothly between ticks for a polished feel.
 *
 * Styling follows DESIGN.md: primary teal for the arc, soft off-white for text,
 * surface container for the background track. No hard borders.
 *
 * @param timeRemainingSeconds Current seconds remaining
 * @param totalTimeSeconds Total allocated seconds for this step
 * @param habitName Name of the current habit (used for accessibility context, not displayed directly)
 * @param isPaused Whether the timer is paused
 */
@Composable
fun TimerDisplay(
    timeRemainingSeconds: Int,
    totalTimeSeconds: Int,
    habitName: String,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalTimeSeconds > 0) {
        timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat()
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "timer_progress",
    )

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeText = String.format("%d:%02d", minutes, seconds)

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val pausedColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Background track and progress arc
        Canvas(modifier = Modifier.size(240.dp)) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(
                size.width - strokeWidth,
                size.height - strokeWidth,
            )
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc
            val arcColor = if (isPaused) pausedColor else primaryColor
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPaused) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (isPaused) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
