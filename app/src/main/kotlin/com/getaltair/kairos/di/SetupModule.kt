package com.getaltair.kairos.di

import com.getaltair.kairos.data.firebase.FirebaseConfigStore
import com.getaltair.kairos.setup.FirebaseSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Provides [FirebaseConfigStore] (which encrypts and persists credentials)
 * and [FirebaseSetupViewModel] (which orchestrates the setup flow).
 */
val setupModule = module {
    single { FirebaseConfigStore(get()) }
    viewModel { FirebaseSetupViewModel(get()) }
}
