package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineExecutionEntity
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import java.time.Instant
import java.util.UUID

/**
 * Bidirectional mapper between [RoutineExecutionEntity] and [RoutineExecution].
 */
object RoutineExecutionEntityMapper {

    /**
     * Converts [RoutineExecutionEntity] to domain [RoutineExecution].
     */
    fun toDomain(entity: RoutineExecutionEntity): RoutineExecution = RoutineExecution(
        id = entity.id,
        routineId = entity.routineId,
        variantId = entity.variantId,
        startedAt = Instant.ofEpochMilli(entity.startedAt),
        completedAt = entity.completedAt?.let { Instant.ofEpochMilli(it) },
        status = entity.status,
        currentStepIndex = entity.currentStepIndex,
        currentStepRemainingSeconds = entity.currentStepRemainingSeconds,
        totalPausedSeconds = entity.totalPausedSeconds,
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt)
    )

    /**
     * Converts domain [RoutineExecution] to [RoutineExecutionEntity].
     */
    fun toEntity(domain: RoutineExecution): RoutineExecutionEntity = RoutineExecutionEntity(
        id = domain.id,
        routineId = domain.routineId,
        variantId = domain.variantId,
        startedAt = domain.startedAt.toEpochMilli(),
        completedAt = domain.completedAt?.toEpochMilli(),
        status = domain.status,
        currentStepIndex = domain.currentStepIndex,
        currentStepRemainingSeconds = domain.currentStepRemainingSeconds,
        totalPausedSeconds = domain.totalPausedSeconds,
        createdAt = domain.createdAt.toEpochMilli(),
        updatedAt = domain.updatedAt.toEpochMilli()
    )

    /**
     * Converts a list of [RoutineExecutionEntity] to domain [List<RoutineExecution>].
     */
    fun toDomainList(entities: List<RoutineExecutionEntity>): List<RoutineExecution> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [RoutineExecution] to [List<RoutineExecutionEntity>].
     */
    fun toEntityList(domains: List<RoutineExecution>): List<RoutineExecutionEntity> = domains.map { toEntity(it) }
}
