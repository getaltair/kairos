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
 * given user.  Collection paths: `users/{userId}/habits` and
 * `users/{userId}/completions`.
 */
open class FirebaseAdminClient(private val config: DashboardConfig) {

    private val log = LoggerFactory.getLogger(FirebaseAdminClient::class.java)

    private var firestore: Firestore? = null

    private fun db(): Firestore =
        firestore ?: error("FirebaseAdminClient has not been initialized. Call initialize() first.")

    /**
     * Initialises the Firebase Admin SDK and obtains a [Firestore] instance.
     * Safe to call multiple times -- skips initialisation if an app already exists.
     */
    fun initialize() {
        val options = if (config.useEmulator) {
            log.info(
                "Emulator mode: connecting to FIRESTORE_EMULATOR_HOST={}",
                System.getenv("FIRESTORE_EMULATOR_HOST")
            )
            FirebaseOptions.builder()
                .setProjectId("demo-kairos")
                .build()
        } else {
            val serviceAccount = FileInputStream(config.firebaseServiceAccountPath)
            val credentials = GoogleCredentials.fromStream(serviceAccount)
            FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()
        }
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
        firestore = FirestoreClient.getFirestore()
        log.info("Firebase Admin SDK initialised (emulator={})", config.useEmulator)
    }

    /**
     * Emits the current list of **active** [Habit]s every time the
     * `users/{userId}/habits` collection changes.
     */
    fun habitsFlow(userId: String): Flow<List<Habit>> = callbackFlow {
        val collection = db()
            .collection("users").document(userId)
            .collection("habits")
        val query = collection.whereEqualTo("status", "ACTIVE")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log.error("Habits snapshot listener error", error)
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
                val sendResult = trySend(habits)
                if (sendResult.isFailure) {
                    log.warn("Failed to emit habits update", sendResult.exceptionOrNull())
                }
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Emits the current list of [Completion]s for the given [date] every time
     * the `users/{userId}/completions` collection changes.
     */
    fun completionsFlow(userId: String, date: LocalDate): Flow<List<Completion>> = callbackFlow {
        val collection = db()
            .collection("users").document(userId)
            .collection("completions")
        val query = collection.whereEqualTo("date", date.toString())

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log.error("Completions snapshot listener error", error)
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
                val sendResult = trySend(completions)
                if (sendResult.isFailure) {
                    log.warn("Failed to emit completions update", sendResult.exceptionOrNull())
                }
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Writes a completion document to Firestore at
     * `users/{userId}/completions/{completionId}`.
     *
     * @param userId the user document ID
     * @param completionId the completion document ID
     * @param data the Firestore-compatible map produced by [AdminFirestoreMapper.completionToMap]
     * @return [Result.success] on successful write, [Result.failure] on error
     */
    open fun writeCompletion(userId: String, completionId: String, data: Map<String, Any?>): Result<Unit> {
        log.info("Writing completion {} for user {}", completionId, userId)
        return try {
            db().collection("users").document(userId)
                .collection("completions").document(completionId)
                .set(data).get()
            log.info("Completion {} written successfully", completionId)
            Result.success(Unit)
        } catch (e: java.util.concurrent.ExecutionException) {
            val cause = e.cause ?: e
            log.error(
                "Firestore write failed for completion {} user {}: {}",
                completionId,
                userId,
                cause.message,
                cause,
            )
            Result.failure(cause)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error("Interrupted writing completion {} for user {}", completionId, userId, e)
            Result.failure(e)
        }
    }

    /** Cleans up resources when the dashboard shuts down. */
    fun close() {
        log.info("Firebase Admin client closing")
        try {
            firestore?.close()
        } catch (e: Exception) {
            log.warn("Error closing Firestore client", e)
        }
    }
}
