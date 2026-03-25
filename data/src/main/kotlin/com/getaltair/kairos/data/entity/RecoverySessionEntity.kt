package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.getaltair.kairos.data.converter.RecoveryTypeConverter
import com.getaltair.kairos.data.converter.SessionStatusConverter
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import java.util.UUID

/**
 * Room entity representing a structured return from lapse or relapse.
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
@Entity(
    tableName = "recovery_sessions",
    indices = [
        Index(value = ["habit_id", "status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(RecoveryTypeConverter::class, SessionStatusConverter::class)
data class RecoverySessionEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "habit_id")
    val habitId: UUID,

    @ColumnInfo(name = "type")
    val type: RecoveryType,

    @ColumnInfo(name = "status")
    val status: SessionStatus,

    @ColumnInfo(name = "triggered_at")
    val triggeredAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "blockers")
    val blockers: String,

    @ColumnInfo(name = "action")
    val action: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
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
}
