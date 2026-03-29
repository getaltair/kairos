package com.getaltair.kairos.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.KairosApp
import com.getaltair.kairos.data.firebase.FirebaseConfig
import com.getaltair.kairos.data.firebase.FirebaseConfigStore
import com.getaltair.kairos.data.firebase.FirebaseInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

data class SetupUiState(val jsonText: String = "", val status: SetupStatus = SetupStatus.Idle,)

sealed interface SetupStatus {
    data object Idle : SetupStatus
    data object Loading : SetupStatus
    data class Error(val message: String) : SetupStatus
    data object Success : SetupStatus
}

class FirebaseSetupViewModel(private val configStore: FirebaseConfigStore,) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onJsonTextChanged(text: String) {
        _uiState.update { it.copy(jsonText = text, status = SetupStatus.Idle) }
    }

    fun onConfigureClicked(context: Context) {
        if (_uiState.value.status is SetupStatus.Loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(status = SetupStatus.Loading) }

            // Phase 1: Parse JSON
            val config = try {
                parseGoogleServicesJson(_uiState.value.jsonText.trim())
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse Firebase config JSON")
                _uiState.update { it.copy(status = SetupStatus.Error("Invalid google-services.json: ${e.message}")) }
                return@launch
            }

            // Phase 2: Save to encrypted storage
            val saved = try {
                configStore.save(config)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save Firebase config")
                _uiState.update {
                    it.copy(status = SetupStatus.Error("Could not save configuration securely. Try clearing app data."))
                }
                return@launch
            }
            if (!saved) {
                _uiState.update {
                    it.copy(status = SetupStatus.Error("Could not save configuration securely. Try clearing app data."))
                }
                return@launch
            }

            // Phase 3: Initialize Firebase
            try {
                FirebaseInitializer.initialize(context, config)
                (context.applicationContext as KairosApp).onFirebaseConfigured()
                _uiState.update { it.copy(status = SetupStatus.Success) }
                Timber.d("Firebase configured successfully from user input")
            } catch (e: Exception) {
                Timber.e(e, "Firebase initialization failed")
                _uiState.update {
                    it.copy(
                        status = SetupStatus.Error(
                            "Firebase rejected this configuration. Verify your project settings."
                        )
                    )
                }
            }
        }
    }

    companion object {
        internal fun parseGoogleServicesJson(jsonText: String): FirebaseConfig {
            val json = JSONObject(jsonText)
            val projectInfo = json.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val storageBucket = projectInfo.optString("storage_bucket", "").ifEmpty { null }
            val gcmSenderId = projectInfo.optString("project_number", "").ifEmpty { null }
            val clients = json.getJSONArray("client")
            val firstClient = clients.getJSONObject(0)
            val applicationId = firstClient.getJSONObject("client_info").getString("mobilesdk_app_id")
            val apiKey = firstClient.getJSONArray("api_key").getJSONObject(0).getString("current_key")
            return FirebaseConfig(
                projectId = projectId,
                applicationId = applicationId,
                apiKey = apiKey,
                storageBucket = storageBucket,
                gcmSenderId = gcmSenderId,
            )
        }
    }
}
