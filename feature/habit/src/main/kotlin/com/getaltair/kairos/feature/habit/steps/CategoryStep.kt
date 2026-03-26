package com.getaltair.kairos.feature.habit.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.enums.HabitCategory

private val allCategories: List<HabitCategory> = listOf(
    HabitCategory.Morning,
    HabitCategory.Afternoon,
    HabitCategory.Evening,
    HabitCategory.Anytime,
    HabitCategory.Departure
)

private fun categoryDescription(category: HabitCategory): String = when (category) {
    is HabitCategory.Morning -> "Before noon"
    is HabitCategory.Afternoon -> "12 PM - 6 PM"
    is HabitCategory.Evening -> "After 6 PM"
    is HabitCategory.Anytime -> "No specific time"
    is HabitCategory.Departure -> "Shown on doorway dashboard"
}

@Composable
fun CategoryStep(
    selected: HabitCategory?,
    categoryError: String?,
    onCategorySelected: (HabitCategory) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Choose a category",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            allCategories.chunked(2).forEach { rowCategories ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowCategories.forEach { category ->
                        val isSelected = selected == category
                        if (isSelected) {
                            ElevatedCard(
                                onClick = { onCategorySelected(category) },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                CategoryCardContent(category = category)
                            }
                        } else {
                            OutlinedCard(
                                onClick = { onCategorySelected(category) },
                                modifier = Modifier.weight(1f)
                            ) {
                                CategoryCardContent(category = category)
                            }
                        }
                    }
                    // Fill remaining space if odd number of items in row
                    if (rowCategories.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (categoryError != null) {
            Text(
                text = categoryError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun CategoryCardContent(category: HabitCategory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = category.emoji,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = categoryDescription(category),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
