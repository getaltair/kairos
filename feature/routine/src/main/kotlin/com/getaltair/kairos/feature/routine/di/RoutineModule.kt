package com.getaltair.kairos.feature.routine.di

import com.getaltair.kairos.feature.routine.RoutineBuilderViewModel
import com.getaltair.kairos.feature.routine.RoutineListViewModel
import com.getaltair.kairos.feature.routine.RoutineRunnerViewModel
import com.getaltair.kairos.feature.routine.RoutineSummaryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing the routine feature's ViewModels.
 *
 * - [RoutineListViewModel] has no parameters (loads all active routines).
 * - [RoutineBuilderViewModel] takes an optional [String] routineId (null = create mode).
 * - [RoutineRunnerViewModel] takes a required [String] routineId.
 * - [RoutineSummaryViewModel] takes a required [String] executionId.
 */
val routineModule = module {
    viewModel {
        RoutineListViewModel(
            getActiveRoutinesUseCase = get(),
        )
    }
    viewModel { params ->
        RoutineBuilderViewModel(
            routineId = params.getOrNull<String>(),
            getRoutineDetailUseCase = get(),
            getActiveHabitsUseCase = get(),
            createRoutineUseCase = get(),
        )
    }
    viewModel { params ->
        RoutineRunnerViewModel(
            application = get(),
            routineId = params.get<String>(),
            getRoutineDetailUseCase = get(),
            startRoutineUseCase = get(),
            advanceRoutineStepUseCase = get(),
            completeRoutineUseCase = get(),
            abandonRoutineUseCase = get(),
        )
    }
    viewModel { params ->
        RoutineSummaryViewModel(
            executionId = params.get<String>(),
            getRoutineExecutionUseCase = get(),
            getRoutineDetailUseCase = get(),
        )
    }
}
