package com.getaltair.kairos.sync

import com.getaltair.kairos.data.dao.CompletionDao
import com.getaltair.kairos.data.dao.HabitDao
import com.getaltair.kairos.data.dao.RecoverySessionDao
import com.getaltair.kairos.data.dao.RoutineDao
import com.getaltair.kairos.data.dao.RoutineExecutionDao
import com.getaltair.kairos.data.dao.RoutineHabitDao
import com.getaltair.kairos.data.dao.RoutineVariantDao
import com.getaltair.kairos.data.dao.UserPreferencesDao
import com.getaltair.kairos.data.mapper.CompletionEntityMapper
import com.getaltair.kairos.data.mapper.HabitEntityMapper
import com.getaltair.kairos.data.mapper.RecoverySessionEntityMapper
import com.getaltair.kairos.data.mapper.RoutineEntityMapper
import com.getaltair.kairos.data.mapper.RoutineExecutionEntityMapper
import com.getaltair.kairos.data.mapper.RoutineHabitEntityMapper
import com.getaltair.kairos.data.mapper.RoutineVariantEntityMapper
import com.getaltair.kairos.data.mapper.UserPreferencesEntityMapper
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.sync.SyncState
import com.getaltair.kairos.domain.sync.SyncStateProvider
import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.sync.firestore.FirestoreCollections
import com.getaltair.kairos.sync.firestore.FirestoreMapper
import com.getaltair.kairos.sync.firestore.toFirestoreMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Core synchronization engine that bridges local Room storage with Firestore.
 *
 * Responsibilities:
 * - Listens to Firestore snapshot changes and writes them into local DAOs.
 * - Pushes local writes and deletions to Firestore.
 * - Performs an initial merge when a user first signs in.
 * - Observes auth state to start/stop sync automatically.
 */
class SyncManager(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    private val routineDao: RoutineDao,
    private val routineHabitDao: RoutineHabitDao,
    private val routineVariantDao: RoutineVariantDao,
    private val routineExecutionDao: RoutineExecutionDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val userPreferencesDao: UserPreferencesDao,
) : SyncTrigger,
    SyncStateProvider {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.NotSignedIn)

    /** Observable sync state for UI consumption. */
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val activeRoutineListeners = mutableSetOf<String>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ------------------------------------------------------------------
    // Snapshot listening
    // ------------------------------------------------------------------

    /**
     * Starts real-time Firestore snapshot listeners for all user subcollections.
     * Each incoming document change is mapped to its domain entity, converted
     * to a Room entity, and upserted or deleted locally.
     */
    fun startListening(userId: String) {
        // Remove any existing listeners to prevent duplicates on re-entry
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        activeRoutineListeners.clear()

        attachHabitListener(userId)
        attachCompletionListener(userId)
        attachRoutineListener(userId)
        attachRoutineExecutionListener(userId)
        attachRecoverySessionListener(userId)
        attachUserPreferencesListener(userId)
        // RoutineHabit and RoutineVariant listeners are attached per-routine
        // via a separate routines collection listener that discovers routine IDs
        // and subscribes to their subcollections.
        attachRoutineSubcollectionListeners(userId)
    }

    /**
     * Removes all active Firestore snapshot listeners and resets sync state.
     */
    fun stopListening() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        _syncState.value = SyncState.NotSignedIn
    }

    /**
     * Stops all listeners and cancels the internal coroutine scope.
     * Call this when the SyncManager is no longer needed.
     */
    fun close() {
        stopListening()
        coroutineScope.cancel()
    }

    // ------------------------------------------------------------------
    // Account deletion
    // ------------------------------------------------------------------

    /**
     * Deletes ALL Firestore data for a user, then the user document itself.
     *
     * This is used during account deletion to wipe cloud data before the
     * Firebase Auth account is removed. The method:
     * 1. Stops all snapshot listeners (prevents callbacks during deletion).
     * 2. Deletes documents from every subcollection under `users/{userId}`.
     * 3. For routines, also deletes nested subcollections (habits, variants).
     * 4. Deletes the user document at `users/{userId}`.
     *
     * Firestore batches are limited to 500 operations, so documents are
     * committed in chunks when a subcollection exceeds that limit.
     */
    suspend fun deleteAllUserData(userId: String) {
        require(userId.isNotBlank()) { "userId must not be blank" }

        Timber.i("Starting full data deletion for user %s", userId)
        stopListening()

        try {
            // --- Routines require special handling for nested subcollections ---
            val routinesPath = FirestoreCollections.routines(userId).value
            val routineDocs = firestore.collection(routinesPath).get().await().documents
            Timber.d("Found %d routine(s) to clean up", routineDocs.size)

            for (routineDoc in routineDocs) {
                val routineId = routineDoc.id
                // Delete routine_habits subcollection
                val habitsPath = FirestoreCollections.routineHabits(userId, routineId).value
                deleteCollection(habitsPath, "routine_habits for routine $routineId")
                // Delete routine_variants subcollection
                val variantsPath = FirestoreCollections.routineVariants(userId, routineId).value
                deleteCollection(variantsPath, "routine_variants for routine $routineId")
            }

            // --- Delete top-level subcollections ---
            deleteCollection(
                FirestoreCollections.habits(userId).value,
                "habits",
            )
            deleteCollection(
                FirestoreCollections.completions(userId).value,
                "completions",
            )
            deleteCollection(
                routinesPath,
                "routines",
            )
            deleteCollection(
                FirestoreCollections.routineExecutions(userId).value,
                "routine_executions",
            )
            deleteCollection(
                FirestoreCollections.recoverySessions(userId).value,
                "recovery_sessions",
            )
            deleteCollection(
                FirestoreCollections.preferences(userId).value,
                "preferences",
            )
            deleteCollection(
                FirestoreCollections.deletions(userId).value,
                "deletions",
            )

            // --- Delete the user document itself ---
            val userDocPath = FirestoreCollections.user(userId).value
            firestore.document(userDocPath).delete().await()
            Timber.i("Deleted user document at %s", userDocPath)

            Timber.i("Completed full data deletion for user %s", userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all user data for %s (partial deletion may have occurred)", userId)
            throw e
        }
    }

    /**
     * Deletes all documents in a Firestore collection, respecting the
     * 500-operation batch limit. Documents are fetched and deleted in
     * batches until the collection is empty.
     *
     * @param collectionPath Full Firestore collection path.
     * @param label Human-readable label for logging.
     */
    private suspend fun deleteCollection(collectionPath: String, label: String) {
        val collectionRef = firestore.collection(collectionPath)
        var totalDeleted = 0

        try {
            while (true) {
                val snapshot = collectionRef.limit(BATCH_LIMIT.toLong()).get().await()
                if (snapshot.isEmpty) break

                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                totalDeleted += snapshot.size()
                Timber.d("Deleted batch of %d document(s) from %s (total: %d)", snapshot.size(), label, totalDeleted)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed deleting %s after %d document(s) already removed", label, totalDeleted)
            throw e
        }

        if (totalDeleted > 0) {
            Timber.d("Deleted %d document(s) from %s", totalDeleted, label)
        }
    }

    // ------------------------------------------------------------------
    // Push operations
    // ------------------------------------------------------------------

    /**
     * Pushes a local entity change to Firestore.
     *
     * Non-cancellation exceptions are caught and logged rather than propagated.
     * Note: this is a suspending function that awaits the Firestore write;
     * fire-and-forget semantics are achieved by the caller launching this in
     * a non-blocking scope.
     *
     * @param userId      The owning user's ID.
     * @param entityType  Which entity collection to write to.
     * @param id          The document ID (entity primary key as string).
     * @param firestoreMap The Firestore-compatible map produced by `toFirestoreMap()`.
     */
    suspend fun pushLocalChange(
        userId: String,
        entityType: SyncEntityType,
        id: String,
        firestoreMap: Map<String, Any?>,
    ) {
        try {
            val path = collectionPathForType(userId, entityType, firestoreMap)
            firestore.collection(path).document(id).set(firestoreMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleSyncException(e, "pushLocalChange($entityType, $id)")
        }
    }

    /**
     * Records a deletion in Firestore.
     *
     * Writes a tombstone to `users/{userId}/deletions` and removes the
     * actual document from its collection.
     */
    private suspend fun pushDeletion(
        userId: String,
        entityType: SyncEntityType,
        id: String,
        firestoreMap: Map<String, Any?> = emptyMap(),
    ) {
        try {
            val deletionData = mapOf(
                "entityType" to entityType.name,
                "entityId" to id,
                "deletedAt" to Timestamp.now(),
            )
            val deletionRef = firestore
                .collection(FirestoreCollections.deletions(userId).value)
                .document()
            val path = collectionPathForType(userId, entityType, firestoreMap)
            val docRef = firestore.collection(path).document(id)

            firestore.batch()
                .set(deletionRef, deletionData)
                .delete(docRef)
                .commit()
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleSyncException(e, "pushDeletion($entityType, $id)")
        }
    }

    // ------------------------------------------------------------------
    // Initial sync
    // ------------------------------------------------------------------

    /**
     * Performs an initial bidirectional merge between Room and Firestore.
     *
     * Four scenarios:
     * 1. Remote empty, local has data: push all local data to Firestore.
     * 2. Remote has data, local empty: pull all Firestore data into Room.
     * 3. Both have data: per-entity timestamp comparison, newest wins.
     * 4. Both empty: no action needed.
     */
    suspend fun performInitialSync(userId: String) {
        _syncState.value = SyncState.Syncing
        try {
            syncHabits(userId)
            syncCompletions(userId)
            syncRoutines(userId)
            syncRoutineExecutions(userId)
            syncRecoverySessions(userId)
            syncUserPreferences(userId)
            // Routine subcollections (habits/variants) sync after routines
            syncRoutineSubcollections(userId)
            _syncState.value = SyncState.Synced
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleSyncException(e, "performInitialSync")
        }
    }

    // ------------------------------------------------------------------
    // Auth observation
    // ------------------------------------------------------------------

    /**
     * Observes an [AuthState] flow and starts or stops sync accordingly.
     *
     * On [AuthState.SignedIn]: runs initial sync then attaches listeners.
     * On [AuthState.SignedOut]: detaches all listeners.
     */
    fun observeAuthAndSync(authStateFlow: Flow<AuthState>) {
        coroutineScope.launch {
            authStateFlow.collect { state ->
                when (state) {
                    is AuthState.SignedIn -> {
                        performInitialSync(state.userId)
                        if (_syncState.value !is SyncState.Error) {
                            startListening(state.userId)
                        }
                    }

                    is AuthState.SignedOut -> {
                        stopListening()
                    }
                }
            }
        }
    }

    // =========================================================================
    // Private: Snapshot listener attachment
    // =========================================================================

    private fun attachHabitListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.habits(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleHabitChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachCompletionListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.completions(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleCompletionChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachRoutineListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.routines(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleRoutineChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachRoutineExecutionListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.routineExecutions(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleRoutineExecutionChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachRecoverySessionListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.recoverySessions(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleRecoverySessionChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachUserPreferencesListener(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.preferences(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleUserPreferencesChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    /**
     * For each routine currently in Firestore, attaches listeners on
     * the nested `habits` and `variants` subcollections.
     */
    private fun attachRoutineSubcollectionListeners(userId: String) {
        val registration = firestore
            .collection(FirestoreCollections.routines(userId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    val routineId = change.document.id
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (activeRoutineListeners.add(routineId)) {
                                attachRoutineHabitListener(userId, routineId)
                                attachRoutineVariantListener(userId, routineId)
                            }
                        }

                        DocumentChange.Type.REMOVED -> {
                            activeRoutineListeners.remove(routineId)
                            // Listeners for removed routines will be cleaned up on stopListening
                        }

                        DocumentChange.Type.MODIFIED -> { /* no-op for listener management */ }
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachRoutineHabitListener(userId: String, routineId: String) {
        val registration = firestore
            .collection(FirestoreCollections.routineHabits(userId, routineId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleRoutineHabitChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private fun attachRoutineVariantListener(userId: String, routineId: String) {
        val registration = firestore
            .collection(FirestoreCollections.routineVariants(userId, routineId).value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleSnapshotError(error)
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    coroutineScope.launch {
                        handleRoutineVariantChange(change)
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    // =========================================================================
    // Private: Document change handlers
    // =========================================================================

    private suspend fun handleHabitChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.habitFromMap(change.document.data)
                    val entity = HabitEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { habitDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { habitDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle habit change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleCompletionChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.completionFromMap(change.document.data)
                    val entity = CompletionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { completionDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { completionDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle completion change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleRoutineChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.routineFromMap(change.document.data)
                    val entity = RoutineEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { routineDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle routine change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleRoutineHabitChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.routineHabitFromMap(change.document.data)
                    val entity = RoutineHabitEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineHabitDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { routineHabitDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle routine-habit change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleRoutineVariantChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.routineVariantFromMap(change.document.data)
                    val entity = RoutineVariantEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineVariantDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { routineVariantDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle routine-variant change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleRoutineExecutionChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.routineExecutionFromMap(change.document.data)
                    val entity = RoutineExecutionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineExecutionDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { routineExecutionDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle routine-execution change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleRecoverySessionChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.recoverySessionFromMap(change.document.data)
                    val entity = RecoverySessionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { recoverySessionDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { recoverySessionDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle recovery-session change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    private suspend fun handleUserPreferencesChange(change: DocumentChange) {
        try {
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val domain = FirestoreMapper.userPreferencesFromMap(change.document.data)
                    val entity = UserPreferencesEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { userPreferencesDao.upsert(entity) }
                }

                DocumentChange.Type.REMOVED -> {
                    val id = UUID.fromString(change.document.id)
                    withContext(Dispatchers.IO) { userPreferencesDao.delete(id) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle user-preferences change: %s", change.document.id)
            _syncState.value = SyncState.Error(
                message = "Failed to sync: ${e.message}",
                cause = e,
            )
        }
    }

    // =========================================================================
    // Private: Initial sync per entity type
    // =========================================================================

    private suspend fun syncHabits(userId: String) {
        val localEntities = withContext(Dispatchers.IO) { habitDao.getAll() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.habits(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            // Remote empty, local has data: push all local
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = HabitEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.habits(userId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            // Remote has data, local empty: pull all remote
            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.habitFromMap(data)
                    val entity = HabitEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { habitDao.upsert(entity) }
                }
            }

            // Both have data: merge by updatedAt timestamp
            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                val localMap = localEntities.associateBy { it.id.toString() }
                val processedIds = mutableSetOf<String>()

                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val remoteVersion = extractVersion(data)
                    val localEntity = localMap[doc.id]

                    if (localEntity == null) {
                        // Remote-only: pull into Room
                        val domain = FirestoreMapper.habitFromMap(data)
                        val entity = HabitEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { habitDao.upsert(entity) }
                    } else if (remoteVersion > localEntity.updatedAt) {
                        // Remote newer: pull into Room
                        val domain = FirestoreMapper.habitFromMap(data)
                        val entity = HabitEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { habitDao.upsert(entity) }
                    } else if (localEntity.updatedAt > remoteVersion) {
                        // Local newer: push to Firestore
                        val domain = HabitEntityMapper.toDomain(localEntity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.habits(userId).value)
                            .document(doc.id)
                            .set(map)
                            .await()
                    }
                    processedIds.add(doc.id)
                }

                // Local-only entities: push to Firestore
                localEntities
                    .filter { it.id.toString() !in processedIds }
                    .forEach { entity ->
                        val domain = HabitEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.habits(userId).value)
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    }
            }
        }
    }

    private suspend fun syncCompletions(userId: String) {
        val localEntities = withContext(Dispatchers.IO) { completionDao.getAll() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.completions(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = CompletionEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.completions(userId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.completionFromMap(data)
                    val entity = CompletionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { completionDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.completionFromMap(data)
                        val entity = CompletionEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { completionDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = CompletionEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.completions(userId).value)
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncRoutines(userId: String) {
        val localEntities = withContext(Dispatchers.IO) { routineDao.getAll() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.routines(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = RoutineEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.routines(userId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.routineFromMap(data)
                    val entity = RoutineEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.routineFromMap(data)
                        val entity = RoutineEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { routineDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = RoutineEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.routines(userId).value)
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncRoutineSubcollections(userId: String) {
        val routines = withContext(Dispatchers.IO) { routineDao.getAll() }
        for (routine in routines) {
            syncRoutineHabits(userId, routine.id.toString())
            syncRoutineVariants(userId, routine.id.toString())
        }
    }

    private suspend fun syncRoutineHabits(userId: String, routineId: String) {
        val localEntities = withContext(Dispatchers.IO) {
            routineHabitDao.getByRoutineId(UUID.fromString(routineId))
        }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.routineHabits(userId, routineId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = RoutineHabitEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.routineHabits(userId, routineId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.routineHabitFromMap(data)
                    val entity = RoutineHabitEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineHabitDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.routineHabitFromMap(data)
                        val entity = RoutineHabitEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { routineHabitDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = RoutineHabitEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(
                                FirestoreCollections.routineHabits(userId, routineId).value,
                            )
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncRoutineVariants(userId: String, routineId: String) {
        val localEntities = withContext(Dispatchers.IO) {
            routineVariantDao.getByRoutineId(UUID.fromString(routineId))
        }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.routineVariants(userId, routineId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = RoutineVariantEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(
                            FirestoreCollections.routineVariants(userId, routineId).value,
                        )
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.routineVariantFromMap(data)
                    val entity = RoutineVariantEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineVariantDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.routineVariantFromMap(data)
                        val entity = RoutineVariantEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { routineVariantDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = RoutineVariantEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(
                                FirestoreCollections.routineVariants(userId, routineId).value,
                            )
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncRoutineExecutions(userId: String) {
        val localEntities = withContext(Dispatchers.IO) { routineExecutionDao.getAll() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.routineExecutions(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = RoutineExecutionEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.routineExecutions(userId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.routineExecutionFromMap(data)
                    val entity = RoutineExecutionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { routineExecutionDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.routineExecutionFromMap(data)
                        val entity = RoutineExecutionEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { routineExecutionDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = RoutineExecutionEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.routineExecutions(userId).value)
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncRecoverySessions(userId: String) {
        val localEntities = withContext(Dispatchers.IO) { recoverySessionDao.getAll() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.recoverySessions(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntities.isNotEmpty() -> {
                localEntities.forEach { entity ->
                    val domain = RecoverySessionEntityMapper.toDomain(entity)
                    val map = domain.toFirestoreMap()
                    firestore
                        .collection(FirestoreCollections.recoverySessions(userId).value)
                        .document(entity.id.toString())
                        .set(map)
                        .await()
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isEmpty() -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.recoverySessionFromMap(data)
                    val entity = RecoverySessionEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { recoverySessionDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntities.isNotEmpty() -> {
                mergeEntities(
                    remoteDocs = remoteDocs,
                    localEntities = localEntities,
                    localId = { it.id.toString() },
                    localUpdatedAt = { it.updatedAt },
                    pullRemote = { data ->
                        val domain = FirestoreMapper.recoverySessionFromMap(data)
                        val entity = RecoverySessionEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { recoverySessionDao.upsert(entity) }
                    },
                    pushLocal = { entity ->
                        val domain = RecoverySessionEntityMapper.toDomain(entity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.recoverySessions(userId).value)
                            .document(entity.id.toString())
                            .set(map)
                            .await()
                    },
                )
            }
        }
    }

    private suspend fun syncUserPreferences(userId: String) {
        val localEntity = withContext(Dispatchers.IO) { userPreferencesDao.get() }
        val remoteSnapshot = firestore
            .collection(FirestoreCollections.preferences(userId).value)
            .get().await()
        val remoteDocs = remoteSnapshot.documents

        when {
            remoteDocs.isEmpty() && localEntity != null -> {
                val domain = UserPreferencesEntityMapper.toDomain(localEntity)
                val map = domain.toFirestoreMap()
                firestore
                    .collection(FirestoreCollections.preferences(userId).value)
                    .document(localEntity.id.toString())
                    .set(map)
                    .await()
            }

            remoteDocs.isNotEmpty() && localEntity == null -> {
                remoteDocs.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val domain = FirestoreMapper.userPreferencesFromMap(data)
                    val entity = UserPreferencesEntityMapper.toEntity(domain)
                    withContext(Dispatchers.IO) { userPreferencesDao.upsert(entity) }
                }
            }

            remoteDocs.isNotEmpty() && localEntity != null -> {
                val remoteDoc = remoteDocs.first()
                val data = remoteDoc.data
                if (data != null) {
                    val remoteVersion = extractVersion(data)
                    if (remoteVersion > localEntity.updatedAt) {
                        val domain = FirestoreMapper.userPreferencesFromMap(data)
                        val entity = UserPreferencesEntityMapper.toEntity(domain)
                        withContext(Dispatchers.IO) { userPreferencesDao.upsert(entity) }
                    } else if (localEntity.updatedAt > remoteVersion) {
                        val domain = UserPreferencesEntityMapper.toDomain(localEntity)
                        val map = domain.toFirestoreMap()
                        firestore
                            .collection(FirestoreCollections.preferences(userId).value)
                            .document(remoteDoc.id)
                            .set(map)
                            .await()
                    }
                }
            }
        }
    }

    // =========================================================================
    // Private: Helpers
    // =========================================================================

    /**
     * Generic merge helper that compares remote vs local by updatedAt timestamps.
     */
    private suspend fun <T> mergeEntities(
        remoteDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        localEntities: List<T>,
        localId: (T) -> String,
        localUpdatedAt: (T) -> Long,
        pullRemote: suspend (Map<String, Any?>) -> Unit,
        pushLocal: suspend (T) -> Unit,
    ) {
        val localMap = localEntities.associateBy { localId(it) }
        val processedIds = mutableSetOf<String>()

        remoteDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val remoteVersion = extractVersion(data)
            val local = localMap[doc.id]

            if (local == null) {
                pullRemote(data)
            } else if (remoteVersion > localUpdatedAt(local)) {
                pullRemote(data)
            } else if (localUpdatedAt(local) > remoteVersion) {
                pushLocal(local)
            }
            processedIds.add(doc.id)
        }

        // Local-only entities: push
        localEntities
            .filter { localId(it) !in processedIds }
            .forEach { pushLocal(it) }
    }

    /**
     * Extracts the `updatedAt` timestamp from a Firestore map as epoch millis.
     * Falls back to `version` field, then 0.
     */
    private fun extractVersion(data: Map<String, Any?>): Long {
        val updatedAt = data["updatedAt"]
        if (updatedAt is Timestamp) {
            return updatedAt.toInstant().toEpochMilli()
        }
        val version = data["version"]
        if (version is Number) {
            return version.toLong()
        }
        Timber.w(
            "Could not extract version from Firestore document, updatedAt=%s, version=%s",
            updatedAt?.javaClass?.simpleName,
            version?.javaClass?.simpleName,
        )
        return 0L
    }

    /**
     * Resolves the Firestore collection path for a given entity type.
     *
     * For [SyncEntityType.ROUTINE_HABIT] and [SyncEntityType.ROUTINE_VARIANT],
     * the `routineId` is extracted from the [firestoreMap].
     */
    private fun collectionPathForType(
        userId: String,
        entityType: SyncEntityType,
        firestoreMap: Map<String, Any?>,
    ): String = when (entityType) {
        SyncEntityType.HABIT ->
            FirestoreCollections.habits(userId).value

        SyncEntityType.COMPLETION ->
            FirestoreCollections.completions(userId).value

        SyncEntityType.ROUTINE ->
            FirestoreCollections.routines(userId).value

        SyncEntityType.ROUTINE_HABIT -> {
            val routineId = firestoreMap["routineId"] as? String
                ?: throw IllegalArgumentException("routineId required for ROUTINE_HABIT")
            FirestoreCollections.routineHabits(userId, routineId).value
        }

        SyncEntityType.ROUTINE_VARIANT -> {
            val routineId = firestoreMap["routineId"] as? String
                ?: throw IllegalArgumentException("routineId required for ROUTINE_VARIANT")
            FirestoreCollections.routineVariants(userId, routineId).value
        }

        SyncEntityType.ROUTINE_EXECUTION ->
            FirestoreCollections.routineExecutions(userId).value

        SyncEntityType.RECOVERY_SESSION ->
            FirestoreCollections.recoverySessions(userId).value

        SyncEntityType.USER_PREFERENCE ->
            FirestoreCollections.preferences(userId).value
    }

    private fun handleSnapshotError(error: FirebaseFirestoreException) {
        if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            _syncState.value = SyncState.Offline
            Timber.w("Firestore unavailable, switching to offline mode")
        } else {
            _syncState.value = SyncState.Error(error.message ?: "Unknown Firestore error")
            Timber.e(error, "Firestore snapshot listener error")
        }
    }

    private fun handleSyncException(e: Exception, operation: String) {
        if (e is FirebaseFirestoreException &&
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE
        ) {
            _syncState.value = SyncState.Offline
            Timber.w("Firestore unavailable during %s", operation)
        } else {
            _syncState.value = SyncState.Error(e.message ?: "Unknown sync error")
            Timber.e(e, "Sync error during %s", operation)
        }
    }

    // ------------------------------------------------------------------
    // SyncTrigger implementation
    // ------------------------------------------------------------------

    override suspend fun triggerPush(userId: String, entityType: String, id: String, entity: Any) {
        val type = try {
            SyncEntityType.valueOf(entityType)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Unknown entity type for sync push: %s", entityType)
            return
        }
        val firestoreMap = entityToFirestoreMap(entity)
        pushLocalChange(userId, type, id, firestoreMap)
    }

    override suspend fun triggerDeletion(
        userId: String,
        entityType: String,
        id: String,
        metadata: Map<String, String>,
    ) {
        val type = try {
            SyncEntityType.valueOf(entityType)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Unknown entity type for sync deletion: %s", entityType)
            return
        }
        val firestoreMap: Map<String, Any?> = metadata
        pushDeletion(userId, type, id, firestoreMap)
    }

    /**
     * Converts a domain entity to its Firestore-compatible map representation.
     * Delegates to the appropriate `toFirestoreMap()` extension defined as
     * top-level extensions in the firestore package (alongside FirestoreMapper).
     */
    private fun entityToFirestoreMap(entity: Any): Map<String, Any?> = when (entity) {
        is com.getaltair.kairos.domain.entity.Habit -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.Completion -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.Routine -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.RoutineHabit -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.RoutineVariant -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.RoutineExecution -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.RecoverySession -> entity.toFirestoreMap()
        is com.getaltair.kairos.domain.entity.UserPreferences -> entity.toFirestoreMap()
        else -> throw IllegalArgumentException("Unknown entity type: ${entity::class.simpleName}")
    }

    companion object {
        /** Firestore WriteBatch maximum operations per commit. */
        private const val BATCH_LIMIT = 500
    }
}
