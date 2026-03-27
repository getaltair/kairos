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
    // Test helpers
    // ------------------------------------------------------------------

    private fun createTestHabitEntity(): HabitEntity = HabitEntity(
        id = UUID.randomUUID(),
        name = "Test Habit",
        anchorBehavior = "After waking up",
        anchorType = "AfterBehavior",
        category = "Morning",
        frequency = "Daily",
        estimatedSeconds = 300,
        phase = "FORMING",
        status = "Active",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lapseThresholdDays = 3,
        relapseThresholdDays = 7,
    )

    private fun createTestFirestoreHabitMap(id: UUID): Map<String, Any?> = mapOf(
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
        "updatedAt" to com.google.firebase.Timestamp.now(),
        "pausedAt" to null,
        "archivedAt" to null,
        "version" to System.currentTimeMillis(),
    )
}
