package com.getaltair.kairos.feature.recovery.di

import com.getaltair.kairos.feature.recovery.RecoverySessionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing the recovery feature's ViewModel.
 *
 * The ViewModel takes a [String] parameter (habitId) that is supplied at
 * injection site via `parametersOf(habitId)`.
 */
val recoveryModule = module {
    viewModel { params ->
        RecoverySessionViewModel(
            habitId = params.get<String>(),
            getPendingRecoveriesUseCase = get(),
            completeRecoverySessionUseCase = get()
        )
    }
}
