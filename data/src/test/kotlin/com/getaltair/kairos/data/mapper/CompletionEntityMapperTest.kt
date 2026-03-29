package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class CompletionEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly for FULL completion`() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val entity = CompletionEntity(
            id = id,
            habitId = habitId,
            date = "2025-03-15",
            completedAt = now.toEpochMilli(),
            type = "Full",
            partialPercent = null,
            skipReason = null,
            energyLevel = 4,
            note = "Felt great",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = CompletionEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(habitId, domain.habitId)
        assertEquals(LocalDate.of(2025, 3, 15), domain.date)
        assertEquals(now, domain.completedAt)
        assertEquals(CompletionType.Full, domain.type)
        assertNull(domain.partialPercent)
        assertNull(domain.skipReason)
        assertEquals(4, domain.energyLevel)
        assertEquals("Felt great", domain.note)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps PARTIAL completion with partialPercent`() {
        val entity = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-06-01",
            completedAt = now.toEpochMilli(),
            type = "Partial",
            partialPercent = 50,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = CompletionEntityMapper.toDomain(entity)

        assertEquals(CompletionType.Partial, domain.type)
        assertEquals(50, domain.partialPercent)
    }

    @Test
    fun `toDomain maps SKIPPED completion with skipReason`() {
        val entity = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-06-01",
            completedAt = now.toEpochMilli(),
            type = "Skipped",
            skipReason = "TooTired",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = CompletionEntityMapper.toDomain(entity)

        assertEquals(CompletionType.Skipped, domain.type)
        assertEquals(SkipReason.TooTired, domain.skipReason)
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-06-01",
            completedAt = now.toEpochMilli(),
            type = "Full",
            energyLevel = null,
            note = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = CompletionEntityMapper.toDomain(entity)

        assertNull(domain.energyLevel)
        assertNull(domain.note)
        assertNull(domain.partialPercent)
        assertNull(domain.skipReason)
    }

    @Test
    fun `toDomain maps all CompletionType enum values`() {
        fun entityWithType(type: String, partialPercent: Int? = null, skipReason: String? = null) = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-01-01",
            completedAt = now.toEpochMilli(),
            type = type,
            partialPercent = partialPercent,
            skipReason = skipReason,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(CompletionType.Full, CompletionEntityMapper.toDomain(entityWithType("Full")).type)
        assertEquals(
            CompletionType.Partial,
            CompletionEntityMapper.toDomain(entityWithType("Partial", partialPercent = 50)).type
        )
        assertEquals(
            CompletionType.Skipped,
            CompletionEntityMapper.toDomain(entityWithType("Skipped", skipReason = "NoTime")).type
        )
        assertEquals(CompletionType.Missed, CompletionEntityMapper.toDomain(entityWithType("Missed")).type)
    }

    @Test
    fun `toDomain maps all SkipReason enum values`() {
        fun entityWithSkipReason(reason: String) = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-01-01",
            completedAt = now.toEpochMilli(),
            type = "Skipped",
            skipReason = reason,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(SkipReason.TooTired, CompletionEntityMapper.toDomain(entityWithSkipReason("TooTired")).skipReason)
        assertEquals(SkipReason.NoTime, CompletionEntityMapper.toDomain(entityWithSkipReason("NoTime")).skipReason)
        assertEquals(
            SkipReason.NotFeelingWell,
            CompletionEntityMapper.toDomain(entityWithSkipReason("NotFeelingWell")).skipReason
        )
        assertEquals(
            SkipReason.Traveling,
            CompletionEntityMapper.toDomain(entityWithSkipReason("Traveling")).skipReason
        )
        assertEquals(
            SkipReason.TookDayOff,
            CompletionEntityMapper.toDomain(entityWithSkipReason("TookDayOff")).skipReason
        )
        assertEquals(SkipReason.Other, CompletionEntityMapper.toDomain(entityWithSkipReason("Other")).skipReason)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly for FULL completion`() {
        val completion = HabitFactory.completion(
            date = LocalDate.of(2025, 3, 15),
            completedAt = now,
            type = CompletionType.Full,
            energyLevel = 4,
            note = "Felt great",
            createdAt = now,
            updatedAt = later,
        )

        val entity = CompletionEntityMapper.toEntity(completion)

        assertEquals(completion.id, entity.id)
        assertEquals(completion.habitId, entity.habitId)
        assertEquals("2025-03-15", entity.date)
        assertEquals(now.toEpochMilli(), entity.completedAt)
        assertEquals("Full", entity.type)
        assertNull(entity.partialPercent)
        assertNull(entity.skipReason)
        assertEquals(4, entity.energyLevel)
        assertEquals("Felt great", entity.note)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps PARTIAL completion with partialPercent`() {
        val completion = HabitFactory.completion(
            type = CompletionType.Partial,
            partialPercent = 75,
            createdAt = now,
            updatedAt = now,
        )

        val entity = CompletionEntityMapper.toEntity(completion)

        assertEquals("Partial", entity.type)
        assertEquals(75, entity.partialPercent)
    }

    @Test
    fun `toEntity maps SKIPPED completion with skipReason`() {
        val completion = HabitFactory.completion(
            type = CompletionType.Skipped,
            skipReason = SkipReason.Traveling,
            createdAt = now,
            updatedAt = now,
        )

        val entity = CompletionEntityMapper.toEntity(completion)

        assertEquals("Skipped", entity.type)
        assertEquals("Traveling", entity.skipReason)
    }

    @Test
    fun `toEntity converts date to ISO string`() {
        val completion = HabitFactory.completion(
            date = LocalDate.of(2025, 12, 31),
            createdAt = now,
            updatedAt = now,
        )

        val entity = CompletionEntityMapper.toEntity(completion)

        assertEquals("2025-12-31", entity.date)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val completion = HabitFactory.completion(
            completedAt = now,
            createdAt = now,
            updatedAt = later,
        )

        val entity = CompletionEntityMapper.toEntity(completion)

        assertEquals(now.toEpochMilli(), entity.completedAt)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = CompletionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = "2025-06-15",
            completedAt = now.toEpochMilli(),
            type = "Full",
            energyLevel = 3,
            note = "Decent session",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = CompletionEntityMapper.toDomain(original)
        val roundTripped = CompletionEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.habitId, roundTripped.habitId)
        assertEquals(original.date, roundTripped.date)
        assertEquals(original.completedAt, roundTripped.completedAt)
        assertEquals(original.type, roundTripped.type)
        assertEquals(original.partialPercent, roundTripped.partialPercent)
        assertEquals(original.skipReason, roundTripped.skipReason)
        assertEquals(original.energyLevel, roundTripped.energyLevel)
        assertEquals(original.note, roundTripped.note)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }
}
