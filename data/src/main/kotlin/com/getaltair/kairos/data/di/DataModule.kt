package com.getaltair.kairos.data.di

import android.content.Context
import androidx.room.Room
import com.getaltair.kairos.data.database.KairosDatabase
import com.getaltair.kairos.data.database.MIGRATION_1_2
import com.getaltair.kairos.data.repository.AuthRepositoryImpl
import com.getaltair.kairos.data.repository.CompletionRepositoryImpl
import com.getaltair.kairos.data.repository.HabitRepositoryImpl
import com.getaltair.kairos.data.repository.PreferencesRepositoryImpl
import com.getaltair.kairos.data.repository.RecoveryRepositoryImpl
import com.getaltair.kairos.data.repository.RoutineExecutionRepositoryImpl
import com.getaltair.kairos.data.repository.RoutineRepositoryImpl
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.PreferencesRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import com.getaltair.kairos.domain.repository.RoutineRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin dependency injection module for data layer.
 * Provides Room database, all DAOs, repository bindings,
 * and a dedicated CoroutineScope for non-blocking sync operations.
 */
val dataModule = module {

    // Provide Room database instance
    single<KairosDatabase> {
        provideDatabase(androidContext())
    }

    // Dedicated scope for fire-and-forget sync pushes.
    // Uses SupervisorJob so that one failed sync does not cancel others.
    single(named("syncScope")) { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

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
    single<HabitRepository> {
        HabitRepositoryImpl(
            habitDao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<CompletionRepository> {
        CompletionRepositoryImpl(
            completionDao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<PreferencesRepository> {
        PreferencesRepositoryImpl(
            userPreferencesDao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<RecoveryRepository> {
        RecoveryRepositoryImpl(
            dao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<RoutineRepository> {
        RoutineRepositoryImpl(
            routineDao = get(),
            routineHabitDao = get(),
            routineExecutionDao = get(),
            routineVariantDao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<RoutineExecutionRepository> {
        RoutineExecutionRepositoryImpl(
            routineExecutionDao = get(),
            syncTrigger = get(),
            authRepository = get(),
            syncScope = get(named("syncScope")),
        )
    }
    single<AuthRepository> { AuthRepositoryImpl(Firebase.auth) }
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
    .addMigrations(MIGRATION_1_2)
    .build()
