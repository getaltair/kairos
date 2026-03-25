package com.getaltair.kairos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for Kairos.
 * Provides to main database access point and manages all DAOs.
 */
@Database(version = 1)
abstract class KairosDatabase : RoomDatabase()
