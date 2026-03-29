package com.getaltair.kairos.data.firebase

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import timber.log.Timber

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
) {
    init {
        require(projectId.isNotBlank()) { "projectId must not be blank" }
        require(applicationId.isNotBlank()) { "applicationId must not be blank" }
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
    }

    override fun toString(): String =
        "FirebaseConfig(projectId=$projectId, applicationId=$applicationId, apiKey=REDACTED, storageBucket=$storageBucket, gcmSenderId=$gcmSenderId)"
}

/**
 * Persists [FirebaseConfig] in [EncryptedSharedPreferences] so that
 * the user only has to enter their Firebase credentials once. The
 * values are encrypted at rest via the Android Keystore.
 */
class FirebaseConfigStore(context: Context) {

    private val prefs: SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            PREFS_FILE,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to create EncryptedSharedPreferences; attempting recovery")
        try {
            context.applicationContext.deleteSharedPreferences(PREFS_FILE)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (retryException: Exception) {
            Timber.e(retryException, "Recovery failed; config store unavailable")
            null
        }
    }

    fun save(config: FirebaseConfig): Boolean {
        val p = prefs ?: run {
            Timber.e("Config store unavailable")
            return false
        }
        return p.edit()
            .putString(KEY_PROJECT_ID, config.projectId)
            .putString(KEY_APPLICATION_ID, config.applicationId)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_STORAGE_BUCKET, config.storageBucket)
            .putString(KEY_GCM_SENDER_ID, config.gcmSenderId)
            .commit()
    }

    fun load(): FirebaseConfig? {
        val p = prefs ?: return null
        val projectId = p.getString(KEY_PROJECT_ID, null) ?: return null
        val applicationId = p.getString(KEY_APPLICATION_ID, null) ?: return null
        val apiKey = p.getString(KEY_API_KEY, null) ?: return null
        return try {
            FirebaseConfig(
                projectId = projectId,
                applicationId = applicationId,
                apiKey = apiKey,
                storageBucket = p.getString(KEY_STORAGE_BUCKET, null),
                gcmSenderId = p.getString(KEY_GCM_SENDER_ID, null),
            )
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Stored Firebase config failed validation")
            null
        }
    }

    /** Removes all stored credentials. Call when the user wants to reconfigure their Firebase project. */
    fun clear() {
        prefs?.edit()?.clear()?.commit()
    }

    fun isConfigured(): Boolean = load() != null

    private companion object {
        const val PREFS_FILE = "firebase_config"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_APPLICATION_ID = "application_id"
        const val KEY_API_KEY = "api_key"
        const val KEY_STORAGE_BUCKET = "storage_bucket"
        const val KEY_GCM_SENDER_ID = "gcm_sender_id"
    }
}
