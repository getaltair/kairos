package com.getaltair.kairos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [],
    version = 1,
    exportSchema = false
)
abstract class TestKspDatabase : RoomDatabase()
