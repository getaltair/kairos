package com.getaltair.kairos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.getaltair.kairos.data.converter.HabitCategoryConverter
import com.getaltair.kairos.data.converter.RoutineStatusConverter
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
import java.util.UUID

/**
 * Room entity representing an ordered sequence of habits for grouped execution.
 * Routines allow users to execute multiple habits in a predetermined order.
 *
 * @property id Unique identifier for this routine
 * @property name Display name (1-50 characters)
 * @property description Optional description
 * @property icon Emoji or icon reference for UI
 * @property color Hex color code for UI
 * @property category Time-of-day category for the routine
 * @property status Current routine status
 * @property userId ID of the user (for sync, nullable)
 * @property createdAt When the routine was created
 * @property updatedAt When the routine was last updated
 */
@Entity(tableName = "routines")
@TypeConverters(HabitCategoryConverter::class, RoutineStatusConverter::class)
data class RoutineEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "category")
    val category: HabitCategory,

    @ColumnInfo(name = "status")
    val status: RoutineStatus,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
