package com.getaltair.kairos.feature.habit.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.time.DayOfWeek

private data class HabitIcon(val name: String, val vector: ImageVector)

private val habitIcons: List<HabitIcon> = listOf(
    HabitIcon("fitness", Icons.Filled.FitnessCenter),
    HabitIcon("run", Icons.Filled.DirectionsRun),
    HabitIcon("book", Icons.Filled.Book),
    HabitIcon("meditation", Icons.Filled.SelfImprovement),
    HabitIcon("journal", Icons.Filled.Edit),
    HabitIcon("water", Icons.Filled.LocalDrink),
    HabitIcon("food", Icons.Filled.Restaurant),
    HabitIcon("sleep", Icons.Filled.Bedtime),
    HabitIcon("music", Icons.Filled.MusicNote)
)

private val habitColors: List<Pair<String, Color>> = listOf(
    "#4CAF50" to Color(0xFF4CAF50),
    "#2196F3" to Color(0xFF2196F3),
    "#9C27B0" to Color(0xFF9C27B0),
    "#FF9800" to Color(0xFFFF9800),
    "#F44336" to Color(0xFFF44336),
    "#00BCD4" to Color(0xFF00BCD4),
    "#FF5722" to Color(0xFFFF5722),
    "#607D8B" to Color(0xFF607D8B)
)

private val frequencyOptions: List<HabitFrequency> = listOf(
    HabitFrequency.Daily,
    HabitFrequency.Weekdays,
    HabitFrequency.Weekends,
    HabitFrequency.Custom
)

private val daysOfWeek: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes == 1) "1 minute" else "$minutes minutes"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OptionsStep(
    estimatedSeconds: Int,
    microVersion: String,
    icon: String?,
    color: String?,
    frequency: HabitFrequency,
    activeDays: Set<DayOfWeek>,
    isCreating: Boolean,
    onEstimatedSecondsChanged: (Int) -> Unit,
    onMicroVersionChanged: (String) -> Unit,
    onIconSelected: (String?) -> Unit,
    onColorSelected: (String?) -> Unit,
    onFrequencySelected: (HabitFrequency) -> Unit,
    onActiveDaysChanged: (Set<DayOfWeek>) -> Unit,
    onCreateHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Optional details",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "All fields have sensible defaults",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Duration section
        Text(
            text = "Estimated duration",
            style = MaterialTheme.typography.labelLarge
        )

        Text(
            text = formatDuration(estimatedSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Slider(
            value = estimatedSeconds.toFloat(),
            onValueChange = { onEstimatedSecondsChanged(it.toInt()) },
            valueRange = 60f..3600f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Micro version section
        Text(
            text = "Micro version",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = microVersion,
            onValueChange = onMicroVersionChanged,
            label = { Text("Micro version") },
            placeholder = { Text("The smallest possible version of this habit") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("The smallest possible version of this habit") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Frequency section
        Text(
            text = "Frequency",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            frequencyOptions.forEachIndexed { index, freq ->
                SegmentedButton(
                    selected = frequency == freq,
                    onClick = { onFrequencySelected(freq) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = frequencyOptions.size
                    ),
                    label = { Text(freq.displayName) }
                )
            }
        }

        if (frequency is HabitFrequency.Custom) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    FilterChip(
                        selected = day in activeDays,
                        onClick = {
                            val newDays = if (day in activeDays) {
                                activeDays - day
                            } else {
                                activeDays + day
                            }
                            onActiveDaysChanged(newDays)
                        },
                        label = { Text(dayLabel(day)) },
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Icon section
        Text(
            text = "Icon",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(habitIcons) { habitIcon ->
                val isSelected = icon == habitIcon.name
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            onIconSelected(if (isSelected) null else habitIcon.name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = habitIcon.vector,
                        contentDescription = habitIcon.name,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color section
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            habitColors.forEach { (hex, colorValue) ->
                val isSelected = color == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorValue)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(if (isSelected) null else hex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateHabit,
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Habit")
        }

        TextButton(
            onClick = onCreateHabit,
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip, use defaults")
        }
    }
}
