package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineExecutionEntity
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class RoutineExecutionEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly for completed execution`() {
        val id = UUID.randomUUID()
        val routineId = UUID.randomUUID()
        val variantId = UUID.randomUUID()
        val entity = RoutineExecutionEntity(
            id = id,
            routineId = routineId,
            variantId = variantId,
            startedAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
            currentStepRemainingSeconds = 45,
            totalPausedSeconds = 10,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineExecutionEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(routineId, domain.routineId)
        assertEquals(variantId, domain.variantId)
        assertEquals(now, domain.startedAt)
        assertEquals(later, domain.completedAt)
        assertEquals(ExecutionStatus.Completed, domain.status)
        assertEquals(3, domain.currentStepIndex)
        assertEquals(45, domain.currentStepRemainingSeconds)
        assertEquals(10, domain.totalPausedSeconds)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            variantId = null,
            startedAt = now.toEpochMilli(),
            completedAt = null,
            status = ExecutionStatus.InProgress,
            currentStepIndex = 0,
            currentStepRemainingSeconds = null,
            totalPausedSeconds = 0,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineExecutionEntityMapper.toDomain(entity)

        assertNull(domain.variantId)
        assertNull(domain.completedAt)
        assertNull(domain.currentStepRemainingSeconds)
    }

    @Test
    fun `toDomain maps all ExecutionStatus enum values`() {
        fun entityWithStatus(status: ExecutionStatus) = RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            startedAt = now.toEpochMilli(),
            status = status,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(
            ExecutionStatus.NotStarted,
            RoutineExecutionEntityMapper.toDomain(entityWithStatus(ExecutionStatus.NotStarted)).status
        )
        assertEquals(
            ExecutionStatus.InProgress,
            RoutineExecutionEntityMapper.toDomain(entityWithStatus(ExecutionStatus.InProgress)).status
        )
        assertEquals(
            ExecutionStatus.Paused,
            RoutineExecutionEntityMapper.toDomain(entityWithStatus(ExecutionStatus.Paused)).status
        )
        assertEquals(
            ExecutionStatus.Completed,
            RoutineExecutionEntityMapper.toDomain(entityWithStatus(ExecutionStatus.Completed)).status
        )
        assertEquals(
            ExecutionStatus.Abandoned,
            RoutineExecutionEntityMapper.toDomain(entityWithStatus(ExecutionStatus.Abandoned)).status
        )
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            startedAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            status = ExecutionStatus.Completed,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineExecutionEntityMapper.toDomain(entity)

        assertEquals(now, domain.startedAt)
        assertEquals(later, domain.completedAt)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly`() {
        val variantId = UUID.randomUUID()
        val execution = HabitFactory.routineExecution(
            variantId = variantId,
            startedAt = now,
            completedAt = later,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
            currentStepRemainingSeconds = 45,
            totalPausedSeconds = 10,
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineExecutionEntityMapper.toEntity(execution)

        assertEquals(execution.id, entity.id)
        assertEquals(execution.routineId, entity.routineId)
        assertEquals(variantId, entity.variantId)
        assertEquals(now.toEpochMilli(), entity.startedAt)
        assertEquals(later.toEpochMilli(), entity.completedAt)
        assertEquals(ExecutionStatus.Completed, entity.status)
        assertEquals(3, entity.currentStepIndex)
        assertEquals(45, entity.currentStepRemainingSeconds)
        assertEquals(10, entity.totalPausedSeconds)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps null optional fields correctly`() {
        val execution = HabitFactory.routineExecution(
            variantId = null,
            completedAt = null,
            currentStepRemainingSeconds = null,
            createdAt = now,
            updatedAt = now,
        )

        val entity = RoutineExecutionEntityMapper.toEntity(execution)

        assertNull(entity.variantId)
        assertNull(entity.completedAt)
        assertNull(entity.currentStepRemainingSeconds)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val execution = HabitFactory.routineExecution(
            startedAt = now,
            createdAt = now,
            updatedAt = later,
        )

        val entity = RoutineExecutionEntityMapper.toEntity(execution)

        assertEquals(now.toEpochMilli(), entity.startedAt)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            variantId = UUID.randomUUID(),
            startedAt = now.toEpochMilli(),
            completedAt = later.toEpochMilli(),
            status = ExecutionStatus.Completed,
            currentStepIndex = 5,
            currentStepRemainingSeconds = 30,
            totalPausedSeconds = 120,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = RoutineExecutionEntityMapper.toDomain(original)
        val roundTripped = RoutineExecutionEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.routineId, roundTripped.routineId)
        assertEquals(original.variantId, roundTripped.variantId)
        assertEquals(original.startedAt, roundTripped.startedAt)
        assertEquals(original.completedAt, roundTripped.completedAt)
        assertEquals(original.status, roundTripped.status)
        assertEquals(original.currentStepIndex, roundTripped.currentStepIndex)
        assertEquals(original.currentStepRemainingSeconds, roundTripped.currentStepRemainingSeconds)
        assertEquals(original.totalPausedSeconds, roundTripped.totalPausedSeconds)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }

    @Test
    fun `round-trip preserves null optional fields`() {
        val original = RoutineExecutionEntity(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            variantId = null,
            startedAt = now.toEpochMilli(),
            completedAt = null,
            status = ExecutionStatus.InProgress,
            currentStepIndex = 0,
            currentStepRemainingSeconds = null,
            totalPausedSeconds = 0,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = RoutineExecutionEntityMapper.toDomain(original)
        val roundTripped = RoutineExecutionEntityMapper.toEntity(domain)

        assertNull(roundTripped.variantId)
        assertNull(roundTripped.completedAt)
        assertNull(roundTripped.currentStepRemainingSeconds)
    }
}
