package com.getaltair.kairos.di

import com.getaltair.kairos.data.firebase.FirebaseConfigStore
import com.getaltair.kairos.setup.FirebaseSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the Firebase setup / onboarding flow.
 * Provides the [FirebaseConfigStore] and [FirebaseSetupViewModel]
 * so the setup screen can persist user-entered credentials.
 */
val setupModule = module {
    single { FirebaseConfigStore(get()) }
    viewModel { FirebaseSetupViewModel(get()) }
}
