package com.getaltair.kairos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SimpleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SimpleDatabase : RoomDatabase() {
    abstract fun simpleDao(): SimpleDao
}

@androidx.room.Dao
interface SimpleDao {
    @androidx.room.Query("SELECT * FROM simple_items")
    fun getAll(): List<SimpleEntity>

    @androidx.room.Insert
    fun insert(item: SimpleEntity)
}
