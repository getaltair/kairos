package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineVariantEntity
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RoutineVariantEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val routineId = UUID.randomUUID()
        val entity = RoutineVariantEntity(
            id = id,
            routineId = routineId,
            name = "Quick Version",
            estimatedMinutes = 15,
            isDefault = true,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineVariantEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(routineId, domain.routineId)
        assertEquals("Quick Version", domain.name)
        assertEquals(15, domain.estimatedMinutes)
        assertTrue(domain.isDefault)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps isDefault false correctly`() {
        val entity = RoutineVariantEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Standard",
            estimatedMinutes = 30,
            isDefault = false,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineVariantEntityMapper.toDomain(entity)

        assertFalse(domain.isDefault)
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = RoutineVariantEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Test",
            estimatedMinutes = 10,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineVariantEntityMapper.toDomain(entity)

        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly`() {
        val variant = HabitFactory.routineVariant(
            name = "Quick Version",
            estimatedMinutes = 15,
            isDefault = true,
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineVariantEntityMapper.toEntity(variant)

        assertEquals(variant.id, entity.id)
        assertEquals(variant.routineId, entity.routineId)
        assertEquals("Quick Version", entity.name)
        assertEquals(15, entity.estimatedMinutes)
        assertTrue(entity.isDefault)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps isDefault false correctly`() {
        val variant = HabitFactory.routineVariant(
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )

        val entity = RoutineVariantEntityMapper.toEntity(variant)

        assertFalse(entity.isDefault)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val variant = HabitFactory.routineVariant(
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineVariantEntityMapper.toEntity(variant)

        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = RoutineVariantEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Extended",
            estimatedMinutes = 45,
            isDefault = true,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineVariantEntityMapper.toDomain(original)
        val roundTripped = RoutineVariantEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.routineId, roundTripped.routineId)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.estimatedMinutes, roundTripped.estimatedMinutes)
        assertEquals(original.isDefault, roundTripped.isDefault)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }

    @Test
    fun `round-trip preserves isDefault false`() {
        val original = RoutineVariantEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Minimal",
            estimatedMinutes = 5,
            isDefault = false,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineVariantEntityMapper.toDomain(original)
        val roundTripped = RoutineVariantEntityMapper.toEntity(domain)

        assertFalse(roundTripped.isDefault)
    }
}
