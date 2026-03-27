package com.getaltair.kairos.dashboard.data

import com.getaltair.kairos.dashboard.config.DashboardConfig
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.FileInputStream
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory

/**
 * Firestore client backed by the Firebase Admin SDK (JVM).
 *
 * Provides real-time [Flow]s of [Habit] and [Completion] documents for a
 * given user.  Collection paths follow the structure defined in
 * `sync/FirestoreCollections`:  `users/{userId}/habits` and
 * `users/{userId}/completions`.
 */
class FirebaseAdminClient(private val config: DashboardConfig) {

    private val log = LoggerFactory.getLogger(FirebaseAdminClient::class.java)

    private lateinit var firestore: Firestore

    /**
     * Initialises the Firebase Admin SDK and obtains a [Firestore] instance.
     * Safe to call multiple times -- skips initialisation if an app already exists.
     */
    fun initialize() {
        val serviceAccount = FileInputStream(config.firebaseServiceAccountPath)
        val credentials = GoogleCredentials.fromStream(serviceAccount)
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
        firestore = FirestoreClient.getFirestore()
        log.info("Firebase Admin SDK initialised")
    }

    /**
     * Emits the current list of **active** [Habit]s every time the
     * `users/{userId}/habits` collection changes.
     */
    fun habitsFlow(userId: String): Flow<List<Habit>> = callbackFlow {
        val collection = firestore
            .collection("users").document(userId)
            .collection("habits")
        val query = collection.whereEqualTo("status", "ACTIVE")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log.warn("Habits snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.let { qs ->
                val habits = qs.documents.mapNotNull { doc ->
                    runCatching {
                        AdminFirestoreMapper.habitFromMap(doc.id, doc.data)
                    }.onFailure { e ->
                        log.warn("Failed to parse habit {}: {}", doc.id, e.message)
                    }.getOrNull()
                }
                trySend(habits)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Emits the current list of [Completion]s for the given [date] every time
     * the `users/{userId}/completions` collection changes.
     */
    fun completionsFlow(userId: String, date: LocalDate): Flow<List<Completion>> = callbackFlow {
        val collection = firestore
            .collection("users").document(userId)
            .collection("completions")
        val query = collection.whereEqualTo("date", date.toString())

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log.warn("Completions snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.let { qs ->
                val completions = qs.documents.mapNotNull { doc ->
                    runCatching {
                        AdminFirestoreMapper.completionFromMap(doc.id, doc.data)
                    }.onFailure { e ->
                        log.warn("Failed to parse completion {}: {}", doc.id, e.message)
                    }.getOrNull()
                }
                trySend(completions)
            }
        }
        awaitClose { registration.remove() }
    }

    /** Cleans up resources when the dashboard shuts down. */
    fun close() {
        log.info("Firebase Admin client closing")
    }
}
