package com.getaltair.kairos.feature.habit.di

import com.getaltair.kairos.feature.habit.CreateHabitViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val habitModule = module {
    viewModelOf(::CreateHabitViewModel)
}
