package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RecoverySessionEntity
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class RecoverySessionEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly for completed session`() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val entity = RecoverySessionEntity(
            id = id,
            habitId = habitId,
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            triggeredAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            blockers = "[\"NoEnergy\",\"TooBusy\"]",
            action = "Resume",
            notes = "Getting back on track",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RecoverySessionEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(habitId, domain.habitId)
        assertEquals(RecoveryType.Lapse, domain.type)
        assertEquals(SessionStatus.Completed, domain.status)
        assertEquals(now, domain.triggeredAt)
        assertEquals(later, domain.completedAt)
        assertEquals(setOf(Blocker.NoEnergy, Blocker.TooBusy), domain.blockers)
        assertEquals(RecoveryAction.Resume, domain.action)
        assertEquals("Getting back on track", domain.notes)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps pending session with null optional fields`() {
        val entity = RecoverySessionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Relapse,
            status = SessionStatus.Pending,
            triggeredAt = now.toEpochMilli(),
            completedAt = null,
            blockers = "[\"Sick\"]",
            action = null,
            notes = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RecoverySessionEntityMapper.toDomain(entity)

        assertNull(domain.completedAt)
        assertNull(domain.action)
        assertNull(domain.notes)
        assertEquals(RecoveryType.Relapse, domain.type)
        assertEquals(SessionStatus.Pending, domain.status)
    }

    @Test
    fun `toDomain maps all RecoveryAction enum values`() {
        fun entityWithAction(action: String) = RecoverySessionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            triggeredAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            blockers = "[\"NoEnergy\"]",
            action = action,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        assertEquals(RecoveryAction.Resume, RecoverySessionEntityMapper.toDomain(entityWithAction("Resume")).action)
        assertEquals(RecoveryAction.Simplify, RecoverySessionEntityMapper.toDomain(entityWithAction("Simplify")).action)
        assertEquals(RecoveryAction.Pause, RecoverySessionEntityMapper.toDomain(entityWithAction("Pause")).action)
        assertEquals(RecoveryAction.Archive, RecoverySessionEntityMapper.toDomain(entityWithAction("Archive")).action)
        assertEquals(
            RecoveryAction.FreshStart,
            RecoverySessionEntityMapper.toDomain(entityWithAction("FreshStart")).action
        )
    }

    @Test
    fun `toDomain maps all blocker values`() {
        val allBlockers = "[\"NoEnergy\",\"PainPhysical\",\"PainMental\",\"TooBusy\"," +
            "\"FamilyEmergency\",\"WorkEmergency\",\"Sick\",\"Weather\"," +
            "\"EquipmentFailure\",\"Other\"]"
        val entity = RecoverySessionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Pending,
            triggeredAt = now.toEpochMilli(),
            blockers = allBlockers,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RecoverySessionEntityMapper.toDomain(entity)

        val expectedBlockers = setOf(
            Blocker.NoEnergy, Blocker.PainPhysical, Blocker.PainMental,
            Blocker.TooBusy, Blocker.FamilyEmergency, Blocker.WorkEmergency,
            Blocker.Sick, Blocker.Weather, Blocker.EquipmentFailure, Blocker.Other,
        )
        assertEquals(expectedBlockers, domain.blockers)
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = RecoverySessionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Pending,
            triggeredAt = now.toEpochMilli(),
            blockers = "[\"NoEnergy\"]",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RecoverySessionEntityMapper.toDomain(entity)

        assertEquals(now, domain.triggeredAt)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly for completed session`() {
        val session = HabitFactory.recoverySession(
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            triggeredAt = now,
            completedAt = later,
            blockers = setOf(Blocker.NoEnergy, Blocker.TooBusy),
            action = RecoveryAction.Resume,
            notes = "Getting back on track",
            createdAt = now,
            updatedAt = later,
        )

        val entity = RecoverySessionEntityMapper.toEntity(session)

        assertEquals(session.id, entity.id)
        assertEquals(session.habitId, entity.habitId)
        assertEquals(RecoveryType.Lapse, entity.type)
        assertEquals(SessionStatus.Completed, entity.status)
        assertEquals(now.toEpochMilli(), entity.triggeredAt)
        assertEquals(later.toEpochMilli(), entity.completedAt)
        assertTrue(entity.blockers.contains("NoEnergy"))
        assertTrue(entity.blockers.contains("TooBusy"))
        assertEquals("Resume", entity.action)
        assertEquals("Getting back on track", entity.notes)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps pending session with null optional fields`() {
        val session = HabitFactory.recoverySession(
            type = RecoveryType.Relapse,
            status = SessionStatus.Pending,
            triggeredAt = now,
            completedAt = null,
            blockers = setOf(Blocker.Sick),
            action = null,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )

        val entity = RecoverySessionEntityMapper.toEntity(session)

        assertNull(entity.completedAt)
        assertNull(entity.action)
        assertNull(entity.notes)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val session = HabitFactory.recoverySession(
            triggeredAt = now,
            createdAt = now,
            updatedAt = later,
        )

        val entity = RecoverySessionEntityMapper.toEntity(session)

        assertEquals(now.toEpochMilli(), entity.triggeredAt)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = RecoverySessionEntity(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            triggeredAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            blockers = "[\"NoEnergy\",\"TooBusy\"]",
            action = "Resume",
            notes = "Back on track",
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RecoverySessionEntityMapper.toDomain(original)
        val roundTripped = RecoverySessionEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.habitId, roundTripped.habitId)
        assertEquals(original.type, roundTripped.type)
        assertEquals(original.status, roundTripped.status)
        assertEquals(original.triggeredAt, roundTripped.triggeredAt)
        assertEquals(original.completedAt, roundTripped.completedAt)
        assertEquals(original.action, roundTripped.action)
        assertEquals(original.notes, roundTripped.notes)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }
}
