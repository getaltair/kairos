package com.getaltair.kairos.feature.habit.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                OutlinedTextField(
                    value = anchorTime ?: "",
                    onValueChange = { onAnchorTimeChanged(it) },
                    label = { Text("Time") },
                    placeholder = { Text("e.g. 7:00 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
