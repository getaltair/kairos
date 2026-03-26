package com.getaltair.kairos.feature.habit.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.enums.AnchorType

private val anchorTypes = listOf(
    AnchorType.AfterBehavior,
    AnchorType.BeforeBehavior,
    AnchorType.AtLocation,
    AnchorType.AtTime
)

private val anchorTabLabels = listOf(
    "After I...",
    "Before I...",
    "When I arrive at...",
    "At a specific time"
)

private val afterPresets = listOf(
    "Wake up",
    "Brush my teeth",
    "Have breakfast",
    "Get to work"
)

private val beforePresets = listOf(
    "Go to bed",
    "Leave for work",
    "Eat dinner"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnchorStep(
    anchorType: AnchorType,
    anchorBehavior: String,
    anchorTime: String?,
    anchorError: String?,
    onAnchorTypeSelected: (AnchorType) -> Unit,
    onAnchorBehaviorChanged: (String) -> Unit,
    onAnchorTimeChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTabIndex = when (anchorType) {
        is AnchorType.AfterBehavior -> 0
        is AnchorType.BeforeBehavior -> 1
        is AnchorType.AtLocation -> 2
        is AnchorType.AtTime -> 3
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "When will you do this?",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            anchorTabLabels.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onAnchorTypeSelected(anchorTypes[index]) },
                    text = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (anchorType) {
            is AnchorType.AfterBehavior -> {
                BehaviorPresetContent(
                    presets = afterPresets,
                    anchorBehavior = anchorBehavior,
                    placeholder = "Or type your own...",
                    onPresetSelected = onAnchorBehaviorChanged,
                    onTextChanged = onAnchorBehaviorChanged
                )
            }

            is AnchorType.BeforeBehavior -> {
                BehaviorPresetContent(
                    presets = beforePresets,
                    anchorBehavior = anchorBehavior,
                    placeholder = "Or type your own...",
                    onPresetSelected = onAnchorBehaviorChanged,
                    onTextChanged = onAnchorBehaviorChanged
                )
            }

            is AnchorType.AtLocation -> {
                OutlinedTextField(
                    value = anchorBehavior,
                    onValueChange = onAnchorBehaviorChanged,
                    label = { Text("Location") },
                    placeholder = { Text("e.g. Home, Work, Gym...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            is AnchorType.AtTime -> {
                var showTimePicker by remember { mutableStateOf(false) }
                val parsedHour = anchorTime?.substringBefore(":")?.toIntOrNull() ?: 7
                val parsedMinute = anchorTime?.substringAfter(":")?.toIntOrNull() ?: 0
                val timePickerState = rememberTimePickerState(
                    initialHour = parsedHour,
                    initialMinute = parsedMinute,
                    is24Hour = false,
                )

                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(
                        text = if (anchorTime.isNullOrBlank()) {
                            "Select a time"
                        } else {
                            formatTime(parsedHour, parsedMinute)
                        },
                    )
                }

                if (showTimePicker) {
                    TimePickerDialog(
                        onConfirm = {
                            val formatted = String.format(
                                "%02d:%02d",
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                            onAnchorTimeChanged(formatted)
                            showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false },
                    ) {
                        TimePicker(state = timePickerState)
                    }
                }
            }
        }

        if (anchorError != null) {
            Text(
                text = anchorError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isContinueEnabled = when (anchorType) {
            is AnchorType.AtTime -> !anchorTime.isNullOrBlank()
            else -> anchorBehavior.isNotBlank()
        }

        Button(
            onClick = onContinue,
            enabled = isContinueEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, content: @Composable () -> Unit,) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { content() },
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BehaviorPresetContent(
    presets: List<String>,
    anchorBehavior: String,
    placeholder: String,
    onPresetSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            presets.forEach { preset ->
                FilterChip(
                    selected = anchorBehavior == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = anchorBehavior,
            onValueChange = onTextChanged,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
