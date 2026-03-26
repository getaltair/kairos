package com.getaltair.kairos.core.di

import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import org.koin.dsl.module

/**
 * Koin module providing all MVP use cases.
 * Each use case is a factory (new instance per injection).
 */
val useCaseModule = module {
    factory { GetTodayHabitsUseCase(get(), get()) }
    factory { CompleteHabitUseCase(get()) }
    factory { SkipHabitUseCase(get()) }
    factory { UndoCompletionUseCase(get()) }
}
