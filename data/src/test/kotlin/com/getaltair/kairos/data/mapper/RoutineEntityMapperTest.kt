package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class RoutineEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val entity = RoutineEntity(
            id = id,
            name = "Morning Routine",
            description = "Wake-up sequence",
            icon = "sun",
            color = "#FFD700",
            category = HabitCategory.Morning,
            status = RoutineStatus.Active,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals("Morning Routine", domain.name)
        assertEquals("Wake-up sequence", domain.description)
        assertEquals("sun", domain.icon)
        assertEquals("#FFD700", domain.color)
        assertEquals(HabitCategory.Morning, domain.category)
        assertEquals(RoutineStatus.Active, domain.status)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = RoutineEntity(
            id = UUID.randomUUID(),
            name = "Quick",
            description = null,
            icon = null,
            color = null,
            category = HabitCategory.Anytime,
            status = RoutineStatus.Archived,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineEntityMapper.toDomain(entity)

        assertNull(domain.description)
        assertNull(domain.icon)
        assertNull(domain.color)
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = RoutineEntity(
            id = UUID.randomUUID(),
            name = "Test",
            category = HabitCategory.Morning,
            status = RoutineStatus.Active,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineEntityMapper.toDomain(entity)

        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps all HabitCategory enum values`() {
        fun entityWithCategory(category: HabitCategory) = RoutineEntity(
            name = "Test",
            category = category,
            status = RoutineStatus.Active,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(
            HabitCategory.Morning,
            RoutineEntityMapper.toDomain(entityWithCategory(HabitCategory.Morning)).category
        )
        assertEquals(
            HabitCategory.Afternoon,
            RoutineEntityMapper.toDomain(entityWithCategory(HabitCategory.Afternoon)).category
        )
        assertEquals(
            HabitCategory.Evening,
            RoutineEntityMapper.toDomain(entityWithCategory(HabitCategory.Evening)).category
        )
        assertEquals(
            HabitCategory.Anytime,
            RoutineEntityMapper.toDomain(entityWithCategory(HabitCategory.Anytime)).category
        )
        assertEquals(
            HabitCategory.Departure,
            RoutineEntityMapper.toDomain(entityWithCategory(HabitCategory.Departure)).category
        )
    }

    @Test
    fun `toDomain maps all RoutineStatus enum values`() {
        fun entityWithStatus(status: RoutineStatus) = RoutineEntity(
            name = "Test",
            category = HabitCategory.Morning,
            status = status,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(RoutineStatus.Active, RoutineEntityMapper.toDomain(entityWithStatus(RoutineStatus.Active)).status)
        assertEquals(RoutineStatus.Paused, RoutineEntityMapper.toDomain(entityWithStatus(RoutineStatus.Paused)).status)
        assertEquals(
            RoutineStatus.Archived,
            RoutineEntityMapper.toDomain(entityWithStatus(RoutineStatus.Archived)).status
        )
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly`() {
        val routine = HabitFactory.routine(
            name = "Morning Routine",
            description = "Wake-up sequence",
            icon = "sun",
            color = "#FFD700",
            category = HabitCategory.Morning,
            status = RoutineStatus.Active,
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineEntityMapper.toEntity(routine)

        assertEquals(routine.id, entity.id)
        assertEquals("Morning Routine", entity.name)
        assertEquals("Wake-up sequence", entity.description)
        assertEquals("sun", entity.icon)
        assertEquals("#FFD700", entity.color)
        assertEquals(HabitCategory.Morning, entity.category)
        assertEquals(RoutineStatus.Active, entity.status)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps null optional fields correctly`() {
        val routine = HabitFactory.routine(
            description = null,
            icon = null,
            color = null,
            createdAt = now,
            updatedAt = now,
        )

        val entity = RoutineEntityMapper.toEntity(routine)

        assertNull(entity.description)
        assertNull(entity.icon)
        assertNull(entity.color)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val routine = HabitFactory.routine(
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineEntityMapper.toEntity(routine)

        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = RoutineEntity(
            id = UUID.randomUUID(),
            name = "Evening Routine",
            description = "Wind-down sequence",
            icon = "moon",
            color = "#191970",
            category = HabitCategory.Evening,
            status = RoutineStatus.Paused,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineEntityMapper.toDomain(original)
        val roundTripped = RoutineEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.description, roundTripped.description)
        assertEquals(original.icon, roundTripped.icon)
        assertEquals(original.color, roundTripped.color)
        assertEquals(original.category, roundTripped.category)
        assertEquals(original.status, roundTripped.status)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }
}
