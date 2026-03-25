package com.getaltair.kairos.data.di

import android.content.Context
import androidx.room.Room
import com.getaltair.kairos.data.database.KairosDatabase
import org.koin.dsl.module

/**
 * Koin dependency injection module for data layer.
 * Provides Room database, all DAOs, and type converters.
 */
val dataModule = module {
    // Temporarily simplified to test compilation

    // Provide Room database instance
    single<KairosDatabase> {
        provideDatabase(it.get())
    }
}

/**
 * Provides Room database instance.
 *
 * @param context Application context for database initialization
 * @return Configured KairosDatabase instance
 */
fun provideDatabase(context: Context): KairosDatabase = Room.databaseBuilder(
    context.applicationContext,
    KairosDatabase::class.java,
    "kairos.db"
)
    .fallbackToDestructiveMigration()
    .build()
