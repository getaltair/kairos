package com.getaltair.kairos.feature.auth.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import java.net.InetAddress
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * UI state for the dashboard QR scan screen.
 */
sealed class DashboardScanUiState {
    data object Idle : DashboardScanUiState()
    data object Confirming : DashboardScanUiState()
    data object Success : DashboardScanUiState()
    data class Error(val message: String) : DashboardScanUiState()
}

/**
 * ViewModel for scanning a dashboard QR code and confirming authentication.
 *
 * Parses a `kairos://link-dashboard?host=...&port=...&session=...` URI from
 * the scanned QR code, obtains the current user's Firebase ID token, and
 * posts it to the dashboard's local HTTP server for verification.
 */
class DashboardScanViewModel(private val auth: FirebaseAuth, private val dashboardAuthClient: DashboardAuthClient,) :
    ViewModel() {

    private val _uiState = MutableStateFlow<DashboardScanUiState>(DashboardScanUiState.Idle)
    val uiState: StateFlow<DashboardScanUiState> = _uiState.asStateFlow()
    private var activeJob: Job? = null

    /**
     * Called when the QR code scanner returns a raw string value.
     * Validates the URI, obtains a Firebase ID token, and confirms
     * authentication with the dashboard.
     */
    fun onQrCodeScanned(rawValue: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = DashboardScanUiState.Confirming

            try {
                val uri = Uri.parse(rawValue)

                if (uri.scheme != EXPECTED_SCHEME || uri.host != EXPECTED_HOST) {
                    Timber.w("Invalid QR URI: scheme=%s, host=%s", uri.scheme, uri.host)
                    _uiState.value = DashboardScanUiState.Error("Not a valid Kairos dashboard QR code")
                    return@launch
                }

                val host = uri.getQueryParameter("host")
                val portStr = uri.getQueryParameter("port")
                val session = uri.getQueryParameter("session")

                if (host.isNullOrBlank() || portStr.isNullOrBlank() || session.isNullOrBlank()) {
                    Timber.w(
                        "QR code missing params: host=%s, port=%s, session=%s",
                        host != null,
                        portStr != null,
                        session != null,
                    )
                    _uiState.value = DashboardScanUiState.Error("Not a valid Kairos dashboard QR code")
                    return@launch
                }

                val port = portStr.toIntOrNull()
                if (port == null) {
                    Timber.w("QR code has non-numeric port: %s", portStr)
                    _uiState.value = DashboardScanUiState.Error("Not a valid Kairos dashboard QR code")
                    return@launch
                }

                // SSRF protection: ensure the dashboard host is on the local network
                val address = InetAddress.getByName(host)
                if (!address.isSiteLocalAddress && !address.isLinkLocalAddress) {
                    Timber.w("Dashboard host is not local: %s", host)
                    _uiState.value = DashboardScanUiState.Error("Dashboard must be on your local network")
                    return@launch
                }

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Timber.w("No signed-in user when attempting dashboard link")
                    _uiState.value = DashboardScanUiState.Error("Please sign in first")
                    return@launch
                }

                val idToken = currentUser.getIdToken(true).await().token
                if (idToken == null) {
                    Timber.w("Firebase ID token was null for user %s", currentUser.uid)
                    _uiState.value = DashboardScanUiState.Error(
                        "Unable to verify your identity. Please try again.",
                    )
                    return@launch
                }

                val result = dashboardAuthClient.confirmAuth(
                    host = host,
                    port = port,
                    sessionToken = session,
                    firebaseIdToken = idToken,
                )

                result.fold(
                    onSuccess = {
                        Timber.d("Dashboard linked successfully for user %s", it.userId)
                        _uiState.value = DashboardScanUiState.Success
                    },
                    onFailure = { error ->
                        Timber.e(error, "Dashboard auth confirmation failed")
                        _uiState.value = DashboardScanUiState.Error(
                            "Unable to link dashboard. " +
                                "Make sure you are on the same network.",
                        )
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during QR scan processing")
                _uiState.value = DashboardScanUiState.Error("Something went wrong. Please try again.")
            }
        }
    }

    /**
     * Resets the UI state back to [DashboardScanUiState.Idle] for retry.
     */
    fun resetState() {
        _uiState.value = DashboardScanUiState.Idle
    }

    /**
     * Called when the scanner fails to open or the barcode cannot be read.
     */
    fun onScanFailed(message: String) {
        _uiState.value = DashboardScanUiState.Error(message)
    }

    /**
     * Called when the user cancels the scanner.
     */
    fun onScanCancelled() {
        _uiState.value = DashboardScanUiState.Idle
    }

    private companion object {
        const val EXPECTED_SCHEME = "kairos"
        const val EXPECTED_HOST = "link-dashboard"
    }
}
