package com.getaltair.kairos.data.di

import android.content.Context
import androidx.room.Room
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.repository.CompletionRepositoryImpl
import com.getaltair.kairos.data.repository.HabitRepositoryImpl
import com.getaltair.kairos.data.repository.PreferencesRepositoryImpl
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.PreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for data layer.
 * Provides Room database, all DAOs, and repository bindings.
 */
val dataModule = module {

    // Provide Room database instance
    single<KairosDatabase> {
        provideDatabase(androidContext())
    }

    // DAOs
    single { get<KairosDatabase>().habitDao() }
    single { get<KairosDatabase>().completionDao() }
    single { get<KairosDatabase>().userPreferencesDao() }
    single { get<KairosDatabase>().routineDao() }
    single { get<KairosDatabase>().routineVariantDao() }
    single { get<KairosDatabase>().routineHabitDao() }
    single { get<KairosDatabase>().routineExecutionDao() }
    single { get<KairosDatabase>().habitNotificationDao() }
    single { get<KairosDatabase>().recoverySessionDao() }

    // Repository bindings
    single<HabitRepository> { HabitRepositoryImpl(get()) }
    single<CompletionRepository> { CompletionRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
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
    // TODO: Implement proper Room migrations for schema changes
    .build()
