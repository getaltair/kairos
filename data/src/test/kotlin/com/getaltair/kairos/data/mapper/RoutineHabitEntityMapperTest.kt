package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineHabitEntity
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class RoutineHabitEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all scalar fields correctly`() {
        val id = UUID.randomUUID()
        val routineId = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val entity = RoutineHabitEntity(
            id = id,
            routineId = routineId,
            habitId = habitId,
            orderIndex = 2,
            overrideDurationSeconds = 120,
            variantIds = null,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(routineId, domain.routineId)
        assertEquals(habitId, domain.habitId)
        assertEquals(2, domain.orderIndex)
        assertEquals(120, domain.overrideDurationSeconds)
        assertTrue(domain.variantIds.isEmpty())
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain gracefully handles unparseable variantIds JSON`() {
        // Moshi's default UUID handling may fail in unit test context;
        // the mapper catches the exception and falls back to emptyList.
        val entity = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            variantIds = "[\"not-a-uuid\"]",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(entity)

        // Mapper's catch block returns emptyList for parse failures
        assertTrue(domain.variantIds.isEmpty())
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            overrideDurationSeconds = null,
            variantIds = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(entity)

        assertNull(domain.overrideDurationSeconds)
        assertTrue(domain.variantIds.isEmpty())
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(entity)

        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps empty variant list from null`() {
        val entity = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            variantIds = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(entity)

        assertTrue(domain.variantIds.isEmpty())
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all scalar fields correctly`() {
        val routineHabit = HabitFactory.routineHabit(
            orderIndex = 2,
            overrideDurationSeconds = 120,
            variantIds = emptyList(),
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineHabitEntityMapper.toEntity(routineHabit)

        assertEquals(routineHabit.id, entity.id)
        assertEquals(routineHabit.routineId, entity.routineId)
        assertEquals(routineHabit.habitId, entity.habitId)
        assertEquals(2, entity.orderIndex)
        assertEquals(120, entity.overrideDurationSeconds)
        // Empty variantIds maps to null in entity
        assertNull(entity.variantIds)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps null overrideDurationSeconds`() {
        val routineHabit = HabitFactory.routineHabit(
            overrideDurationSeconds = null,
            createdAt = now,
            updatedAt = now,
        )

        val entity = RoutineHabitEntityMapper.toEntity(routineHabit)

        assertNull(entity.overrideDurationSeconds)
    }

    @Test
    fun `toEntity maps empty variantIds to null`() {
        val routineHabit = HabitFactory.routineHabit(
            variantIds = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        val entity = RoutineHabitEntityMapper.toEntity(routineHabit)

        assertNull(entity.variantIds)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val routineHabit = HabitFactory.routineHabit(
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineHabitEntityMapper.toEntity(routineHabit)

        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 3,
            overrideDurationSeconds = 90,
            variantIds = null,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(original)
        val roundTripped = RoutineHabitEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.routineId, roundTripped.routineId)
        assertEquals(original.habitId, roundTripped.habitId)
        assertEquals(original.orderIndex, roundTripped.orderIndex)
        assertEquals(original.overrideDurationSeconds, roundTripped.overrideDurationSeconds)
        assertEquals(original.variantIds, roundTripped.variantIds)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }

    @Test
    fun `round-trip preserves null optional fields`() {
        val original = RoutineHabitEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            overrideDurationSeconds = null,
            variantIds = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineHabitEntityMapper.toDomain(original)
        val roundTripped = RoutineHabitEntityMapper.toEntity(domain)

        assertNull(roundTripped.overrideDurationSeconds)
        assertNull(roundTripped.variantIds)
    }
}
