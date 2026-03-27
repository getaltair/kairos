package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.converter.BlockerConverter
import com.getaltair.kairos.data.converter.RecoveryActionConverter
import com.getaltair.kairos.data.entity.RecoverySessionEntity
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction
import java.time.Instant
import java.util.UUID

/**
 * Bidirectional mapper between [RecoverySessionEntity] and [RecoverySession].
 */
object RecoverySessionEntityMapper {

    private val blockerConverter = BlockerConverter()
    private val recoveryActionConverter = RecoveryActionConverter()

    /**
     * Converts [RecoverySessionEntity] to domain [RecoverySession].
     */
    fun toDomain(entity: RecoverySessionEntity): RecoverySession {
        val blockers = try {
            blockerConverter.stringToBlockerList(entity.blockers)
                ?: throw IllegalArgumentException("Invalid blockers JSON")
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse blockers: ${e.message}")
        }

        return RecoverySession(
            id = entity.id,
            habitId = entity.habitId,
            type = entity.type,
            status = entity.status,
            triggeredAt = Instant.ofEpochMilli(entity.triggeredAt),
            completedAt = entity.completedAt?.let { Instant.ofEpochMilli(it) },
            blockers = blockers.toSet(),
            action = entity.action?.let {
                recoveryActionConverter.stringToRecoveryAction(it)
                    ?: throw IllegalArgumentException("Invalid recovery action: $it")
            },
            notes = entity.notes,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    /**
     * Converts domain [RecoverySession] to [RecoverySessionEntity].
     */
    fun toEntity(domain: RecoverySession): RecoverySessionEntity {
        val recoveryAction = domain.action?.let { recoveryActionConverter.recoveryActionToString(it) }
        val blockers = blockerConverter.blockerListToString(domain.blockers.toList()) ?: "[]"

        return RecoverySessionEntity(
            id = domain.id,
            habitId = domain.habitId,
            type = domain.type,
            status = domain.status,
            triggeredAt = domain.triggeredAt.toEpochMilli(),
            completedAt = domain.completedAt?.toEpochMilli(),
            blockers = blockers,
            action = recoveryAction,
            notes = domain.notes,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli()
        )
    }

    /**
     * Converts a list of [RecoverySessionEntity] to domain [List<RecoverySession>].
     */
    fun toDomainList(entities: List<RecoverySessionEntity>): List<RecoverySession> = entities.map { toDomain(it) }

    /**
     * Converts a list of domain [RecoverySession] to [List<RecoverySessionEntity>].
     */
    fun toEntityList(domains: List<RecoverySession>): List<RecoverySessionEntity> = domains.map { toEntity(it) }
}
