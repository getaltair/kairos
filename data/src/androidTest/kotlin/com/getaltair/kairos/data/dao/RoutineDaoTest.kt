package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.data.entity.RoutineHabitEntity
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
class RoutineDaoTest {

    private lateinit var db: KairosDatabase
    private lateinit var routineDao: RoutineDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, KairosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        routineDao = db.routineDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() {
        val routine = createRoutine(name = "Morning Routine")
        routineDao.insert(routine)

        val result = routineDao.getById(routine.id)
        assertNotNull(result)
        assertEquals("Morning Routine", result.name)
        assertEquals(routine.id, result.id)
    }

    @Test
    fun getByIdReturnsNullForMissingId() {
        val result = routineDao.getById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun getAllReturnsInsertedRoutines() {
        routineDao.insertAll(
            listOf(
                createRoutine(name = "Morning"),
                createRoutine(name = "Evening"),
            )
        )

        val results = routineDao.getAll()
        assertEquals(2, results.size)
    }

    @Test
    fun getActiveRoutinesFiltersCorrectly() {
        val active = createRoutine(name = "Active Routine", status = RoutineStatus.Active)
        val paused = createRoutine(name = "Paused Routine", status = RoutineStatus.Paused)
        val archived = createRoutine(name = "Archived Routine", status = RoutineStatus.Archived)
        routineDao.insertAll(listOf(active, paused, archived))

        val results = routineDao.getActiveRoutines()
        assertEquals(1, results.size)
        assertEquals("Active Routine", results[0].name)
    }

    @Test
    fun getByStatusReturnsMatchingRoutines() {
        routineDao.insertAll(
            listOf(
                createRoutine(name = "Active", status = RoutineStatus.Active),
                createRoutine(name = "Paused", status = RoutineStatus.Paused),
                createRoutine(name = "Archived", status = RoutineStatus.Archived),
            )
        )

        val paused = routineDao.getByStatus(RoutineStatus.Paused)
        assertEquals(1, paused.size)
        assertEquals("Paused", paused[0].name)
    }

    @Test
    fun updateChangesFields() {
        val routine = createRoutine(name = "Stretch Routine")
        routineDao.insert(routine)

        val now = Instant.now().toEpochMilli()
        routineDao.update(
            id = routine.id,
            name = "Full Stretch Routine",
            description = "Complete body stretch",
            icon = null,
            color = "#00FF00",
            category = "Evening",
            status = RoutineStatus.Active,
            userId = null,
            updatedAt = now,
        )

        val updated = routineDao.getById(routine.id)
        assertNotNull(updated)
        assertEquals("Full Stretch Routine", updated.name)
        assertEquals("Complete body stretch", updated.description)
        assertEquals("#00FF00", updated.color)
    }

    @Test
    fun deleteRemovesRoutine() {
        val routine = createRoutine(name = "To Delete")
        routineDao.insert(routine)

        routineDao.delete(routine.id)
        assertNull(routineDao.getById(routine.id))
    }

    @Test
    fun deleteAllClearsTable() {
        routineDao.insertAll(
            listOf(
                createRoutine(name = "A"),
                createRoutine(name = "B"),
            )
        )
        assertEquals(2, routineDao.getAll().size)

        routineDao.deleteAll()
        assertTrue(routineDao.getAll().isEmpty())
    }

    @Test
    fun upsertInsertsNewAndUpdatesExisting() = runTest {
        val routine = createRoutine(name = "Upsert Test")
        routineDao.upsert(routine)

        val inserted = routineDao.getById(routine.id)
        assertNotNull(inserted)
        assertEquals("Upsert Test", inserted.name)

        routineDao.upsert(routine.copy(name = "Upserted Name"))
        val updated = routineDao.getById(routine.id)
        assertNotNull(updated)
        assertEquals("Upserted Name", updated.name)
    }

    @Test
    fun insertWithHabitsIsAtomic() {
        val routine = createRoutine(name = "With Habits")
        val now = Instant.now().toEpochMilli()

        // Insert a habit in the habits table first (RoutineHabitEntity references habits)
        val habitDao = db.habitDao()
        val habitEntity = com.getaltair.kairos.data.entity.HabitEntity(
            id = UUID.randomUUID(),
            name = "Linked Habit",
            anchorBehavior = "After waking up",
            anchorType = "AfterBehavior",
            category = "Morning",
            frequency = "Daily",
            phase = "Initiation",
            status = "Active",
            createdAt = now,
            updatedAt = now,
        )
        habitDao.insert(habitEntity)

        val routineHabits = listOf(
            RoutineHabitEntity(
                id = UUID.randomUUID(),
                routineId = routine.id,
                habitId = habitEntity.id,
                orderIndex = 0,
                overrideDurationSeconds = null,
                createdAt = now,
                updatedAt = now,
            )
        )

        routineDao.insertWithHabits(routine, routineHabits)

        val savedRoutine = routineDao.getById(routine.id)
        assertNotNull(savedRoutine)
        assertEquals("With Habits", savedRoutine.name)

        val savedHabits = db.routineHabitDao().getByRoutineId(routine.id)
        assertEquals(1, savedHabits.size)
        assertEquals(habitEntity.id, savedHabits[0].habitId)
    }

    @Test
    fun getAllFlowEmitsOnChange() = runTest {
        val initial = routineDao.getAllFlow().first()
        assertTrue(initial.isEmpty())

        routineDao.insert(createRoutine(name = "Flow Test"))

        val afterInsert = routineDao.getAllFlow().first()
        assertEquals(1, afterInsert.size)
        assertEquals("Flow Test", afterInsert[0].name)
    }

    @Test
    fun getByUserIdFiltersCorrectly() {
        routineDao.insertAll(
            listOf(
                createRoutine(name = "User A Routine", userId = "user-a"),
                createRoutine(name = "User B Routine", userId = "user-b"),
            )
        )

        val results = routineDao.getByUserId("user-a")
        assertEquals(1, results.size)
        assertEquals("User A Routine", results[0].name)
    }

    private fun createRoutine(
        name: String,
        status: RoutineStatus = RoutineStatus.Active,
        userId: String? = null,
    ): RoutineEntity {
        val now = Instant.now().toEpochMilli()
        return RoutineEntity(
            id = UUID.randomUUID(),
            name = name,
            description = null,
            icon = null,
            color = null,
            category = HabitCategory.Morning,
            status = status,
            userId = userId,
            createdAt = now,
            updatedAt = now,
        )
    }
}
