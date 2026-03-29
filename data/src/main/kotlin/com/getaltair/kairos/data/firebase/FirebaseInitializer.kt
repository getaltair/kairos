package com.getaltair.kairos.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * Initializes [FirebaseApp] either from a runtime-supplied [FirebaseConfig]
 * or from the bundled `google-services.json` (when the google-services
 * Gradle plugin has already handled initialization).
 *
 * The rest of the app accesses Firebase services via the Koin DI graph
 * ([com.getaltair.kairos.di.firebaseModule]), not through this object.
 */
object FirebaseInitializer {

    private val initialized = AtomicBoolean(false)

    /**
     * Initialize Firebase from a user-supplied [FirebaseConfig].
     * Call this when no `google-services.json` is bundled.
     */
    fun initialize(context: Context, config: FirebaseConfig) {
        if (!initialized.compareAndSet(false, true)) {
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

        try {
            FirebaseApp.initializeApp(context.applicationContext, options)
            Timber.d("Firebase initialized from runtime config (project=%s)", config.projectId)
        } catch (e: Exception) {
            initialized.set(false)
            throw e
        }
    }

    /**
     * Marks Firebase as initialized. Call this when the google-services
     * plugin already handled bootstrapping via `google-services.json`.
     */
    fun initializeFromExisting() {
        if (!initialized.compareAndSet(false, true)) return
        Timber.d("Firebase marked as initialized from google-services.json")
    }

    /**
     * Returns `true` when [FirebaseApp] has been initialized (either
     * from runtime config or from the bundled JSON).
     */
    fun isInitialized(): Boolean = initialized.get() || try {
        FirebaseApp.getInstance()
        true
    } catch (e: IllegalStateException) {
        Timber.d(e, "FirebaseApp.getInstance() not available")
        false
    }
}
