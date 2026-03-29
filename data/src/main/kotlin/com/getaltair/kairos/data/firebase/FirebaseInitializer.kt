package com.getaltair.kairos.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

/**
 * Initializes [FirebaseApp] either from a runtime-supplied [FirebaseConfig]
 * or from the bundled `google-services.json` (when the google-services
 * Gradle plugin has already handled initialization).
 *
 * All access to Firebase services should go through the accessors on this
 * object so that the rest of the app is decoupled from how Firebase was
 * bootstrapped.
 */
object FirebaseInitializer {

    @Volatile
    private var initialized = false

    /**
     * Initialize Firebase from a user-supplied [FirebaseConfig].
     * Call this when no `google-services.json` is bundled.
     */
    fun initialize(context: Context, config: FirebaseConfig) {
        if (initialized) {
            Timber.d("Firebase already initialized; skipping")
            return
        }

        val options = FirebaseOptions.Builder()
            .setProjectId(config.projectId)
            .setApplicationId(config.applicationId)
            .setApiKey(config.apiKey)
            .apply {
                config.storageBucket?.let { setStorageBucket(it) }
                config.gcmSenderId?.let { setGcmSenderId(it) }
            }
            .build()

        FirebaseApp.initializeApp(context.applicationContext, options)
        initialized = true
        Timber.d("Firebase initialized from runtime config (project=%s)", config.projectId)
    }

    /**
     * Marks Firebase as initialized. Call this when the google-services
     * plugin already handled bootstrapping via `google-services.json`.
     */
    fun initializeFromExisting() {
        if (initialized) return
        initialized = true
        Timber.d("Firebase marked as initialized from google-services.json")
    }

    /**
     * Returns `true` when [FirebaseApp] has been initialized (either
     * from runtime config or from the bundled JSON).
     */
    fun isInitialized(): Boolean = initialized || try {
        FirebaseApp.getInstance()
        true
    } catch (_: IllegalStateException) {
        false
    }

    /** Convenience accessor for [FirebaseAuth]. */
    fun getAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** Convenience accessor for [FirebaseFirestore]. */
    fun getFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
