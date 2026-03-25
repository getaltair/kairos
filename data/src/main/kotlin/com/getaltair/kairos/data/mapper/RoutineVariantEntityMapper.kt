package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineVariantEntity
import com.getaltair.kairos.domain.entity.RoutineVariant
import java.time.Instant
import java.util.UUID

/**
 * Bidirectional mapper between [RoutineVariantEntity] and [RoutineVariant].
 */
object RoutineVariantEntityMapper {

    /**
     * Converts [RoutineVariantEntity] to domain [RoutineVariant].
     */
    fun toDomain(entity: RoutineVariantEntity): RoutineVariant = RoutineVariant(
        id = entity.id,
        routineId = entity.routineId,
        name = entity.name,
        estimatedMinutes = entity.estimatedMinutes,
        isDefault = entity.isDefault,
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt)
    )

    /**
     * Converts domain [RoutineVariant] to [RoutineVariantEntity].
     */
    fun toEntity(domain: RoutineVariant): RoutineVariantEntity = RoutineVariantEntity(
        id = domain.id,
        routineId = domain.routineId,
        name = domain.name,
        estimatedMinutes = domain.estimatedMinutes,
        isDefault = domain.isDefault,
        createdAt = domain.createdAt.toEpochMilli(),
        updatedAt = domain.updatedAt.toEpochMilli()
    )

    /**
     * Converts a list of [RoutineVariantEntity] to domain [List<RoutineVariant>].
     */
    fun toDomainList(entities: List<RoutineVariantEntity>): List<RoutineVariant> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [RoutineVariant] to [List<RoutineVariantEntity>].
     */
    fun toEntityList(domains: List<RoutineVariant>): List<RoutineVariantEntity> = domains.map { toEntity(it) }
}
