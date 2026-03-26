package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A record of habit execution for a specific day.
 * Each completion is tied to a specific habit and date.
 *
 * @property id Unique identifier for this completion
 * @property habitId ID of the associated habit
 * @property date The habit date (date when the habit was due)
 * @property completedAt When the completion was logged
 * @property type The type of completion (FULL, PARTIAL, SKIPPED, MISSED)
 * @property partialPercent Percentage for partial completions (1-99)
 * @property skipReason Optional reason for skipping
 * @property energyLevel User's energy level (1-5) during completion
 * @property note Optional free text note
 * @property createdAt When this completion record was created
 * @property updatedAt When this completion record was last updated
 */
data class Completion(
    val id: UUID = UUID.randomUUID(),
    val habitId: UUID,
    val date: LocalDate,
    val completedAt: Instant = Instant.now(),
    val type: CompletionType,
    val partialPercent: Int? = null,
    val skipReason: SkipReason? = null,
    val energyLevel: Int? = null,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(type is CompletionType.Partial || partialPercent == null) {
            "partialPercent must only be set for PARTIAL type"
        }
        require(type is CompletionType.Skipped || skipReason == null) {
            "skipReason must only be set for SKIPPED type"
        }
        if (partialPercent != null) {
            require(partialPercent in 1..99) { "partialPercent must be in 1..99" }
        }
        if (energyLevel != null) {
            require(energyLevel in 1..5) { "energyLevel must be in 1..5" }
        }
    }

    /**
     * Creates a copy of this completion with the specified changes.
     */
    fun copy(
        type: CompletionType = this.type,
        partialPercent: Int? = this.partialPercent,
        skipReason: SkipReason? = this.skipReason,
        energyLevel: Int? = this.energyLevel,
        note: String? = this.note,
        updatedAt: Instant = Instant.now()
    ): Completion = Completion(
        id = this.id,
        habitId = this.habitId,
        date = this.date,
        completedAt = this.completedAt,
        type = type,
        partialPercent = partialPercent,
        skipReason = skipReason,
        energyLevel = energyLevel,
        note = note,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )
}
