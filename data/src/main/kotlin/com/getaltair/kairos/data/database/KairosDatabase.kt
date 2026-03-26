package com.getaltair.kairos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.getaltair.kairos.data.dao.CompletionDao
import com.getaltair.kairos.data.dao.HabitDao
import com.getaltair.kairos.data.dao.HabitNotificationDao
import com.getaltair.kairos.data.dao.RecoverySessionDao
import com.getaltair.kairos.data.dao.RoutineDao
import com.getaltair.kairos.data.dao.RoutineExecutionDao
import com.getaltair.kairos.data.dao.RoutineHabitDao
import com.getaltair.kairos.data.dao.RoutineVariantDao
import com.getaltair.kairos.data.dao.UserPreferencesDao

/**
 * Room database for Kairos.
 * Provides to main database access point and manages all DAOs.
 */
@Database(
    version = 1,
    entities = [
        com.getaltair.kairos.data.entity.HabitEntity::class,
        com.getaltair.kairos.data.entity.CompletionEntity::class,
        com.getaltair.kairos.data.entity.RoutineEntity::class,
        com.getaltair.kairos.data.entity.RoutineExecutionEntity::class,
        com.getaltair.kairos.data.entity.RoutineHabitEntity::class,
        com.getaltair.kairos.data.entity.RoutineVariantEntity::class,
        com.getaltair.kairos.data.entity.RecoverySessionEntity::class,
        com.getaltair.kairos.data.entity.HabitNotificationEntity::class,
        com.getaltair.kairos.data.entity.UserPreferencesEntity::class
    ],
    exportSchema = true
)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineHabitDao(): RoutineHabitDao
    abstract fun routineVariantDao(): RoutineVariantDao
    abstract fun routineExecutionDao(): RoutineExecutionDao
    abstract fun recoverySessionDao(): RecoverySessionDao
    abstract fun habitNotificationDao(): HabitNotificationDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}
