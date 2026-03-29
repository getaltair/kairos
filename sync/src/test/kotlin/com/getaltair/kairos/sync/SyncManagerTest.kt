package com.getaltair.kairos.sync

import com.getaltair.kairos.data.dao.CompletionDao
import com.getaltair.kairos.data.dao.HabitDao
import com.getaltair.kairos.data.dao.RecoverySessionDao
import com.getaltair.kairos.data.dao.RoutineDao
import com.getaltair.kairos.data.dao.RoutineExecutionDao
import com.getaltair.kairos.data.dao.RoutineHabitDao
import com.getaltair.kairos.data.dao.RoutineVariantDao
import com.getaltair.kairos.data.dao.UserPreferencesDao
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.sync.SyncState
import com.getaltair.kairos.sync.firestore.FirestoreCollections
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SyncManagerTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: CompletionDao
    private lateinit var routineDao: RoutineDao
    private lateinit var routineHabitDao: RoutineHabitDao
    private lateinit var routineVariantDao: RoutineVariantDao
    private lateinit var routineExecutionDao: RoutineExecutionDao
    private lateinit var recoverySessionDao: RecoverySessionDao
    private lateinit var userPreferencesDao: UserPreferencesDao
    private lateinit var syncManager: SyncManager

    private val userId = "test-user-123"

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        habitDao = mockk(relaxed = true)
        completionDao = mockk(relaxed = true)
        routineDao = mockk(relaxed = true)
        routineHabitDao = mockk(relaxed = true)
        routineVariantDao = mockk(relaxed = true)
        routineExecutionDao = mockk(relaxed = true)
        recoverySessionDao = mockk(relaxed = true)
        userPreferencesDao = mockk(relaxed = true)

        syncManager = SyncManager(
            firestore = firestore,
            auth = auth,
            habitDao = habitDao,
            completionDao = completionDao,
            routineDao = routineDao,
            routineHabitDao = routineHabitDao,
            routineVariantDao = routineVariantDao,
            routineExecutionDao = routineExecutionDao,
            recoverySessionDao = recoverySessionDao,
            userPreferencesDao = userPreferencesDao,
        )
    }

    // ------------------------------------------------------------------
    // pushLocalChange
    // ------------------------------------------------------------------

    @Test
    fun `pushLocalChange writes to correct Firestore path for habits`() = runTest {
        val habitId = UUID.randomUUID().toString()
        val firestoreMap = mapOf<String, Any?>("id" to habitId, "name" to "Test Habit")
        val expectedPath = FirestoreCollections.habits(userId).value

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection(expectedPath) } returns collectionRef
        every { collectionRef.document(habitId) } returns docRef
        every { docRef.set(firestoreMap) } returns Tasks.forResult(null)

        syncManager.pushLocalChange(userId, SyncEntityType.HABIT, habitId, firestoreMap)

        verify { firestore.collection(expectedPath) }
        verify { collectionRef.document(habitId) }
        verify { docRef.set(firestoreMap) }
    }

    @Test
    fun `pushLocalChange writes to correct Firestore path for completions`() = runTest {
        val completionId = UUID.randomUUID().toString()
        val firestoreMap = mapOf<String, Any?>("id" to completionId)
        val expectedPath = FirestoreCollections.completions(userId).value

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection(expectedPath) } returns collectionRef
        every { collectionRef.document(completionId) } returns docRef
        every { docRef.set(firestoreMap) } returns Tasks.forResult(null)

        syncManager.pushLocalChange(userId, SyncEntityType.COMPLETION, completionId, firestoreMap)

        verify { firestore.collection(expectedPath) }
    }

    @Test
    fun `pushLocalChange writes to correct nested path for routine habits`() = runTest {
        val id = UUID.randomUUID().toString()
        val routineId = UUID.randomUUID().toString()
        val firestoreMap = mapOf<String, Any?>(
            "id" to id,
            "routineId" to routineId,
        )
        val expectedPath = FirestoreCollections.routineHabits(userId, routineId).value

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection(expectedPath) } returns collectionRef
        every { collectionRef.document(id) } returns docRef
        every { docRef.set(firestoreMap) } returns Tasks.forResult(null)

        syncManager.pushLocalChange(userId, SyncEntityType.ROUTINE_HABIT, id, firestoreMap)

        verify { firestore.collection(expectedPath) }
    }

    @Test
    fun `pushLocalChange does not throw on Firestore error`() = runTest {
        val habitId = UUID.randomUUID().toString()
        val firestoreMap = mapOf<String, Any?>("id" to habitId)

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        val expectedPath = FirestoreCollections.habits(userId).value

        every { firestore.collection(expectedPath) } returns collectionRef
        every { collectionRef.document(habitId) } returns docRef
        every { docRef.set(firestoreMap) } returns Tasks.forException(
            RuntimeException("Network error"),
        )

        // Should not throw
        syncManager.pushLocalChange(userId, SyncEntityType.HABIT, habitId, firestoreMap)
    }

    // ------------------------------------------------------------------
    // performInitialSync: empty remote -> pushes local data
    // ------------------------------------------------------------------

    @Test
    fun `performInitialSync pushes local data when remote is empty`() = runTest {
        val habitEntity = createTestHabitEntity()

        // Local has data
        every { habitDao.getAll() } returns listOf(habitEntity)
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null

        // Remote is empty for all collections
        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.get() } returns Tasks.forResult(emptySnapshot)

        val docRef = mockk<DocumentReference>(relaxed = true)
        every { collectionRef.document(any()) } returns docRef
        every { docRef.set(any()) } returns Tasks.forResult(null)

        syncManager.performInitialSync(userId)

        // Habit should be pushed to Firestore
        verify {
            collectionRef.document(habitEntity.id.toString())
        }
        verify {
            docRef.set(any())
        }
    }

    // ------------------------------------------------------------------
    // performInitialSync: empty local -> pulls remote data
    // ------------------------------------------------------------------

    @Test
    fun `performInitialSync pulls remote data when local is empty`() = runTest {
        // Local is empty
        every { habitDao.getAll() } returns emptyList()
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null
        coEvery { habitDao.upsert(any()) } returns Unit

        // Remote has habit data
        val habitId = UUID.randomUUID()
        val remoteData = createTestFirestoreHabitMap(habitId)

        val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { docSnapshot.id } returns habitId.toString()
        every { docSnapshot.data } returns remoteData

        val habitSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { habitSnapshot.documents } returns listOf(docSnapshot)

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.get() } returns Tasks.forResult(habitSnapshot)

        // All other collections return empty
        every { firestore.collection(neq(FirestoreCollections.habits(userId).value)) } returns
            mockk(relaxed = true) {
                every { get() } returns Tasks.forResult(emptySnapshot)
            }

        syncManager.performInitialSync(userId)

        // Habit should be upserted into local DAO
        coVerify { habitDao.upsert(any()) }
    }

    // ------------------------------------------------------------------
    // Sync failure does not affect local state
    // ------------------------------------------------------------------

    @Test
    fun `sync failure sets error state but does not throw`() = runTest {
        every { habitDao.getAll() } returns emptyList()
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.get() } returns Tasks.forException(
            RuntimeException("Connection failed"),
        )

        // Should not throw
        syncManager.performInitialSync(userId)

        // State should reflect the error
        val state = syncManager.syncState.value
        assert(state is SyncState.Error) {
            "Expected SyncState.Error but got $state"
        }
    }

    // ------------------------------------------------------------------
    // stopListening clears state
    // ------------------------------------------------------------------

    @Test
    fun `stopListening sets state to NotSignedIn`() {
        syncManager.stopListening()
        assert(syncManager.syncState.value is SyncState.NotSignedIn)
    }

    // ------------------------------------------------------------------
    // SyncState initial value
    // ------------------------------------------------------------------

    @Test
    fun `initial sync state is NotSignedIn`() {
        assert(syncManager.syncState.value is SyncState.NotSignedIn)
    }

    // ------------------------------------------------------------------
    // triggerDeletion -> pushDeletion (WriteBatch)
    // ------------------------------------------------------------------

    @Test
    fun `triggerDeletion writes tombstone and deletes document atomically using WriteBatch`() = runTest {
        val habitId = UUID.randomUUID().toString()

        val batchMock = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batchMock
        val tombstoneSlot = slot<Map<String, Any?>>()
        every { batchMock.set(any(), capture(tombstoneSlot)) } returns batchMock
        every { batchMock.delete(any()) } returns batchMock
        every { batchMock.commit() } returns Tasks.forResult(null)

        val deletionsCollection = mockk<CollectionReference>(relaxed = true)
        val deletionDocRef = mockk<DocumentReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.deletions(userId).value)
        } returns deletionsCollection
        every { deletionsCollection.document() } returns deletionDocRef

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        val habitDocRef = mockk<DocumentReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.document(habitId) } returns habitDocRef

        syncManager.triggerDeletion(userId, "HABIT", habitId)

        verify { firestore.batch() }
        verify { batchMock.set(deletionDocRef, any()) }
        assert(tombstoneSlot.isCaptured) { "Tombstone data should have been captured" }
        val tombstone = tombstoneSlot.captured
        assert(tombstone["entityType"] == "HABIT") { "entityType should be HABIT" }
        assert(tombstone["entityId"] == habitId) { "entityId should be $habitId" }
        assert(tombstone.containsKey("deletedAt")) { "deletedAt should be present" }
        verify { batchMock.delete(habitDocRef) }
        verify { batchMock.commit() }
    }

    @Test
    fun `triggerDeletion handles Firestore failure and sets SyncState to Error`() = runTest {
        val habitId = UUID.randomUUID().toString()

        val batchMock = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batchMock
        every { batchMock.set(any(), any()) } returns batchMock
        every { batchMock.delete(any()) } returns batchMock
        every { batchMock.commit() } returns Tasks.forException(
            RuntimeException("Permission denied"),
        )

        val deletionsCollection = mockk<CollectionReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.deletions(userId).value)
        } returns deletionsCollection
        every { deletionsCollection.document() } returns mockk(relaxed = true)

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.document(habitId) } returns mockk(relaxed = true)

        syncManager.triggerDeletion(userId, "HABIT", habitId)

        val state = syncManager.syncState.value
        assert(state is SyncState.Error) {
            "Expected SyncState.Error but got $state"
        }
    }

    @Test
    fun `triggerDeletion for ROUTINE_HABIT passes routineId from metadata to build correct collection path`() =
        runTest {
            val id = UUID.randomUUID().toString()
            val routineId = UUID.randomUUID().toString()
            val expectedPath = FirestoreCollections.routineHabits(userId, routineId).value

            val batchMock = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
            every { firestore.batch() } returns batchMock
            every { batchMock.set(any(), any()) } returns batchMock
            every { batchMock.delete(any()) } returns batchMock
            every { batchMock.commit() } returns Tasks.forResult(null)

            val deletionsCollection = mockk<CollectionReference>(relaxed = true)
            every {
                firestore.collection(FirestoreCollections.deletions(userId).value)
            } returns deletionsCollection
            every { deletionsCollection.document() } returns mockk(relaxed = true)

            val routineHabitsCollection = mockk<CollectionReference>(relaxed = true)
            every { firestore.collection(expectedPath) } returns routineHabitsCollection
            every { routineHabitsCollection.document(id) } returns mockk(relaxed = true)

            syncManager.triggerDeletion(
                userId,
                "ROUTINE_HABIT",
                id,
                mapOf("routineId" to routineId),
            )

            verify { firestore.collection(expectedPath) }
        }

    // ------------------------------------------------------------------
    // triggerPush / triggerDeletion: unknown entityType
    // ------------------------------------------------------------------

    @Test
    fun `triggerPush with unknown entityType returns without pushing`() = runTest {
        syncManager.triggerPush(userId, "UNKNOWN_TYPE", "some-id", mockk())

        // No Firestore write should have occurred
        verify(exactly = 0) { firestore.collection(any()) }
        // syncState should remain unchanged (NotSignedIn from init)
        assert(syncManager.syncState.value is SyncState.NotSignedIn) {
            "Expected SyncState.NotSignedIn but got ${syncManager.syncState.value}"
        }
    }

    @Test
    fun `triggerDeletion with unknown entityType returns without pushing`() = runTest {
        syncManager.triggerDeletion(userId, "UNKNOWN_TYPE", "some-id")

        // No Firestore write should have occurred
        verify(exactly = 0) { firestore.batch() }
        // syncState should remain unchanged
        assert(syncManager.syncState.value is SyncState.NotSignedIn) {
            "Expected SyncState.NotSignedIn but got ${syncManager.syncState.value}"
        }
    }

    // ------------------------------------------------------------------
    // Conflict resolution: remote newer wins
    // ------------------------------------------------------------------

    @Test
    fun `performInitialSync remote newer than local - remote wins`() = runTest {
        val habitId = UUID.randomUUID()
        val oldTimestamp = 1000L
        val newTimestamp = 2000L

        // Local has older data
        val localEntity = createTestHabitEntity(id = habitId, updatedAt = oldTimestamp)
        every { habitDao.getAll() } returns listOf(localEntity)
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null
        coEvery { habitDao.upsert(any()) } returns Unit

        // Remote has newer data (version > local updatedAt)
        val remoteData = createTestFirestoreHabitMap(habitId, version = newTimestamp)

        val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { docSnapshot.id } returns habitId.toString()
        every { docSnapshot.data } returns remoteData

        val habitSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { habitSnapshot.documents } returns listOf(docSnapshot)

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.get() } returns Tasks.forResult(habitSnapshot)

        every { firestore.collection(neq(FirestoreCollections.habits(userId).value)) } returns
            mockk(relaxed = true) {
                every { get() } returns Tasks.forResult(emptySnapshot)
            }

        syncManager.performInitialSync(userId)

        // Remote wins: local DAO should be updated
        coVerify { habitDao.upsert(any()) }
    }

    // ------------------------------------------------------------------
    // Conflict resolution: local newer wins
    // ------------------------------------------------------------------

    @Test
    fun `performInitialSync local newer than remote - local wins and pushes to Firestore`() = runTest {
        val habitId = UUID.randomUUID()
        val oldTimestamp = 1000L
        val newTimestamp = 2000L

        // Local has newer data
        val localEntity = createTestHabitEntity(id = habitId, updatedAt = newTimestamp)
        every { habitDao.getAll() } returns listOf(localEntity)
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null

        // Remote has older data (version < local updatedAt)
        val remoteData = createTestFirestoreHabitMap(habitId, version = oldTimestamp)

        val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { docSnapshot.id } returns habitId.toString()
        every { docSnapshot.data } returns remoteData

        val habitSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { habitSnapshot.documents } returns listOf(docSnapshot)

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.get() } returns Tasks.forResult(habitSnapshot)
        every { habitsCollection.document(habitId.toString()) } returns docRef
        every { docRef.set(any()) } returns Tasks.forResult(null)

        every { firestore.collection(neq(FirestoreCollections.habits(userId).value)) } returns
            mockk(relaxed = true) {
                every { get() } returns Tasks.forResult(emptySnapshot)
            }

        syncManager.performInitialSync(userId)

        // Local wins: should push to Firestore
        verify { habitsCollection.document(habitId.toString()) }
        verify { docRef.set(any()) }
        // Should NOT upsert into local DAO (local is already newer)
        coVerify(exactly = 0) { habitDao.upsert(any()) }
    }

    // ------------------------------------------------------------------
    // Conflict resolution: local-only entity pushes to Firestore
    // ------------------------------------------------------------------

    @Test
    fun `performInitialSync local only entity pushes to Firestore`() = runTest {
        val localOnlyId = UUID.randomUUID()
        val remoteOnlyId = UUID.randomUUID()

        // Local has an entity that remote does not
        val localEntity = createTestHabitEntity(id = localOnlyId, updatedAt = 1000L)
        every { habitDao.getAll() } returns listOf(localEntity)
        every { completionDao.getAll() } returns emptyList()
        every { routineDao.getAll() } returns emptyList()
        every { routineExecutionDao.getAll() } returns emptyList()
        every { recoverySessionDao.getAll() } returns emptyList()
        every { userPreferencesDao.get() } returns null

        // Remote has a different entity
        val remoteData = createTestFirestoreHabitMap(remoteOnlyId, version = 1000L)
        val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { docSnapshot.id } returns remoteOnlyId.toString()
        every { docSnapshot.data } returns remoteData

        val habitSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { habitSnapshot.documents } returns listOf(docSnapshot)

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        val localDocRef = mockk<DocumentReference>(relaxed = true)
        every {
            firestore.collection(FirestoreCollections.habits(userId).value)
        } returns habitsCollection
        every { habitsCollection.get() } returns Tasks.forResult(habitSnapshot)
        every { habitsCollection.document(localOnlyId.toString()) } returns localDocRef
        every { localDocRef.set(any()) } returns Tasks.forResult(null)

        every { firestore.collection(neq(FirestoreCollections.habits(userId).value)) } returns
            mockk(relaxed = true) {
                every { get() } returns Tasks.forResult(emptySnapshot)
            }

        // Remote-only entity should be upserted locally
        coEvery { habitDao.upsert(any()) } returns Unit

        syncManager.performInitialSync(userId)

        // Local-only entity should be pushed to Firestore
        verify { habitsCollection.document(localOnlyId.toString()) }
        verify { localDocRef.set(any()) }
        // Remote-only entity should be upserted into local DAO
        coVerify { habitDao.upsert(any()) }
    }

    // ------------------------------------------------------------------
    // startListening clears existing listeners before attaching new ones
    // ------------------------------------------------------------------

    @Test
    fun `startListening clears existing listeners before attaching new ones`() {
        val listenerReg1 = mockk<com.google.firebase.firestore.ListenerRegistration>(relaxed = true)
        val listenerReg2 = mockk<com.google.firebase.firestore.ListenerRegistration>(relaxed = true)

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.addSnapshotListener(any()) } returnsMany listOf(
            listenerReg1, listenerReg1, listenerReg1, listenerReg1, listenerReg1, listenerReg1, listenerReg1,
            listenerReg2, listenerReg2, listenerReg2, listenerReg2, listenerReg2, listenerReg2, listenerReg2,
        )

        // First call attaches listeners
        syncManager.startListening(userId)

        // Second call should remove existing listeners before attaching new ones
        syncManager.startListening(userId)

        verify { listenerReg1.remove() }
    }

    // ------------------------------------------------------------------
    // deleteAllUserData
    // ------------------------------------------------------------------

    @Test
    fun `deleteAllUserData happy path deletes all collections and user document`() = runTest {
        // Mock routines collection (empty -- no nested subcollections to clean)
        val routinesPath = FirestoreCollections.routines(userId).value
        val routinesCollection = mockk<CollectionReference>(relaxed = true)
        val emptyRoutinesSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptyRoutinesSnapshot.documents } returns emptyList()
        every { firestore.collection(routinesPath) } returns routinesCollection
        every { routinesCollection.get() } returns Tasks.forResult(emptyRoutinesSnapshot)

        // All other collections return empty snapshots on limit().get()
        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.isEmpty } returns true
        every { emptySnapshot.size() } returns 0

        val genericCollection = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(neq(routinesPath)) } returns genericCollection
        val limitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { genericCollection.limit(any()) } returns limitQuery
        every { limitQuery.get() } returns Tasks.forResult(emptySnapshot)

        // Also set up the routinesCollection.limit() for the deleteCollection call on routines
        val routinesLimitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { routinesCollection.limit(any()) } returns routinesLimitQuery
        every { routinesLimitQuery.get() } returns Tasks.forResult(emptySnapshot)

        // Mock user document deletion
        val userDocPath = FirestoreCollections.user(userId).value
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.document(userDocPath) } returns userDocRef
        every { userDocRef.delete() } returns Tasks.forResult(null)

        syncManager.deleteAllUserData(userId)

        // Verify user document was deleted
        verify { firestore.document(userDocPath) }
        verify { userDocRef.delete() }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteAllUserData throws IllegalArgumentException for blank userId`() = runTest {
        syncManager.deleteAllUserData("")
    }

    @Test
    fun `deleteAllUserData cleans up nested routine subcollections`() = runTest {
        val routineId = "routine-abc"
        val routinesPath = FirestoreCollections.routines(userId).value
        val habitsSubPath = FirestoreCollections.routineHabits(userId, routineId).value
        val variantsSubPath = FirestoreCollections.routineVariants(userId, routineId).value

        // Mock routine doc
        val routineDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { routineDocSnapshot.id } returns routineId

        val routinesSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { routinesSnapshot.documents } returns listOf(routineDocSnapshot)

        val routinesCollection = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(routinesPath) } returns routinesCollection
        every { routinesCollection.get() } returns Tasks.forResult(routinesSnapshot)

        // Empty snapshots for deleteCollection calls
        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.isEmpty } returns true
        every { emptySnapshot.size() } returns 0

        // For each collection path, mock limit().get() returning empty
        val setupEmptyCollection = { path: String ->
            val coll = mockk<CollectionReference>(relaxed = true)
            val query = mockk<com.google.firebase.firestore.Query>(relaxed = true)
            every { firestore.collection(path) } returns coll
            every { coll.limit(any()) } returns query
            every { query.get() } returns Tasks.forResult(emptySnapshot)
        }

        setupEmptyCollection(habitsSubPath)
        setupEmptyCollection(variantsSubPath)

        // Set up routines collection limit for deleteCollection
        val routinesLimitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { routinesCollection.limit(any()) } returns routinesLimitQuery
        every { routinesLimitQuery.get() } returns Tasks.forResult(emptySnapshot)

        // Generic collection for all other paths
        val genericCollection = mockk<CollectionReference>(relaxed = true)
        val genericQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every {
            firestore.collection(not(match { it == routinesPath || it == habitsSubPath || it == variantsSubPath }))
        } returns genericCollection
        every { genericCollection.limit(any()) } returns genericQuery
        every { genericQuery.get() } returns Tasks.forResult(emptySnapshot)

        // User doc delete
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.document(any()) } returns userDocRef
        every { userDocRef.delete() } returns Tasks.forResult(null)

        syncManager.deleteAllUserData(userId)

        // Verify that we tried to access the subcollection paths
        verify { firestore.collection(habitsSubPath) }
        verify { firestore.collection(variantsSubPath) }
    }

    @Test
    fun `deleteAllUserData handles empty collections gracefully`() = runTest {
        // All collections are empty
        val emptyRoutinesSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptyRoutinesSnapshot.documents } returns emptyList()

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.isEmpty } returns true
        every { emptySnapshot.size() } returns 0

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.get() } returns Tasks.forResult(emptyRoutinesSnapshot)
        val limitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { collectionRef.limit(any()) } returns limitQuery
        every { limitQuery.get() } returns Tasks.forResult(emptySnapshot)

        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.document(any()) } returns userDocRef
        every { userDocRef.delete() } returns Tasks.forResult(null)

        // Should complete without error
        syncManager.deleteAllUserData(userId)

        verify { userDocRef.delete() }
    }

    @Test(expected = RuntimeException::class)
    fun `deleteAllUserData propagates Firestore failure`() = runTest {
        // Routines collection fetch fails
        val routinesPath = FirestoreCollections.routines(userId).value
        val routinesCollection = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(routinesPath) } returns routinesCollection
        every { routinesCollection.get() } returns Tasks.forException(
            RuntimeException("Permission denied"),
        )

        syncManager.deleteAllUserData(userId)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `deleteAllUserData rethrows CancellationException`() = runTest {
        val routinesPath = FirestoreCollections.routines(userId).value
        val routinesCollection = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(routinesPath) } returns routinesCollection
        coEvery { routinesCollection.get() } throws
            kotlinx.coroutines.CancellationException("Job cancelled")

        syncManager.deleteAllUserData(userId)
    }

    @Test
    fun `deleteAllUserData handles batch pagination with more than 500 docs`() = runTest {
        // Mock routines collection (empty)
        val routinesPath = FirestoreCollections.routines(userId).value
        val routinesCollection = mockk<CollectionReference>(relaxed = true)
        val emptyRoutinesSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptyRoutinesSnapshot.documents } returns emptyList()
        every { firestore.collection(routinesPath) } returns routinesCollection
        every { routinesCollection.get() } returns Tasks.forResult(emptyRoutinesSnapshot)

        // Create a snapshot with documents for first batch, then empty for second
        val firstBatchDocs = (1..500).map { i ->
            mockk<DocumentSnapshot>(relaxed = true) {
                every { reference } returns mockk(relaxed = true)
            }
        }
        val firstBatchSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { firstBatchSnapshot.isEmpty } returns false
        every { firstBatchSnapshot.documents } returns firstBatchDocs
        every { firstBatchSnapshot.size() } returns 500

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.isEmpty } returns true
        every { emptySnapshot.size() } returns 0

        // habits collection: first call returns 500 docs, second returns empty
        val habitsPath = FirestoreCollections.habits(userId).value
        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        val habitsLimitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { firestore.collection(habitsPath) } returns habitsCollection
        every { habitsCollection.limit(any()) } returns habitsLimitQuery
        every { habitsLimitQuery.get() } returnsMany listOf(
            Tasks.forResult(firstBatchSnapshot),
            Tasks.forResult(emptySnapshot),
        )

        // Batch mock
        val batchMock = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batchMock
        every { batchMock.delete(any()) } returns batchMock
        every { batchMock.commit() } returns Tasks.forResult(null)

        // All other collections return empty
        val genericCollection = mockk<CollectionReference>(relaxed = true)
        val genericLimitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { firestore.collection(not(match { it == routinesPath || it == habitsPath })) } returns genericCollection
        every { genericCollection.limit(any()) } returns genericLimitQuery
        every { genericLimitQuery.get() } returns Tasks.forResult(emptySnapshot)
        every { routinesCollection.limit(any()) } returns mockk<com.google.firebase.firestore.Query>(relaxed = true) {
            every { get() } returns Tasks.forResult(emptySnapshot)
        }

        // User doc delete
        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.document(any()) } returns userDocRef
        every { userDocRef.delete() } returns Tasks.forResult(null)

        syncManager.deleteAllUserData(userId)

        // batch.commit() should have been called at least once for the 500-doc batch
        verify(atLeast = 1) { batchMock.commit() }
    }

    @Test
    fun `deleteAllUserData stops listeners before deletion`() = runTest {
        // First start listening so there are active listeners
        val listenerReg = mockk<com.google.firebase.firestore.ListenerRegistration>(relaxed = true)
        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.addSnapshotListener(any()) } returns listenerReg

        syncManager.startListening(userId)

        // Now set up for deleteAllUserData
        val emptyRoutinesSnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptyRoutinesSnapshot.documents } returns emptyList()
        every { collectionRef.get() } returns Tasks.forResult(emptyRoutinesSnapshot)

        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.isEmpty } returns true
        every { emptySnapshot.size() } returns 0

        val limitQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { collectionRef.limit(any()) } returns limitQuery
        every { limitQuery.get() } returns Tasks.forResult(emptySnapshot)

        val userDocRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.document(any()) } returns userDocRef
        every { userDocRef.delete() } returns Tasks.forResult(null)

        syncManager.deleteAllUserData(userId)

        // Listeners should have been removed (stopListening is called)
        verify(atLeast = 1) { listenerReg.remove() }

        // After stopListening, syncState should be NotSignedIn
        assert(syncManager.syncState.value is SyncState.NotSignedIn)
    }

    // ------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------

    private fun createTestHabitEntity(
        id: UUID = UUID.randomUUID(),
        updatedAt: Long = System.currentTimeMillis(),
    ): HabitEntity = HabitEntity(
        id = id,
        name = "Test Habit",
        anchorBehavior = "After waking up",
        anchorType = "AfterBehavior",
        category = "Morning",
        frequency = "Daily",
        estimatedSeconds = 300,
        phase = "FORMING",
        status = "Active",
        createdAt = System.currentTimeMillis(),
        updatedAt = updatedAt,
        lapseThresholdDays = 3,
        relapseThresholdDays = 7,
    )

    private fun createTestFirestoreHabitMap(id: UUID, version: Long = System.currentTimeMillis()): Map<String, Any?> {
        val updatedAtTimestamp = com.google.firebase.Timestamp(
            version / 1000,
            ((version % 1000) * 1_000_000).toInt(),
        )
        return mapOf(
            "id" to id.toString(),
            "name" to "Remote Habit",
            "description" to null,
            "icon" to null,
            "color" to null,
            "anchorBehavior" to "After lunch",
            "anchorType" to "AFTER_BEHAVIOR",
            "timeWindow" to null,
            "category" to "AFTERNOON",
            "frequency" to "DAILY",
            "activeDays" to null,
            "estimatedSeconds" to 300,
            "microVersion" to null,
            "allowPartial" to true,
            "subtasks" to emptyList<String>(),
            "lapseThresholdDays" to 3,
            "relapseThresholdDays" to 7,
            "phase" to "FORMING",
            "status" to "ACTIVE",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to updatedAtTimestamp,
            "pausedAt" to null,
            "archivedAt" to null,
            "version" to version,
        )
    }
}
