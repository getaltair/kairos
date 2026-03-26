package com.getaltair.kairos.feature.today.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.model.HabitWithStatus

@Composable
fun HabitCard(habitWithStatus: HabitWithStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val completion = habitWithStatus.todayCompletion
    val isDone = completion != null && completion.type is CompletionType.Full
    val isSkipped = completion != null && completion.type is CompletionType.Skipped
    val isPartial = completion != null && completion.type is CompletionType.Partial
    val cardAlpha = if (isDone || isSkipped) 0.6f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .alpha(cardAlpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            when {
                isDone -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )

                isPartial -> Icon(
                    Icons.Filled.Adjust,
                    contentDescription = "Partial",
                    tint = MaterialTheme.colorScheme.secondary
                )

                isSkipped -> Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Skipped",
                    tint = MaterialTheme.colorScheme.outline
                )

                else -> Icon(
                    Icons.Outlined.Circle,
                    contentDescription = "Pending",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitWithStatus.habit.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = habitWithStatus.habit.anchorBehavior,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
