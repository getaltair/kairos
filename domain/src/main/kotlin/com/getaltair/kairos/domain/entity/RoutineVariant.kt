package com.getaltair.kairos.domain.entity

import java.time.Instant
import java.util.UUID

/**
 * Represents a variant of a routine with different characteristics.
 * Variants allow users to have multiple versions of the same routine (e.g., Quick, Standard).
 *
 * @property id Unique identifier for this variant
 * @property routineId ID of the parent routine
 * @property name Display name for the variant
 * @property estimatedMinutes Estimated duration in minutes
 * @property isDefault Whether this is the default variant
 * @property createdAt When this variant was created
 * @property updatedAt When this variant was last updated
 */
data class RoutineVariant(
    val id: UUID = UUID.randomUUID(),
    val routineId: UUID,
    val name: String,
    val estimatedMinutes: Int,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(estimatedMinutes > 0) {
            "estimatedMinutes must be positive"
        }
    }

    /**
     * Creates a copy of this variant with the specified changes.
     */
    fun copy(
        name: String = this.name,
        estimatedMinutes: Int = this.estimatedMinutes,
        isDefault: Boolean = this.isDefault,
        updatedAt: Instant = Instant.now()
    ): RoutineVariant = RoutineVariant(
        id = this.id,
        routineId = this.routineId,
        name = name,
        estimatedMinutes = estimatedMinutes,
        isDefault = isDefault,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )
}
