package com.getaltair.kairos.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "simple_items")
data class SimpleEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String
)
