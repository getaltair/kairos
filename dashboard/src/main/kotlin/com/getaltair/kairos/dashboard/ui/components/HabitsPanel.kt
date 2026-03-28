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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import java.util.UUID

/**
 * Main panel showing today's habits grouped by [HabitCategory].
 *
 * Each category section has a header with the category emoji and name,
 * followed by habit rows with completion circles. A summary line at the
 * bottom shows progress.
 */
@Composable
fun HabitsPanel(
    habitsByCategory: Map<HabitCategory, List<Habit>>,
    completedHabitIds: Set<UUID>,
    completedCount: Int,
    totalHabits: Int,
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
                text = "TODAY'S HABITS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Ordered display: Morning, Afternoon, Evening, Anytime
                val categoryOrder = listOf(
                    HabitCategory.Morning,
                    HabitCategory.Afternoon,
                    HabitCategory.Evening,
                    HabitCategory.Anytime,
                )

                categoryOrder.forEach { category ->
                    val habits = habitsByCategory[category]
                    if (!habits.isNullOrEmpty()) {
                        CategorySection(
                            category = category,
                            habits = habits,
                            completedHabitIds = completedHabitIds,
                            onComplete = onComplete,
                        )
                    }
                }

                // Render any future categories not yet in the explicit ordering
                habitsByCategory.forEach { (category, habits) ->
                    if (category !in categoryOrder && habits.isNotEmpty()) {
                        CategorySection(
                            category = category,
                            habits = habits,
                            completedHabitIds = completedHabitIds,
                            onComplete = onComplete,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val percent = if (totalHabits > 0) (completedCount * 100) / totalHabits else 0
            Text(
                text = "Completed: $completedCount of $totalHabits ($percent%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CategorySection(
    category: HabitCategory,
    habits: List<Habit>,
    completedHabitIds: Set<UUID>,
    onComplete: (UUID) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${category.emoji}  ${category.displayName}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        habits.forEach { habit ->
            val isCompleted = habit.id in completedHabitIds
            HabitRow(
                habitId = habit.id,
                name = habit.name,
                isCompleted = isCompleted,
                onComplete = onComplete,
            )
        }
    }
}

@Composable
private fun HabitRow(habitId: UUID, name: String, isCompleted: Boolean, onComplete: (UUID) -> Unit,) {
    val iconColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "habitRowIconColor",
    )

    val textColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        label = "habitRowTextColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted) { onComplete(habitId) }
            .padding(start = 8.dp),
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isCompleted) "Completed" else "Pending",
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}
