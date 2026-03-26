package com.getaltair.kairos.core.di

import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import com.getaltair.kairos.domain.usecase.CreateHabitUseCase
import com.getaltair.kairos.domain.usecase.GetWeeklyStatsUseCase
import org.koin.dsl.module

/**
 * Koin module providing Today screen and shared use cases.
 * Each use case is a factory (new instance per injection).
 */
val useCaseModule = module {
    factory { GetTodayHabitsUseCase(get(), get()) }
    factory { CompleteHabitUseCase(get(), get()) }
    factory { SkipHabitUseCase(get(), get()) }
    factory { UndoCompletionUseCase(get()) }
    factory { CreateHabitUseCase(get()) }
    factory { GetWeeklyStatsUseCase(get(), get()) }
}
