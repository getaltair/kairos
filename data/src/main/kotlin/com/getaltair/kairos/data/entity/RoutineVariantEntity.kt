package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing a variant of a routine.
 * Variants allow users to have multiple versions of the same routine.
 *
 * @property id Unique identifier for this variant
 * @property routineId ID of the parent routine
 * @property name Display name for the variant
 * @property estimatedMinutes Estimated duration in minutes
 * @property isDefault Whether this is the default variant
 * @property createdAt When this variant was created
 * @property updatedAt When this variant was last updated
 */
@Entity(
    tableName = "routine_variants",
    indices = [
        Index(value = ["routine_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineVariantEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "routine_id")
    val routineId: UUID,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    init {
        require(estimatedMinutes > 0) {
            "estimatedMinutes must be positive"
        }
    }
}
