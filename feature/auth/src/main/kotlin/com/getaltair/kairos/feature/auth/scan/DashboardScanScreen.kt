package com.getaltair.kairos.feature.auth.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScanScreen(onBack: () -> Unit, viewModel: DashboardScanViewModel = koinViewModel(),) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launch scanner on initial entry
    LaunchedEffect(Unit) {
        launchScanner(context, viewModel)
    }

    // Auto-navigate back after success
    if (uiState.scanStatus == ScanStatus.Success) {
        LaunchedEffect(Unit) {
            delay(AUTO_NAVIGATE_DELAY_MS)
            onBack()
        }
    }

    Scaffold(
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
                title = { Text(text = "Link Dashboard") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (uiState.scanStatus) {
                ScanStatus.Idle,
                ScanStatus.Scanning -> ScanningContent()

                ScanStatus.Confirming -> ConfirmingContent()

                ScanStatus.Success -> SuccessContent()

                ScanStatus.Error -> ErrorContent(
                    errorMessage = uiState.errorMessage,
                    onTryAgain = {
                        viewModel.resetState()
                        launchScanner(context, viewModel)
                    },
                    onGoBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun ScanningContent() {
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 3.dp,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Opening scanner...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConfirmingContent() {
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 3.dp,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Linking dashboard...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SuccessContent() {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Success",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Dashboard linked successfully!",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorContent(errorMessage: String?, onTryAgain: () -> Unit, onGoBack: () -> Unit,) {
    Icon(
        imageVector = Icons.Filled.Error,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(64.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = errorMessage ?: "An unexpected error occurred.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onTryAgain,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text = "Try Again")
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onGoBack) {
        Text(text = "Go Back")
    }
}

/**
 * Launches the Google Code Scanner and forwards the result to the ViewModel.
 */
private fun launchScanner(context: android.content.Context, viewModel: DashboardScanViewModel,) {
    val scanner = GmsBarcodeScanning.getClient(context)
    scanner.startScan()
        .addOnSuccessListener { barcode ->
            val rawValue = barcode.rawValue
            if (rawValue != null) {
                viewModel.onQrCodeScanned(rawValue)
            } else {
                Timber.w("Barcode scanned but rawValue was null")
                viewModel.resetState()
            }
        }
        .addOnFailureListener { exception ->
            Timber.e(exception, "QR code scan failed")
            viewModel.resetState()
        }
        .addOnCanceledListener {
            Timber.d("QR code scan cancelled by user")
            viewModel.resetState()
        }
}

private const val AUTO_NAVIGATE_DELAY_MS = 2_000L
