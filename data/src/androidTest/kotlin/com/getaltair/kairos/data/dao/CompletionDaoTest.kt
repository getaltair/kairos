package com.getaltair.kairos.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.data.entity.HabitEntity
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
class CompletionDaoTest {

    private lateinit var db: KairosDatabase
    private lateinit var completionDao: CompletionDao
    private lateinit var habitDao: HabitDao

    private lateinit var testHabitId: UUID

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, KairosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        completionDao = db.completionDao()
        habitDao = db.habitDao()

        // Insert a parent habit (required by foreign key)
        val habit = createHabit("Test Habit")
        habitDao.insert(habit)
        testHabitId = habit.id
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() {
        val completion = createCompletion(habitId = testHabitId, date = "2026-03-28")
        completionDao.insert(completion)

        val result = completionDao.getById(completion.id)
        assertNotNull(result)
        assertEquals(testHabitId, result.habitId)
        assertEquals("2026-03-28", result.date)
        assertEquals("Full", result.type)
    }

    @Test
    fun getByIdReturnsNullForMissingId() {
        val result = completionDao.getById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun getForHabitOnDateReturnsCorrectCompletion() {
        val completion = createCompletion(habitId = testHabitId, date = "2026-03-28")
        completionDao.insert(completion)

        val result = completionDao.getForHabitOnDate(testHabitId, "2026-03-28")
        assertNotNull(result)
        assertEquals(completion.id, result.id)

        val noResult = completionDao.getForHabitOnDate(testHabitId, "2026-03-27")
        assertNull(noResult)
    }

    @Test
    fun getForDateReturnsAllCompletionsOnDate() {
        val habit2 = createHabit("Habit Two")
        habitDao.insert(habit2)

        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-28"))
        completionDao.insert(createCompletion(habitId = habit2.id, date = "2026-03-28"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-27"))

        val results = completionDao.getForDate("2026-03-28")
        assertEquals(2, results.size)
    }

    @Test
    fun getForDateRangeReturnsCompletionsInRange() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-26"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-28"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-30"))

        val results = completionDao.getForDateRange("2026-03-25", "2026-03-28")
        assertEquals(3, results.size)
    }

    @Test
    fun getForHabitInRangeFiltersCorrectly() {
        val habit2 = createHabit("Habit Two")
        habitDao.insert(habit2)

        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-27"))
        completionDao.insert(createCompletion(habitId = habit2.id, date = "2026-03-26"))

        val results = completionDao.getForHabitInRange(testHabitId, "2026-03-24", "2026-03-28")
        assertEquals(2, results.size)
        assertTrue(results.all { it.habitId == testHabitId })
    }

    @Test
    fun getForHabitReturnsAllCompletions() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-26"))

        val results = completionDao.getForHabit(testHabitId)
        assertEquals(2, results.size)
    }

    @Test
    fun deleteRemovesCompletion() {
        val completion = createCompletion(habitId = testHabitId, date = "2026-03-28")
        completionDao.insert(completion)

        completionDao.delete(completion.id)
        assertNull(completionDao.getById(completion.id))
    }

    @Test
    fun deleteForHabitRemovesAllCompletionsForHabit() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-26"))
        assertEquals(2, completionDao.getForHabit(testHabitId).size)

        completionDao.deleteForHabit(testHabitId)
        assertTrue(completionDao.getForHabit(testHabitId).isEmpty())
    }

    @Test
    fun deleteForDateRangeClearsRange() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-26"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-28"))

        completionDao.deleteForDateRange("2026-03-25", "2026-03-26")
        val remaining = completionDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("2026-03-28", remaining[0].date)
    }

    @Test
    fun deleteAllClearsTable() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-25"))
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-26"))

        completionDao.deleteAll()
        assertTrue(completionDao.getAll().isEmpty())
    }

    @Test
    fun cascadeDeleteRemovesCompletionsWhenHabitDeleted() {
        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-28"))
        assertEquals(1, completionDao.getForHabit(testHabitId).size)

        habitDao.delete(testHabitId)

        assertTrue(completionDao.getForHabit(testHabitId).isEmpty())
    }

    @Test
    fun updateChangesFields() {
        val completion = createCompletion(habitId = testHabitId, date = "2026-03-28")
        completionDao.insert(completion)

        val newTime = Instant.now().toEpochMilli()
        completionDao.update(
            id = completion.id,
            completedAt = newTime,
            type = "Partial",
            partialPercent = 50,
            skipReason = null,
            energyLevel = 4,
            note = "Halfway there",
            updatedAt = newTime,
        )

        val updated = completionDao.getById(completion.id)
        assertNotNull(updated)
        assertEquals("Partial", updated.type)
        assertEquals(50, updated.partialPercent)
        assertEquals(4, updated.energyLevel)
        assertEquals("Halfway there", updated.note)
    }

    @Test
    fun upsertInsertsNewAndUpdatesExisting() = runTest {
        val completion = createCompletion(habitId = testHabitId, date = "2026-03-28")
        completionDao.upsert(completion)

        val inserted = completionDao.getById(completion.id)
        assertNotNull(inserted)
        assertEquals("Full", inserted.type)

        val updatedTime = Instant.now().toEpochMilli()
        completionDao.upsert(
            completion.copy(
                type = "Partial",
                partialPercent = 75,
                updatedAt = updatedTime,
            )
        )
        val updated = completionDao.getById(completion.id)
        assertNotNull(updated)
        assertEquals("Partial", updated.type)
        assertEquals(75, updated.partialPercent)
    }

    @Test
    fun getAllFlowEmitsOnChange() = runTest {
        val initial = completionDao.getAllFlow().first()
        assertTrue(initial.isEmpty())

        completionDao.insert(createCompletion(habitId = testHabitId, date = "2026-03-28"))

        val afterInsert = completionDao.getAllFlow().first()
        assertEquals(1, afterInsert.size)
    }

    @Test
    fun insertWithReplaceStrategyUpdatesExisting() {
        val id = UUID.randomUUID()
        val now = Instant.now().toEpochMilli()
        val original = CompletionEntity(
            id = id,
            habitId = testHabitId,
            date = "2026-03-28",
            completedAt = now,
            type = "Full",
            energyLevel = 3,
            createdAt = now,
            updatedAt = now,
        )
        completionDao.insert(original)

        val replacement = original.copy(
            energyLevel = 5,
            note = "Great session",
            updatedAt = now + 1000,
        )
        completionDao.insert(replacement)

        val result = completionDao.getById(id)
        assertNotNull(result)
        assertEquals(5, result.energyLevel)
        assertEquals("Great session", result.note)
    }

    private fun createHabit(name: String): HabitEntity {
        val now = Instant.now().toEpochMilli()
        return HabitEntity(
            id = UUID.randomUUID(),
            name = name,
            anchorBehavior = "After waking up",
            anchorType = "AfterBehavior",
            category = "Morning",
            frequency = "Daily",
            phase = "Initiation",
            status = "Active",
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createCompletion(habitId: UUID, date: String, type: String = "Full",): CompletionEntity {
        val now = Instant.now().toEpochMilli()
        return CompletionEntity(
            id = UUID.randomUUID(),
            habitId = habitId,
            date = date,
            completedAt = now,
            type = type,
            createdAt = now,
            updatedAt = now,
        )
    }
}
