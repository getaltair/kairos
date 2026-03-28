package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.converter.JsonListConverter
import com.getaltair.kairos.data.entity.RoutineHabitEntity
import com.getaltair.kairos.domain.entity.RoutineHabit
import java.time.Instant
import java.util.UUID

/**
 * Bidirectional mapper between [RoutineHabitEntity] and [RoutineHabit].
 */
object RoutineHabitEntityMapper {

    private val jsonListConverter = JsonListConverter()

    /**
     * Converts [RoutineHabitEntity] to domain [RoutineHabit].
     */
    fun toDomain(entity: RoutineHabitEntity): RoutineHabit = RoutineHabit(
        id = entity.id,
        routineId = entity.routineId,
        habitId = entity.habitId,
        orderIndex = entity.orderIndex,
        overrideDurationSeconds = entity.overrideDurationSeconds,
        variantIds = entity.variantIds?.let { variantIdsString ->
            try {
                jsonListConverter.stringToUuidList(variantIdsString)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList(),
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt)
    )

    /**
     * Converts domain [RoutineHabit] to [RoutineHabitEntity].
     */
    fun toEntity(domain: RoutineHabit): RoutineHabitEntity {
        val variantIds = if (domain.variantIds.isEmpty()) {
            null
        } else {
            jsonListConverter.uuidListToString(domain.variantIds)
        }

        return RoutineHabitEntity(
            id = domain.id,
            routineId = domain.routineId,
            habitId = domain.habitId,
            orderIndex = domain.orderIndex,
            overrideDurationSeconds = domain.overrideDurationSeconds,
            variantIds = variantIds,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Converts a list of [RoutineHabitEntity] to domain [List<RoutineHabit>].
     */
    fun toDomainList(entities: List<RoutineHabitEntity>): List<RoutineHabit> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [RoutineHabit] to [List<RoutineHabitEntity>].
     */
    fun toEntityList(domains: List<RoutineHabit>): List<RoutineHabitEntity> = domains.map { toEntity(it) }
}
