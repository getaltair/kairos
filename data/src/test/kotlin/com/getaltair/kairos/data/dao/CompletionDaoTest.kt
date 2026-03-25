package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.getaltair.kairos.data.converter.LocalDateConverter
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.data.entity.HabitEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [CompletionDao] operations.
 * Uses Room's in-memory database for isolated test execution.
 */
@RunWith(AndroidJUnit4::class.java)
class CompletionDaoTest {

    private lateinit var database: KairosDatabase
    private lateinit var completionDao: CompletionDao
    private lateinit var habitDao: HabitDao
    private lateinit var localDateConverter: LocalDateConverter

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(KairosDatabase::class.java).build()
        completionDao = database.completionDao()
        habitDao = database.habitDao()
        localDateConverter = LocalDateConverter()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== CRUD Operations ====================

    @Test
    fun insertAndGetById() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val completion = createTestCompletion(habit.id)
        completionDao.insert(completion)

        val result = completionDao.getById(completion.id)
        assertNotNull(result)
        assertEquals(completion.id, result!!.id)
    }

    @Test
    fun getAll_returnsAllCompletions() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val completion1 = createTestCompletion(habit.id)
        val completion2 = createTestCompletion(habit.id, daysOffset = 1)

        completionDao.insert(completion1)
        completionDao.insert(completion2)

        val results = completionDao.getAll()
        assertEquals(2, results.size)
    }

    @Test
    fun delete_removesCompletion() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val completion = createTestCompletion(habit.id)
        completionDao.insert(completion)

        completionDao.delete(completion.id)

        val result = completionDao.getById(completion.id)
        assertNull(result)
    }

    @Test
    fun deleteForHabit_removesAllCompletionsForHabit() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val completion1 = createTestCompletion(habit.id)
        val completion2 = createTestCompletion(habit.id, daysOffset = 1)

        completionDao.insert(completion1)
        completionDao.insert(completion2)

        completionDao.deleteForHabit(habit.id)

        val results = completionDao.getForHabit(habit.id)
        assertTrue(results.isEmpty())
    }

    // ==================== Specialized Queries ====================

    @Test
    fun getForHabitOnDate_returnsSpecificCompletion() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val targetDate = LocalDate.now()
        val completion1 = createTestCompletion(habit.id, targetDate)
        val completion2 = createTestCompletion(habit.id, targetDate.plusDays(1))

        completionDao.insert(completion1)
        completionDao.insert(completion2)

        val result = completionDao.getForHabitOnDate(habit.id, targetDate.toString())
        assertNotNull(result)
        assertEquals(completion1.id, result!!.id)
    }

    @Test
    fun getForDate_returnsCompletionsForDay() {
        val habit1 = createTestHabit()
        val habit2 = createTestHabit()

        habitDao.insert(habit1)
        habitDao.insert(habit2)

        val targetDate = LocalDate.now()
        val completion1 = createTestCompletion(habit1.id, targetDate)
        val completion2 = createTestCompletion(habit2.id, targetDate)

        completionDao.insert(completion1)
        completionDao.insert(completion2)

        val results = completionDao.getForDate(targetDate.toString())
        assertEquals(2, results.size)
    }

    @Test
    fun getForDateRange_returnsCompletionsInRange() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        val completion1 = createTestCompletion(habit.id, startDate)
        val completion2 = createTestCompletion(habit.id, startDate.plusDays(1))
        val completion3 = createTestCompletion(habit.id, startDate.plusDays(7))

        completionDao.insert(completion1)
        completionDao.insert(completion2)
        completionDao.insert(completion3)

        val results = completionDao.getForDateRange(startDate.toString(), endDate.toString())
        assertEquals(2, results.size)
    }

    @Test
    fun getForHabitInRange_returnsCompletionsForHabitInRange() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        val completion1 = createTestCompletion(habit.id, startDate)
        val completion2 = createTestCompletion(habit.id, startDate.plusDays(3))
        val completion3 = createTestCompletion(habit.id, endDate)

        completionDao.insert(completion1)
        completionDao.insert(completion2)
        completionDao.insert(completion3)

        val results = completionDao.getForHabitInRange(habit.id, startDate.toString(), endDate.toString())
        assertEquals(2, results.size)
    }

    // ==================== UNIQUE Constraint ====================

    @Test
    fun insert_replacesExistingCompletion() {
        val habit = createTestHabit()
        habitDao.insert(habit)

        val targetDate = LocalDate.now()
        val completion1 = createTestCompletion(habit.id, targetDate, type = "Full")
        val completion2 = createTestCompletion(habit.id, targetDate, type = "Skipped")

        completionDao.insert(completion1)
        // Insert another completion for same habit/date - should REPLACE
        completionDao.insert(completion2)

        val results = completionDao.getForDate(targetDate.toString())
        assertEquals(1, results.size)
        assertEquals("Skipped", results[0].type)
    }

    // ==================== Helper Methods ====================

    private fun createTestHabit(): HabitEntity = HabitEntity(
        id = UUID.randomUUID(),
        name = "Test Habit",
        description = null,
        icon = null,
        color = null,
        anchorBehavior = "After test",
        anchorType = "AfterBehavior",
        timeWindowStart = null,
        timeWindowEnd = null,
        category = "Morning",
        frequency = "Daily",
        activeDays = null,
        estimatedSeconds = 300,
        microVersion = null,
        allowPartialCompletion = true,
        subtasks = null,
        phase = "ONBOARD",
        status = "Active",
        userId = null,
        createdAt = Instant.now().toEpochMilli(),
        updatedAt = Instant.now().toEpochMilli(),
        pausedAt = null,
        archivedAt = null,
        lapseThresholdDays = 3,
        relapseThresholdDays = 7
    )

    private fun createTestCompletion(
        habitId: UUID,
        date: LocalDate = LocalDate.now(),
        daysOffset: Int = 0,
        type: String = "Full"
    ): CompletionEntity = CompletionEntity(
        id = UUID.randomUUID(),
        habitId = habitId,
        date = date.plusDays(daysOffset).toString(),
        completedAt = Instant.now().toEpochMilli(),
        type = type,
        partialPercent = null,
        skipReason = null,
        energyLevel = null,
        note = null,
        createdAt = Instant.now().toEpochMilli(),
        updatedAt = Instant.now().toEpochMilli()
    )
}
