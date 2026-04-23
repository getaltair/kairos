package com.getaltair.kairos.feature.habit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.ui.icons.filled.Archive
import com.getaltair.kairos.ui.icons.filled.Pause
import com.getaltair.kairos.ui.icons.filled.Restore

@Composable
fun HabitActionButtons(
    status: HabitStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (status) {
            is HabitStatus.Active -> {
                FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null)
                    Text("Archive")
                }
            }

            is HabitStatus.Paused -> {
                FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("Resume")
                }
                OutlinedButton(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null)
                    Text("Archive")
                }
            }

            is HabitStatus.Archived -> {
                FilledTonalButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Text("Restore")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("Delete")
                }
            }
        }
    }
}
