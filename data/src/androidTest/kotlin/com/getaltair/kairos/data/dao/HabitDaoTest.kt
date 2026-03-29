package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitStatus
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
class HabitDaoTest {

    private lateinit var db: KairosDatabase
    private lateinit var habitDao: HabitDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, KairosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = db.habitDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() {
        val habit = createHabit(name = "Meditate")
        habitDao.insert(habit)

        val result = habitDao.getById(habit.id)
        assertNotNull(result)
        assertEquals("Meditate", result.name)
        assertEquals(habit.id, result.id)
    }

    @Test
    fun getByIdReturnsNullForMissingId() {
        val result = habitDao.getById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun getAllReturnsInsertedHabits() {
        val habit1 = createHabit(name = "Exercise")
        val habit2 = createHabit(name = "Read")
        habitDao.insertAll(listOf(habit1, habit2))

        val results = habitDao.getAll()
        assertEquals(2, results.size)
    }

    @Test
    fun deleteRemovesHabit() {
        val habit = createHabit(name = "Journal")
        habitDao.insert(habit)

        habitDao.delete(habit.id)

        val result = habitDao.getById(habit.id)
        assertNull(result)
    }

    @Test
    fun deleteAllClearsTable() {
        habitDao.insertAll(
            listOf(
                createHabit(name = "Habit A"),
                createHabit(name = "Habit B"),
            )
        )
        assertEquals(2, habitDao.getAll().size)

        habitDao.deleteAll()
        assertTrue(habitDao.getAll().isEmpty())
    }

    @Test
    fun updateChangesFields() {
        val now = Instant.now().toEpochMilli()
        val habit = createHabit(name = "Stretch")
        habitDao.insert(habit)

        habitDao.update(
            id = habit.id,
            name = "Morning Stretch",
            description = "Full body stretch",
            icon = null,
            color = "#FF0000",
            anchorBehavior = "After waking up",
            anchorType = "AfterBehavior",
            timeWindowStart = null,
            timeWindowEnd = null,
            category = HabitCategory.Morning,
            frequency = "Daily",
            activeDays = null,
            estimatedSeconds = 600,
            microVersion = null,
            allowPartialCompletion = true,
            subtasks = null,
            phase = "Initiation",
            status = HabitStatus.Active,
            pausedAt = null,
            archivedAt = null,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7,
            updatedAt = now,
        )

        val updated = habitDao.getById(habit.id)
        assertNotNull(updated)
        assertEquals("Morning Stretch", updated.name)
        assertEquals("Full body stretch", updated.description)
        assertEquals("#FF0000", updated.color)
        assertEquals(600, updated.estimatedSeconds)
    }

    @Test
    fun getActiveHabitsFiltersCorrectly() {
        val active = createHabit(name = "Active Habit", status = "Active")
        val paused = createHabit(name = "Paused Habit", status = "Active", pausedAt = Instant.now().toEpochMilli())
        val archived = createHabit(
            name = "Archived Habit",
            status = "Active",
            archivedAt = Instant.now().toEpochMilli(),
        )
        val inactive = createHabit(name = "Inactive Habit", status = "Paused")
        habitDao.insertAll(listOf(active, paused, archived, inactive))

        val results = habitDao.getActiveHabits()
        assertEquals(1, results.size)
        assertEquals("Active Habit", results[0].name)
    }

    @Test
    fun getByStatusReturnsMatchingHabits() {
        val active = createHabit(name = "Running", status = "Active")
        val paused = createHabit(name = "Yoga", status = "Paused")
        habitDao.insertAll(listOf(active, paused))

        val activeResults = habitDao.getByStatus("Active")
        assertEquals(1, activeResults.size)
        assertEquals("Running", activeResults[0].name)

        val pausedResults = habitDao.getByStatus("Paused")
        assertEquals(1, pausedResults.size)
        assertEquals("Yoga", pausedResults[0].name)
    }

    @Test
    fun getActiveHabitsFlowEmitsOnInsert() = runTest {
        val initial = habitDao.getActiveHabitsFlow().first()
        assertTrue(initial.isEmpty())

        val habit = createHabit(name = "Breathwork", status = "Active")
        habitDao.insert(habit)

        val afterInsert = habitDao.getActiveHabitsFlow().first()
        assertEquals(1, afterInsert.size)
        assertEquals("Breathwork", afterInsert[0].name)
    }

    @Test
    fun upsertInsertsNewAndUpdatesExisting() = runTest {
        val habit = createHabit(name = "Cold Shower")
        habitDao.upsert(habit)

        val inserted = habitDao.getById(habit.id)
        assertNotNull(inserted)
        assertEquals("Cold Shower", inserted.name)

        habitDao.upsert(habit.copy(name = "Ice Bath"))
        val updated = habitDao.getById(habit.id)
        assertNotNull(updated)
        assertEquals("Ice Bath", updated.name)
    }

    @Test
    fun getByCategoryReturnsMatchingHabits() {
        val morning = createHabit(name = "Wake Up", category = "Morning")
        val evening = createHabit(name = "Wind Down", category = "Evening")
        habitDao.insertAll(listOf(morning, evening))

        val results = habitDao.getByCategory(HabitCategory.Morning)
        assertEquals(1, results.size)
        assertEquals("Wake Up", results[0].name)
    }

    private fun createHabit(
        name: String,
        status: String = "Active",
        category: String = "Morning",
        pausedAt: Long? = null,
        archivedAt: Long? = null,
    ): HabitEntity {
        val now = Instant.now().toEpochMilli()
        return HabitEntity(
            id = UUID.randomUUID(),
            name = name,
            description = null,
            icon = null,
            color = null,
            anchorBehavior = "After waking up",
            anchorType = "AfterBehavior",
            timeWindowStart = null,
            timeWindowEnd = null,
            category = category,
            frequency = "Daily",
            activeDays = null,
            estimatedSeconds = 300,
            microVersion = null,
            allowPartialCompletion = true,
            subtasks = null,
            phase = "Initiation",
            status = status,
            userId = null,
            createdAt = now,
            updatedAt = now,
            pausedAt = pausedAt,
            archivedAt = archivedAt,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7,
        )
    }
}
