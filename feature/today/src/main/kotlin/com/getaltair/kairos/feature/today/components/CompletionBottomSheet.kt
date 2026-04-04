package com.getaltair.kairos.feature.today.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.ui.icons.filled.Adjust
import com.getaltair.kairos.ui.icons.filled.SkipNext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionBottomSheet(
    habit: Habit,
    onDone: () -> Unit,
    onPartial: (Int) -> Unit,
    onSkip: (SkipReason?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showPartialSlider by remember { mutableStateOf(false) }
    var partialValue by remember { mutableFloatStateOf(50f) }
    var showSkipReasons by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Done row
            ListItem(
                headlineContent = { Text("Done") },
                leadingContent = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onDone()
                    onDismiss()
                }
            )

            // Partial row
            ListItem(
                headlineContent = { Text("Partial") },
                leadingContent = {
                    Icon(Icons.Filled.Adjust, contentDescription = null)
                },
                modifier = Modifier.clickable { showPartialSlider = !showPartialSlider }
            )
            if (showPartialSlider) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Slider(
                        value = partialValue,
                        onValueChange = { partialValue = it },
                        valueRange = 1f..99f
                    )
                    Text(
                        "${partialValue.toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            onPartial(partialValue.toInt())
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Confirm") }
                }
            }

            // Skip row
            ListItem(
                headlineContent = { Text("Skip") },
                leadingContent = {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                },
                modifier = Modifier.clickable { showSkipReasons = !showSkipReasons }
            )
            if (showSkipReasons) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    // No reason option
                    ListItem(
                        headlineContent = { Text("No reason") },
                        modifier = Modifier.clickable {
                            onSkip(null)
                            onDismiss()
                        }
                    )
                    // Skip reason options from SkipReason sealed class
                    ListItem(
                        headlineContent = { Text(SkipReason.TooTired.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.TooTired)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(SkipReason.NoTime.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.NoTime)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(SkipReason.NotFeelingWell.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.NotFeelingWell)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(SkipReason.Traveling.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.Traveling)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(SkipReason.TookDayOff.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.TookDayOff)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(SkipReason.Other.displayName) },
                        modifier = Modifier.clickable {
                            onSkip(SkipReason.Other)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
