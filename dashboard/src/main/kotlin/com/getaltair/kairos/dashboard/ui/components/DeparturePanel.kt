package com.getaltair.kairos.dashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Habit
import java.util.UUID

/**
 * "Don't Forget" panel showing departure-category habits as a checklist.
 *
 * Completed habits display with a filled checkbox and strikethrough text;
 * pending habits show an empty checkbox at full brightness.
 */
@Composable
fun DeparturePanel(
    departureHabits: List<Habit>,
    completedHabitIds: Set<UUID>,
    onComplete: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "DON'T FORGET",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                departureHabits.forEach { habit ->
                    val isCompleted = habit.id in completedHabitIds
                    DepartureItem(
                        habitId = habit.id,
                        name = habit.name,
                        isCompleted = isCompleted,
                        onComplete = onComplete,
                    )
                }

                if (departureHabits.isEmpty()) {
                    Text(
                        text = "No departure items today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DepartureItem(habitId: UUID, name: String, isCompleted: Boolean, onComplete: (UUID) -> Unit,) {
    val textColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        label = "departureItemTextColor",
    )

    val iconColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "departureItemIconColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted) { onComplete(habitId) },
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = if (isCompleted) "Completed" else "Pending",
            tint = iconColor,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}
