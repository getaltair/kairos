package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.HabitEntity
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.DayOfWeek
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class HabitEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val entity = HabitEntity(
            id = id,
            name = "Meditate",
            description = "10 min morning meditation",
            icon = "brain",
            color = "#FF5733",
            anchorBehavior = "After brushing teeth",
            anchorType = "AfterBehavior",
            timeWindowStart = "07:00",
            timeWindowEnd = "08:00",
            category = "Morning",
            frequency = "Custom",
            activeDays = "MONDAY,WEDNESDAY",
            estimatedSeconds = 600,
            microVersion = "5 deep breaths",
            allowPartialCompletion = true,
            subtasks = "[\"Sit down\",\"Set timer\",\"Focus\"]",
            phase = "ONBOARD",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
            pausedAt = null,
            archivedAt = null,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7,
        )

        val domain = HabitEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals("Meditate", domain.name)
        assertEquals("10 min morning meditation", domain.description)
        assertEquals("brain", domain.icon)
        assertEquals("#FF5733", domain.color)
        assertEquals("After brushing teeth", domain.anchorBehavior)
        assertEquals(AnchorType.AfterBehavior, domain.anchorType)
        assertEquals("07:00", domain.timeWindowStart)
        assertEquals("08:00", domain.timeWindowEnd)
        assertEquals(HabitCategory.Morning, domain.category)
        assertEquals(HabitFrequency.Custom, domain.frequency)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), domain.activeDays)
        assertEquals(600, domain.estimatedSeconds)
        assertEquals("5 deep breaths", domain.microVersion)
        assertEquals(true, domain.allowPartialCompletion)
        assertEquals(listOf("Sit down", "Set timer", "Focus"), domain.subtasks)
        assertEquals(HabitPhase.ONBOARD, domain.phase)
        assertEquals(HabitStatus.Active, domain.status)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
        assertNull(domain.pausedAt)
        assertNull(domain.archivedAt)
        assertEquals(3, domain.lapseThresholdDays)
        assertEquals(7, domain.relapseThresholdDays)
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = HabitEntity(
            id = UUID.randomUUID(),
            name = "Walk",
            anchorBehavior = "After lunch",
            anchorType = "AfterBehavior",
            category = "Afternoon",
            frequency = "Daily",
            phase = "ONBOARD",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = HabitEntityMapper.toDomain(entity)

        assertNull(domain.description)
        assertNull(domain.icon)
        assertNull(domain.color)
        assertNull(domain.timeWindowStart)
        assertNull(domain.timeWindowEnd)
        assertNull(domain.activeDays)
        assertNull(domain.microVersion)
        assertNull(domain.subtasks)
        assertNull(domain.pausedAt)
        assertNull(domain.archivedAt)
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val pausedMillis = 1_700_002_000_000L
        val archivedMillis = 1_700_003_000_000L
        val entity = HabitEntity(
            id = UUID.randomUUID(),
            name = "Exercise",
            anchorBehavior = "After waking",
            anchorType = "AtTime",
            category = "Morning",
            frequency = "Daily",
            phase = "LAPSED",
            status = "Paused",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
            pausedAt = pausedMillis,
            archivedAt = archivedMillis,
        )

        val domain = HabitEntityMapper.toDomain(entity)

        assertEquals(Instant.ofEpochMilli(pausedMillis), domain.pausedAt)
        assertEquals(Instant.ofEpochMilli(archivedMillis), domain.archivedAt)
    }

    @Test
    fun `toDomain maps all enum values for anchorType`() {
        val base = HabitEntity(
            name = "Test",
            anchorBehavior = "Test",
            anchorType = "AfterBehavior",
            category = "Morning",
            frequency = "Daily",
            phase = "ONBOARD",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(
            AnchorType.AfterBehavior,
            HabitEntityMapper.toDomain(base.copy(anchorType = "AfterBehavior")).anchorType
        )
        assertEquals(
            AnchorType.BeforeBehavior,
            HabitEntityMapper.toDomain(base.copy(anchorType = "BeforeBehavior")).anchorType
        )
        assertEquals(AnchorType.AtLocation, HabitEntityMapper.toDomain(base.copy(anchorType = "AtLocation")).anchorType)
        assertEquals(AnchorType.AtTime, HabitEntityMapper.toDomain(base.copy(anchorType = "AtTime")).anchorType)
    }

    @Test
    fun `toDomain maps all enum values for category`() {
        val base = HabitEntity(
            name = "Test",
            anchorBehavior = "Test",
            anchorType = "AfterBehavior",
            frequency = "Daily",
            phase = "ONBOARD",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            category = "Morning",
        )

        assertEquals(HabitCategory.Morning, HabitEntityMapper.toDomain(base.copy(category = "Morning")).category)
        assertEquals(HabitCategory.Afternoon, HabitEntityMapper.toDomain(base.copy(category = "Afternoon")).category)
        assertEquals(HabitCategory.Evening, HabitEntityMapper.toDomain(base.copy(category = "Evening")).category)
        assertEquals(HabitCategory.Anytime, HabitEntityMapper.toDomain(base.copy(category = "Anytime")).category)
        assertEquals(HabitCategory.Departure, HabitEntityMapper.toDomain(base.copy(category = "Departure")).category)
    }

    @Test
    fun `toDomain maps all enum values for frequency`() {
        val base = HabitEntity(
            name = "Test",
            anchorBehavior = "Test",
            anchorType = "AfterBehavior",
            category = "Morning",
            phase = "ONBOARD",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            frequency = "Daily",
        )

        assertEquals(HabitFrequency.Daily, HabitEntityMapper.toDomain(base.copy(frequency = "Daily")).frequency)
        assertEquals(HabitFrequency.Weekdays, HabitEntityMapper.toDomain(base.copy(frequency = "Weekdays")).frequency)
        assertEquals(HabitFrequency.Weekends, HabitEntityMapper.toDomain(base.copy(frequency = "Weekends")).frequency)
        assertEquals(HabitFrequency.Custom, HabitEntityMapper.toDomain(base.copy(frequency = "Custom")).frequency)
    }

    @Test
    fun `toDomain maps all enum values for phase`() {
        val base = HabitEntity(
            name = "Test",
            anchorBehavior = "Test",
            anchorType = "AfterBehavior",
            category = "Morning",
            frequency = "Daily",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            phase = "ONBOARD",
        )

        assertEquals(HabitPhase.ONBOARD, HabitEntityMapper.toDomain(base.copy(phase = "ONBOARD")).phase)
        assertEquals(HabitPhase.FORMING, HabitEntityMapper.toDomain(base.copy(phase = "FORMING")).phase)
        assertEquals(HabitPhase.MAINTAINING, HabitEntityMapper.toDomain(base.copy(phase = "MAINTAINING")).phase)
        assertEquals(HabitPhase.LAPSED, HabitEntityMapper.toDomain(base.copy(phase = "LAPSED")).phase)
        assertEquals(HabitPhase.RELAPSED, HabitEntityMapper.toDomain(base.copy(phase = "RELAPSED")).phase)
    }

    @Test
    fun `toDomain maps all enum values for status`() {
        val base = HabitEntity(
            name = "Test",
            anchorBehavior = "Test",
            anchorType = "AfterBehavior",
            category = "Morning",
            frequency = "Daily",
            phase = "ONBOARD",
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            status = "Active",
        )

        assertEquals(HabitStatus.Active, HabitEntityMapper.toDomain(base.copy(status = "Active")).status)
        assertEquals(HabitStatus.Paused, HabitEntityMapper.toDomain(base.copy(status = "Paused")).status)
        assertEquals(HabitStatus.Archived, HabitEntityMapper.toDomain(base.copy(status = "Archived")).status)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly`() {
        val habit = HabitFactory.habit(
            name = "Meditate",
            description = "10 min morning meditation",
            icon = "brain",
            color = "#FF5733",
            anchorBehavior = "After brushing teeth",
            anchorType = AnchorType.AfterBehavior,
            timeWindowStart = "07:00",
            timeWindowEnd = "08:00",
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Custom,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            estimatedSeconds = 600,
            microVersion = "5 deep breaths",
            allowPartialCompletion = true,
            subtasks = listOf("Sit down", "Set timer", "Focus"),
            phase = HabitPhase.FORMING,
            status = HabitStatus.Active,
            createdAt = now,
            updatedAt = later,
            pausedAt = later,
            archivedAt = null,
            lapseThresholdDays = 5,
            relapseThresholdDays = 14,
        )

        val entity = HabitEntityMapper.toEntity(habit)

        assertEquals(habit.id, entity.id)
        assertEquals("Meditate", entity.name)
        assertEquals("10 min morning meditation", entity.description)
        assertEquals("brain", entity.icon)
        assertEquals("#FF5733", entity.color)
        assertEquals("After brushing teeth", entity.anchorBehavior)
        assertEquals("AfterBehavior", entity.anchorType)
        assertEquals("07:00", entity.timeWindowStart)
        assertEquals("08:00", entity.timeWindowEnd)
        assertEquals("Morning", entity.category)
        assertEquals("Custom", entity.frequency)
        assertNotNull(entity.activeDays)
        assertEquals(600, entity.estimatedSeconds)
        assertEquals("5 deep breaths", entity.microVersion)
        assertEquals(true, entity.allowPartialCompletion)
        assertNotNull(entity.subtasks)
        assertEquals("FORMING", entity.phase)
        assertEquals("Active", entity.status)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
        assertEquals(later.toEpochMilli(), entity.pausedAt)
        assertNull(entity.archivedAt)
        assertEquals(5, entity.lapseThresholdDays)
        assertEquals(14, entity.relapseThresholdDays)
    }

    @Test
    fun `toEntity maps null optional fields correctly`() {
        val habit = HabitFactory.habit(
            description = null,
            icon = null,
            color = null,
            timeWindowStart = null,
            timeWindowEnd = null,
            activeDays = null,
            microVersion = null,
            subtasks = null,
            pausedAt = null,
            archivedAt = null,
        )

        val entity = HabitEntityMapper.toEntity(habit)

        assertNull(entity.description)
        assertNull(entity.icon)
        assertNull(entity.color)
        assertNull(entity.timeWindowStart)
        assertNull(entity.timeWindowEnd)
        assertNull(entity.activeDays)
        assertNull(entity.microVersion)
        assertNull(entity.subtasks)
        assertNull(entity.pausedAt)
        assertNull(entity.archivedAt)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val habit = HabitFactory.habit(
            createdAt = now,
            updatedAt = later,
            pausedAt = later,
        )

        val entity = HabitEntityMapper.toEntity(habit)

        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
        assertEquals(later.toEpochMilli(), entity.pausedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = HabitEntity(
            id = UUID.randomUUID(),
            name = "Read",
            description = "Read for 30 min",
            icon = "book",
            color = "#0000FF",
            anchorBehavior = "Before bed",
            anchorType = "BeforeBehavior",
            timeWindowStart = "21:00",
            timeWindowEnd = "22:00",
            category = "Evening",
            frequency = "Weekdays",
            activeDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
            estimatedSeconds = 1800,
            microVersion = "Read 1 page",
            allowPartialCompletion = true,
            subtasks = "[\"Pick book\",\"Set timer\"]",
            phase = "MAINTAINING",
            status = "Active",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
            pausedAt = null,
            archivedAt = null,
            lapseThresholdDays = 4,
            relapseThresholdDays = 10,
        )

        val domain = HabitEntityMapper.toDomain(original)
        val roundTripped = HabitEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.description, roundTripped.description)
        assertEquals(original.icon, roundTripped.icon)
        assertEquals(original.color, roundTripped.color)
        assertEquals(original.anchorBehavior, roundTripped.anchorBehavior)
        assertEquals(original.anchorType, roundTripped.anchorType)
        assertEquals(original.timeWindowStart, roundTripped.timeWindowStart)
        assertEquals(original.timeWindowEnd, roundTripped.timeWindowEnd)
        assertEquals(original.category, roundTripped.category)
        assertEquals(original.frequency, roundTripped.frequency)
        assertEquals(original.activeDays, roundTripped.activeDays)
        assertEquals(original.estimatedSeconds, roundTripped.estimatedSeconds)
        assertEquals(original.microVersion, roundTripped.microVersion)
        assertEquals(original.allowPartialCompletion, roundTripped.allowPartialCompletion)
        assertEquals(original.subtasks, roundTripped.subtasks)
        assertEquals(original.phase, roundTripped.phase)
        assertEquals(original.status, roundTripped.status)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
        assertEquals(original.pausedAt, roundTripped.pausedAt)
        assertEquals(original.archivedAt, roundTripped.archivedAt)
        assertEquals(original.lapseThresholdDays, roundTripped.lapseThresholdDays)
        assertEquals(original.relapseThresholdDays, roundTripped.relapseThresholdDays)
    }
}
