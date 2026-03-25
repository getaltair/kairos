package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.room.Transaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.getaltair.kairos.data.converter.LocalDateConverter
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [HabitDao] operations.
 * Uses Room's in-memory database for isolated test execution.
 */
@RunWith(AndroidJUnit4::class.java)
class HabitDaoTest {

    private lateinit var database: KairosDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: CompletionDao
    private lateinit var localDateConverter: LocalDateConverter

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(KairosDatabase::class.java).build()
        habitDao = database.habitDao()
        completionDao = database.completionDao()
        localDateConverter = LocalDateConverter()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== CRUD Operations ====================

    @Test
    fun insertAndGetById() {
        val habit = createTestHabitEntity()
        habitDao.insert(habit)

        val result = habitDao.getById(habit.id)
        assertNotNull(result)
        assertEquals(habit.id, result!!.id)
        assertEquals("Test Habit", result!!.name)
    }

    @Test
    fun getAll_returnsAllHabits() {
        val habit1 = createTestHabitEntity("Habit 1")
        val habit2 = createTestHabitEntity("Habit 2")
        val habit3 = createTestHabitEntity("Habit 3")
        habitDao.insert(habit1)
        habitDao.insert(habit2)
        habitDao.insert(habit3)

        val results = habitDao.getAll()
        assertEquals(3, results.size)
    }

    @Test
    fun delete_removesHabit() {
        val habit = createTestHabitEntity()
        habitDao.insert(habit)

        habitDao.delete(habit.id)

        val result = habitDao.getById(habit.id)
        assertNull(result)
    }

    @Test
    fun deleteAll_removesAllHabits() {
        val habit1 = createTestHabitEntity("Habit 1")
        val habit2 = createTestHabitEntity("Habit 2")
        habitDao.insert(habit1)
        habitDao.insert(habit2)

        habitDao.deleteAll()

        val results = habitDao.getAll()
        assertTrue(results.isEmpty())
    }

    // ==================== Specialized Queries ====================

    @Test
    fun getActiveHabits_onlyReturnsActiveHabits() {
        val activeHabit = createTestHabitEntity(status = "Active")
        val pausedHabit = createTestHabitEntity(name = "Paused Habit", status = "Paused")
        val archivedHabit = createTestHabitEntity(name = "Archived Habit", pausedAt = Instant.now().toEpochMilli())

        habitDao.insert(activeHabit)
        habitDao.insert(pausedHabit)
        habitDao.insert(archivedHabit)

        val results = habitDao.getActiveHabits()
        assertEquals(1, results.size)
        assertEquals("Active Habit", results[0].name)
    }

    @Test
    fun getByCategory_filtersByCategory() {
        val morningHabit = createTestHabitEntity(name = "Morning Habit", category = "Morning")
        val eveningHabit = createTestHabitEntity(name = "Evening Habit", category = "Evening")

        habitDao.insert(morningHabit)
        habitDao.insert(eveningHabit)

        val morningResults = habitDao.getByCategory(HabitCategory.Morning)
        val eveningResults = habitDao.getByCategory(HabitCategory.Evening)

        assertEquals(1, morningResults.size)
        assertEquals(1, eveningResults.size)
        assertEquals("Morning Habit", morningResults[0].name)
        assertEquals("Evening Habit", eveningResults[0].name)
    }

    @Test
    fun getHabitsByPhase_filtersByPhase() {
        val formingHabit = createTestHabitEntity(name = "Forming Habit", phase = "FORMING")
        val maintainingHabit = createTestHabitEntity(name = "Maintaining Habit", phase = "MAINTAINING")

        habitDao.insert(formingHabit)
        habitDao.insert(maintainingHabit)

        val formingResults = habitDao.getHabitsByPhase(HabitPhase.FORMING)
        val maintainingResults = habitDao.getHabitsByPhase(HabitPhase.MAINTAINING)

        assertEquals(1, formingResults.size)
        assertEquals(1, maintainingResults.size)
        assertEquals("Forming Habit", formingResults[0].name)
        assertEquals("Maintaining Habit", maintainingResults[0].name)
    }

    // ==================== Helper Methods ====================

    private fun createTestHabitEntity(
        name: String = "Test Habit",
        status: String = "Active",
        category: String = "Morning",
        phase: String = "ONBOARD"
    ): HabitEntity = HabitEntity(
        id = UUID.randomUUID(),
        name = name,
        description = null,
        icon = null,
        color = null,
        anchorBehavior = "After breakfast",
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
        phase = phase,
        status = status,
        userId = null,
        createdAt = Instant.now().toEpochMilli(),
        updatedAt = Instant.now().toEpochMilli(),
        pausedAt = null,
        archivedAt = null,
        lapseThresholdDays = 3,
        relapseThresholdDays = 7
    )
}
