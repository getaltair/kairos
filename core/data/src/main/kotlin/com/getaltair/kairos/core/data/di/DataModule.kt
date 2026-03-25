package com.getaltair.kairos.core.data.di

import org.koin.dsl.module

val dataModule = module {
    // Room database, DAOs, and repositories will be defined here in Step 3.
    // Example:
    // single { provideDatabase(androidContext()) }
    // single { get<KairosDatabase>().habitDao() }
    // single<HabitRepository> { HabitRepositoryImpl(get()) }
}
