package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import java.time.Instant
import java.util.UUID

/**
 * A structured return from lapse or relapse.
 * Created when a habit misses its threshold number of consecutive days.
 *
 * @property id Unique identifier for this session
 * @property habitId ID of the associated habit
 * @property type Whether this is a lapse or relapse session
 * @property status Current session status
 * @property triggeredAt When the session was created/triggered
 * @property completedAt When the session was completed (nullable)
 * @property blockers Selected blockers reported by the user
 * @property action The chosen recovery action
 * @property notes Optional free text notes
 * @property createdAt When this session record was created
 * @property updatedAt When this session record was last updated
 */
data class RecoverySession(
    val id: UUID = UUID.randomUUID(),
    val habitId: UUID,
    val type: RecoveryType,
    val status: SessionStatus = SessionStatus.Pending,
    val triggeredAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val blockers: List<Blocker>,
    val action: RecoveryAction? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(blockers.isNotEmpty()) {
            "blockers cannot be empty"
        }
        require(
            status == SessionStatus.Pending ||
                (status == SessionStatus.Completed && action != null) ||
                (status == SessionStatus.Abandoned && action == null)
        ) {
            "Invalid status/action combination"
        }
    }

    /**
     * Creates a copy of this session with the specified changes.
     *
     * Type is mutable to support REC-4 one-way escalation (Lapse -> Relapse).
     * Callers must enforce direction -- de-escalation is never valid.
     */
    fun copy(
        type: RecoveryType = this.type,
        status: SessionStatus = this.status,
        completedAt: Instant? = this.completedAt,
        action: RecoveryAction? = this.action,
        notes: String? = this.notes,
        updatedAt: Instant = Instant.now()
    ): RecoverySession = RecoverySession(
        id = this.id,
        habitId = this.habitId,
        type = type,
        status = status,
        triggeredAt = this.triggeredAt,
        completedAt = completedAt,
        blockers = this.blockers,
        action = action,
        notes = notes,
        createdAt = this.createdAt,
        updatedAt = updatedAt
    )

    /**
     * Checks if the session is still pending user action.
     */
    val isPending: Boolean
        get() = status == SessionStatus.Pending

    /**
     * Checks if the session has been completed.
     */
    val isCompleted: Boolean
        get() = status == SessionStatus.Completed
}
