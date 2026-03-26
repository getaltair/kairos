package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.converter.CompletionTypeConverter
import com.getaltair.kairos.data.converter.SkipReasonConverter
import com.getaltair.kairos.data.entity.CompletionEntity
import com.getaltair.kairos.domain.entity.Completion
import java.time.Instant
import java.time.LocalDate

/**
 * Bidirectional mapper between [CompletionEntity] and [Completion].
 */
object CompletionEntityMapper {

    private val completionTypeConverter = CompletionTypeConverter()
    private val skipReasonConverter = SkipReasonConverter()

    /**
     * Converts [CompletionEntity] to domain [Completion].
     */
    fun toDomain(entity: CompletionEntity): Completion = Completion(
        id = entity.id,
        habitId = entity.habitId,
        date = LocalDate.parse(entity.date),
        completedAt = Instant.ofEpochMilli(entity.completedAt),
        type = completionTypeConverter.stringToCompletionType(entity.type)
            ?: throw IllegalArgumentException("Invalid completion type: ${entity.type}"),
        partialPercent = entity.partialPercent,
        skipReason = entity.skipReason?.let {
            skipReasonConverter.stringToSkipReason(it)
                ?: throw IllegalArgumentException("Invalid skip reason: $it")
        },
        energyLevel = entity.energyLevel,
        note = entity.note,
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        updatedAt = Instant.ofEpochMilli(entity.updatedAt)
    )

    /**
     * Converts domain [Completion] to [CompletionEntity].
     */
    fun toEntity(domain: Completion): CompletionEntity {
        val completionType = completionTypeConverter.completionTypeToString(domain.type)
            ?: "Full"
        val skipReason = domain.skipReason?.let { skipReasonConverter.skipReasonToString(it) }

        return CompletionEntity(
            id = domain.id,
            habitId = domain.habitId,
            date = domain.date.toString(),
            completedAt = domain.completedAt.toEpochMilli(),
            type = completionType,
            partialPercent = domain.partialPercent,
            skipReason = skipReason,
            energyLevel = domain.energyLevel,
            note = domain.note,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Converts a list of [CompletionEntity] to domain [List<Completion>].
     */
    fun toDomainList(entities: List<CompletionEntity>): List<Completion> =
        entities.map { toDomain(it) }

    /**
     * Converts a list of domain [Completion] to [List<CompletionEntity>].
     */
    fun toEntityList(domains: List<Completion>): List<CompletionEntity> =
        domains.map { toEntity(it) }
}
