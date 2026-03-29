package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.data.entity.RoutineExecutionEntity
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineExecutionDaoTest {

    private lateinit var db: KairosDatabase
    private lateinit var executionDao: RoutineExecutionDao
    private lateinit var routineDao: RoutineDao

    private lateinit var testRoutineId: UUID

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, KairosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        executionDao = db.routineExecutionDao()
        routineDao = db.routineDao()

        // Insert a parent routine (required by foreign key)
        val routine = createRoutine("Test Routine")
        routineDao.insert(routine)
        testRoutineId = routine.id
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.insert(execution)

        val result = executionDao.getById(execution.id)
        assertNotNull(result)
        assertEquals(testRoutineId, result.routineId)
        assertEquals(ExecutionStatus.InProgress, result.status)
    }

    @Test
    fun getByIdReturnsNullForMissingId() {
        val result = executionDao.getById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun getByRoutineIdReturnsExecutionsForRoutine() {
        val routine2 = createRoutine("Routine Two")
        routineDao.insert(routine2)

        executionDao.insert(createExecution(routineId = testRoutineId))
        executionDao.insert(createExecution(routineId = testRoutineId))
        executionDao.insert(createExecution(routineId = routine2.id))

        val results = executionDao.getByRoutineId(testRoutineId)
        assertEquals(2, results.size)
        assertTrue(results.all { it.routineId == testRoutineId })
    }

    @Test
    fun getActiveForRoutineReturnsInProgressExecution() {
        val inProgress = createExecution(
            routineId = testRoutineId,
            status = ExecutionStatus.InProgress,
        )
        val completed = createExecution(
            routineId = testRoutineId,
            status = ExecutionStatus.Completed,
        )
        executionDao.insertAll(listOf(inProgress, completed))

        val result = executionDao.getActiveForRoutine(testRoutineId)
        assertNotNull(result)
        assertEquals(ExecutionStatus.InProgress, result.status)
    }

    @Test
    fun getActiveForRoutineReturnsNullWhenNoneActive() {
        val completed = createExecution(
            routineId = testRoutineId,
            status = ExecutionStatus.Completed,
        )
        executionDao.insert(completed)

        val result = executionDao.getActiveForRoutine(testRoutineId)
        assertNull(result)
    }

    @Test
    fun getActiveExecutionsReturnsAllInProgress() {
        val routine2 = createRoutine("Routine Two")
        routineDao.insert(routine2)

        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.InProgress)
        )
        executionDao.insert(
            createExecution(routineId = routine2.id, status = ExecutionStatus.InProgress)
        )
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.Completed)
        )

        val results = executionDao.getActiveExecutions()
        assertEquals(2, results.size)
        assertTrue(results.all { it.status == ExecutionStatus.InProgress })
    }

    @Test
    fun getByStatusFiltersCorrectly() {
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.InProgress)
        )
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.Completed)
        )
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.Abandoned)
        )

        val completed = executionDao.getByStatus(ExecutionStatus.Completed)
        assertEquals(1, completed.size)

        val abandoned = executionDao.getByStatus(ExecutionStatus.Abandoned)
        assertEquals(1, abandoned.size)
    }

    @Test
    fun getCompletedForRoutineReturnsOnlyCompleted() {
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.Completed)
        )
        executionDao.insert(
            createExecution(routineId = testRoutineId, status = ExecutionStatus.InProgress)
        )

        val results = executionDao.getCompletedForRoutine(testRoutineId)
        assertEquals(1, results.size)
        assertEquals(ExecutionStatus.Completed, results[0].status)
    }

    @Test
    fun updateChangesFields() {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.insert(execution)

        val now = Instant.now().toEpochMilli()
        executionDao.update(
            id = execution.id,
            completedAt = now,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
            currentStepRemainingSeconds = null,
            totalPausedSeconds = 120,
            updatedAt = now,
        )

        val updated = executionDao.getById(execution.id)
        assertNotNull(updated)
        assertEquals(ExecutionStatus.Completed, updated.status)
        assertEquals(3, updated.currentStepIndex)
        assertEquals(120, updated.totalPausedSeconds)
        assertNotNull(updated.completedAt)
    }

    @Test
    fun updateStatusChangesStatusOnly() {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.insert(execution)

        val now = Instant.now().toEpochMilli()
        executionDao.updateStatus(
            id = execution.id,
            status = ExecutionStatus.Paused,
            completedAt = null,
            updatedAt = now,
        )

        val updated = executionDao.getById(execution.id)
        assertNotNull(updated)
        assertEquals(ExecutionStatus.Paused, updated.status)
        assertNull(updated.completedAt)
    }

    @Test
    fun updateStatusSetsCompletedAtWhenCompleted() {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.insert(execution)

        val now = Instant.now().toEpochMilli()
        executionDao.updateStatus(
            id = execution.id,
            status = ExecutionStatus.Completed,
            completedAt = now,
            updatedAt = now,
        )

        val updated = executionDao.getById(execution.id)
        assertNotNull(updated)
        assertEquals(ExecutionStatus.Completed, updated.status)
        assertEquals(now, updated.completedAt)
    }

    @Test
    fun deleteRemovesExecution() {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.insert(execution)

        executionDao.delete(execution.id)
        assertNull(executionDao.getById(execution.id))
    }

    @Test
    fun deleteByRoutineIdRemovesAllExecutionsForRoutine() {
        executionDao.insert(createExecution(routineId = testRoutineId))
        executionDao.insert(createExecution(routineId = testRoutineId))
        assertEquals(2, executionDao.getByRoutineId(testRoutineId).size)

        executionDao.deleteByRoutineId(testRoutineId)
        assertTrue(executionDao.getByRoutineId(testRoutineId).isEmpty())
    }

    @Test
    fun deleteAllClearsTable() {
        executionDao.insert(createExecution(routineId = testRoutineId))
        executionDao.insert(createExecution(routineId = testRoutineId))

        executionDao.deleteAll()
        assertTrue(executionDao.getAll().isEmpty())
    }

    @Test
    fun cascadeDeleteRemovesExecutionsWhenRoutineDeleted() {
        executionDao.insert(createExecution(routineId = testRoutineId))
        assertEquals(1, executionDao.getByRoutineId(testRoutineId).size)

        routineDao.delete(testRoutineId)
        assertTrue(executionDao.getByRoutineId(testRoutineId).isEmpty())
    }

    @Test
    fun upsertInsertsNewAndUpdatesExisting() = runTest {
        val execution = createExecution(routineId = testRoutineId)
        executionDao.upsert(execution)

        val inserted = executionDao.getById(execution.id)
        assertNotNull(inserted)
        assertEquals(ExecutionStatus.InProgress, inserted.status)

        executionDao.upsert(
            execution.copy(
                status = ExecutionStatus.Completed,
                completedAt = Instant.now().toEpochMilli(),
            )
        )
        val updated = executionDao.getById(execution.id)
        assertNotNull(updated)
        assertEquals(ExecutionStatus.Completed, updated.status)
    }

    @Test
    fun getAllFlowEmitsOnChange() = runTest {
        val initial = executionDao.getAllFlow().first()
        assertTrue(initial.isEmpty())

        executionDao.insert(createExecution(routineId = testRoutineId))

        val afterInsert = executionDao.getAllFlow().first()
        assertEquals(1, afterInsert.size)
    }

    private fun createRoutine(name: String): RoutineEntity {
        val now = Instant.now().toEpochMilli()
        return RoutineEntity(
            id = UUID.randomUUID(),
            name = name,
            category = HabitCategory.Morning,
            status = RoutineStatus.Active,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createExecution(
        routineId: UUID,
        status: ExecutionStatus = ExecutionStatus.InProgress,
    ): RoutineExecutionEntity {
        val now = Instant.now().toEpochMilli()
        return RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = routineId,
            variantId = null,
            startedAt = now,
            completedAt = if (status == ExecutionStatus.Completed) now else null,
            status = status,
            currentStepIndex = 0,
            currentStepRemainingSeconds = if (status == ExecutionStatus.InProgress) 60 else null,
            totalPausedSeconds = 0,
            createdAt = now,
            updatedAt = now,
        )
    }
}
