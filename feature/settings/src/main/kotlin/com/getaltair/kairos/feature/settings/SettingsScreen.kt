package com.getaltair.kairos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.getaltair.kairos.domain.sync.SyncState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = { Text(text = "Settings") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Account section
            AccountSection(
                uiState = uiState,
                onNavigateToLogin = onNavigateToLogin,
                onSignOutRequest = viewModel::onSignOutRequest,
                onDeleteAccountRequest = viewModel::onDeleteAccountRequest,
            )

            // Sync section
            SyncSection(uiState = uiState, onNavigateToLogin = onNavigateToLogin)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Sign out confirmation dialog
    if (uiState.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onSignOutDismiss,
            title = { Text(text = "Sign out?") },
            text = {
                Text(
                    text = "You will need to sign in again to sync your habits across devices.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::signOut) {
                    Text(text = "Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onSignOutDismiss) {
                    Text(text = "Cancel")
                }
            },
        )
    }

    // Delete account confirmation dialog
    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteAccountDismiss,
            title = { Text(text = "Delete account?") },
            text = {
                Text(
                    text = "This action cannot be undone. All your data will be permanently deleted.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteAccountConfirm) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteAccountDismiss) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun AccountSection(
    uiState: SettingsUiState,
    onNavigateToLogin: () -> Unit,
    onSignOutRequest: () -> Unit,
    onDeleteAccountRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (uiState.isSignedIn) {
                    // Signed in state
                    Text(
                        text = uiState.userEmail ?: "Signed in",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Button(
                        onClick = onSignOutRequest,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = "Sign Out")
                    }

                    TextButton(
                        onClick = onDeleteAccountRequest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Delete Account",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    // Signed out state
                    Text(
                        text = "Sign in to sync your habits across devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = "Sign In")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncSection(uiState: SettingsUiState, onNavigateToLogin: () -> Unit,) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Sync",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Sync status row
                SyncStatusRow(syncState = uiState.syncState)

                // Last sync time
                uiState.lastSyncTime?.let { lastSync ->
                    val formatted = DateTimeFormatter
                        .ofPattern("MMM d, h:mm a")
                        .withZone(ZoneId.systemDefault())
                        .format(lastSync)
                    Text(
                        text = "Last synced: $formatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Sign in prompt when not signed in
                if (!uiState.isSignedIn) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text(text = "Sign in to enable sync")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusRow(syncState: SyncState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (syncState) {
            is SyncState.Synced -> {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Synced",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Up to date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            is SyncState.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            is SyncState.Offline -> {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is SyncState.Error -> {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Sync error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = "Sync error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = syncState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is SyncState.NotSignedIn -> {
                Icon(
                    imageVector = Icons.Filled.PersonOff,
                    contentDescription = "Not signed in",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Sign in to sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
