package com.getaltair.kairos.feature.auth.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Scan status for the dashboard linking flow.
 */
enum class ScanStatus {
    Idle,
    Scanning,
    Confirming,
    Success,
    Error,
}

/**
 * UI state for the dashboard QR scan screen.
 */
data class DashboardScanUiState(val scanStatus: ScanStatus = ScanStatus.Idle, val errorMessage: String? = null,)

/**
 * ViewModel for scanning a dashboard QR code and confirming authentication.
 *
 * Parses a `kairos://link-dashboard?host=...&port=...&session=...` URI from
 * the scanned QR code, obtains the current user's Firebase ID token, and
 * posts it to the dashboard's local HTTP server for verification.
 */
class DashboardScanViewModel(private val auth: FirebaseAuth, private val dashboardAuthClient: DashboardAuthClient,) :
    ViewModel() {

    private val _uiState = MutableStateFlow(DashboardScanUiState())
    val uiState: StateFlow<DashboardScanUiState> = _uiState.asStateFlow()

    /**
     * Called when the QR code scanner returns a raw string value.
     * Validates the URI, obtains a Firebase ID token, and confirms
     * authentication with the dashboard.
     */
    fun onQrCodeScanned(rawValue: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanStatus = ScanStatus.Confirming, errorMessage = null) }

            try {
                val uri = Uri.parse(rawValue)

                // Validate URI scheme and host
                if (uri.scheme != EXPECTED_SCHEME || uri.host != EXPECTED_HOST) {
                    _uiState.update {
                        it.copy(
                            scanStatus = ScanStatus.Error,
                            errorMessage = "Not a valid Kairos dashboard QR code",
                        )
                    }
                    return@launch
                }

                val host = uri.getQueryParameter("host")
                val portStr = uri.getQueryParameter("port")
                val session = uri.getQueryParameter("session")

                if (host.isNullOrBlank() || portStr.isNullOrBlank() || session.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            scanStatus = ScanStatus.Error,
                            errorMessage = "Not a valid Kairos dashboard QR code",
                        )
                    }
                    return@launch
                }

                val port = portStr.toIntOrNull()
                if (port == null) {
                    _uiState.update {
                        it.copy(
                            scanStatus = ScanStatus.Error,
                            errorMessage = "Not a valid Kairos dashboard QR code",
                        )
                    }
                    return@launch
                }

                // Obtain Firebase ID token
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            scanStatus = ScanStatus.Error,
                            errorMessage = "Please sign in first",
                        )
                    }
                    return@launch
                }

                val idToken = currentUser.getIdToken(true).await().token
                if (idToken == null) {
                    _uiState.update {
                        it.copy(
                            scanStatus = ScanStatus.Error,
                            errorMessage = "Unable to verify your identity. Please try again.",
                        )
                    }
                    return@launch
                }

                // Confirm with the dashboard
                val result = dashboardAuthClient.confirmAuth(
                    host = host,
                    port = port,
                    sessionToken = session,
                    firebaseIdToken = idToken,
                )

                result.fold(
                    onSuccess = {
                        Timber.d("Dashboard linked successfully for user %s", it.userId)
                        _uiState.update { state ->
                            state.copy(scanStatus = ScanStatus.Success, errorMessage = null)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Dashboard auth confirmation failed")
                        _uiState.update { state ->
                            state.copy(
                                scanStatus = ScanStatus.Error,
                                errorMessage = "Unable to link dashboard. " +
                                    "Make sure you are on the same network.",
                            )
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during QR scan processing")
                _uiState.update {
                    it.copy(
                        scanStatus = ScanStatus.Error,
                        errorMessage = "Something went wrong. Please try again.",
                    )
                }
            }
        }
    }

    /**
     * Resets the UI state back to [ScanStatus.Idle] for retry.
     */
    fun resetState() {
        _uiState.update { DashboardScanUiState() }
    }

    private companion object {
        const val EXPECTED_SCHEME = "kairos"
        const val EXPECTED_HOST = "link-dashboard"
    }
}
