package com.getaltair.kairos.core.di

import com.getaltair.kairos.domain.usecase.CompleteHabitUseCase
import com.getaltair.kairos.domain.usecase.CreateHabitUseCase
import com.getaltair.kairos.domain.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.domain.usecase.GetWeeklyStatsUseCase
import com.getaltair.kairos.domain.usecase.SkipHabitUseCase
import com.getaltair.kairos.domain.usecase.UndoCompletionUseCase
import org.koin.dsl.module

/**
 * Koin module providing all MVP use cases.
 * Each use case is a factory (new instance per injection).
 */
val useCaseModule = module {
    factory { CreateHabitUseCase(get()) }
    factory { GetTodayHabitsUseCase(get(), get()) }
    factory { CompleteHabitUseCase(get(), get()) }
    factory { SkipHabitUseCase(get(), get()) }
    factory { UndoCompletionUseCase(get()) }
    factory { GetWeeklyStatsUseCase(get(), get()) }
}
