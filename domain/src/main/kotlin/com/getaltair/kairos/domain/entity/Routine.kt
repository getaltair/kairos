package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
import java.time.Instant
import java.util.UUID

/**
 * An ordered sequence of habits for grouped execution.
 * Routines allow users to execute multiple habits in a predetermined order.
 *
 * @property id Unique identifier for this routine
 * @property name Display name (1-50 characters)
 * @property description Optional description
 * @property icon Emoji or icon reference for UI
 * @property color Hex color code for UI
 * @property category Time-of-day category for the routine
 * @property status Current routine status
 * @property createdAt When the routine was created
 * @property updatedAt When the routine was last updated
 */
data class Routine(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val category: HabitCategory,
    val status: RoutineStatus = RoutineStatus.Active,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Creates a copy of this routine with the specified changes.
     */
    fun copy(
        name: String = this.name,
        description: String? = this.description,
        icon: String? = this.icon,
        color: String? = this.color,
        category: HabitCategory = this.category,
        status: RoutineStatus = this.status,
        updatedAt: Instant = Instant.now()
    ): Routine = Routine(
        id = this.id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        category = category,
        status = status,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )

    val isActive: Boolean
        get() = status == RoutineStatus.Active
}
