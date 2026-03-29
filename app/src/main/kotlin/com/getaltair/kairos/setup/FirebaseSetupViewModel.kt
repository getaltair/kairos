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

data class SetupUiState(
    val jsonText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

class FirebaseSetupViewModel(private val configStore: FirebaseConfigStore,) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onJsonTextChanged(text: String) {
        _uiState.update { it.copy(jsonText = text, error = null) }
    }

    fun onConfigureClicked(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val json = JSONObject(_uiState.value.jsonText.trim())

                val projectInfo = json.getJSONObject("project_info")
                val projectId = projectInfo.getString("project_id")
                val storageBucket = projectInfo.optString("storage_bucket", null)
                val gcmSenderId = projectInfo.optString("project_number", null)

                val clients = json.getJSONArray("client")
                val firstClient = clients.getJSONObject(0)
                val applicationId = firstClient
                    .getJSONObject("client_info")
                    .getString("mobilesdk_app_id")
                val apiKey = firstClient
                    .getJSONArray("api_key")
                    .getJSONObject(0)
                    .getString("current_key")

                val config = FirebaseConfig(
                    projectId = projectId,
                    applicationId = applicationId,
                    apiKey = apiKey,
                    storageBucket = storageBucket,
                    gcmSenderId = gcmSenderId,
                )

                configStore.save(config)
                FirebaseInitializer.initialize(context, config)
                (context.applicationContext as KairosApp).onFirebaseConfigured()

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                Timber.d("Firebase configured successfully from user input")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse Firebase config JSON")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Invalid google-services.json: ${e.message}",
                    )
                }
            }
        }
    }
}
