package com.getaltair.kairos.wear.di

import com.getaltair.kairos.wear.data.ActionQueue
import com.getaltair.kairos.wear.data.LocalCache
import com.getaltair.kairos.wear.data.WearDataRepository
import com.getaltair.kairos.wear.presentation.HabitDetailViewModel
import com.getaltair.kairos.wear.presentation.HabitListViewModel
import com.getaltair.kairos.wear.presentation.RoutineListViewModel
import com.getaltair.kairos.wear.presentation.RoutineRunnerViewModel
import com.google.android.gms.wearable.Wearable
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val wearModule = module {
    // Data layer
    single { LocalCache(androidContext()) }
    single { ActionQueue(androidContext()) }
    single { Wearable.getDataClient(androidContext()) }
    single { Wearable.getMessageClient(androidContext()) }
    single { Wearable.getCapabilityClient(androidContext()) }
    single {
        WearDataRepository(
            dataClient = get(),
            messageClient = get(),
            capabilityClient = get(),
            localCache = get(),
            actionQueue = get(),
        )
    }

    // ViewModels
    viewModel { HabitListViewModel(repository = get()) }
    viewModel { params ->
        HabitDetailViewModel(
            habitId = params.get<String>(),
            repository = get(),
        )
    }
    viewModel { RoutineListViewModel(repository = get()) }
    viewModel { params ->
        RoutineRunnerViewModel(
            routineId = params.get<String>(),
            repository = get(),
        )
    }
}
