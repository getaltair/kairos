package com.getaltair.kairos.data.firebase

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Holds the Firebase project configuration needed to initialize
 * [com.google.firebase.FirebaseApp] at runtime (when no bundled
 * `google-services.json` is present).
 */
data class FirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val storageBucket: String? = null,
    val gcmSenderId: String? = null,
)

/**
 * Persists [FirebaseConfig] in [EncryptedSharedPreferences] so that
 * the user only has to enter their Firebase credentials once. The
 * values are encrypted at rest via the Android Keystore.
 */
class FirebaseConfigStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(config: FirebaseConfig) {
        prefs.edit()
            .putString(KEY_PROJECT_ID, config.projectId)
            .putString(KEY_APPLICATION_ID, config.applicationId)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_STORAGE_BUCKET, config.storageBucket)
            .putString(KEY_GCM_SENDER_ID, config.gcmSenderId)
            .apply()
    }

    fun load(): FirebaseConfig? {
        val projectId = prefs.getString(KEY_PROJECT_ID, null) ?: return null
        val applicationId = prefs.getString(KEY_APPLICATION_ID, null) ?: return null
        val apiKey = prefs.getString(KEY_API_KEY, null) ?: return null

        return FirebaseConfig(
            projectId = projectId,
            applicationId = applicationId,
            apiKey = apiKey,
            storageBucket = prefs.getString(KEY_STORAGE_BUCKET, null),
            gcmSenderId = prefs.getString(KEY_GCM_SENDER_ID, null),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isConfigured(): Boolean = prefs.getString(KEY_PROJECT_ID, null) != null &&
        prefs.getString(KEY_APPLICATION_ID, null) != null &&
        prefs.getString(KEY_API_KEY, null) != null

    private companion object {
        const val PREFS_FILE = "firebase_config"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_APPLICATION_ID = "application_id"
        const val KEY_API_KEY = "api_key"
        const val KEY_STORAGE_BUCKET = "storage_bucket"
        const val KEY_GCM_SENDER_ID = "gcm_sender_id"
    }
}
