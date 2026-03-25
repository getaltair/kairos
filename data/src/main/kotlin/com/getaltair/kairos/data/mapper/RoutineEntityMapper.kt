package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.RoutineEntity
import com.getaltair.kairos.domain.entity.Routine
import java.time.Instant
import java.util.UUID

/**
 * Bidirectional mapper between [RoutineEntity] and [Routine].
 */
object RoutineEntityMapper {

    /**
     * Converts [RoutineEntity] to domain [Routine].
     */
    fun toDomain(entity: RoutineEntity): Routine = Routine(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        icon = entity.icon,
        color = entity.color,
        category = entity.category,
        status = entity.status,
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt)
    )

    /**
     * Converts domain [Routine] to [RoutineEntity].
     */
    fun toEntity(domain: Routine): RoutineEntity = RoutineEntity(
        id = domain.id,
        name = domain.name,
        description = domain.description,
        icon = domain.icon,
        color = domain.color,
        category = domain.category,
        status = domain.status,
        createdAt = domain.createdAt.toEpochMilli(),
        updatedAt = domain.updatedAt.toEpochMilli()
    )

    /**
     * Converts a list of [RoutineEntity] to domain [List<Routine>].
     */
    fun toDomainList(entities: List<RoutineEntity>): List<Routine> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [Routine] to [List<RoutineEntity>].
     */
    fun toEntityList(domains: List<Routine>): List<RoutineEntity> = domains.map { toEntity(it) }
}
