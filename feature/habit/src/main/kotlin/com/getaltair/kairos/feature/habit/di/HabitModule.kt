package com.getaltair.kairos.feature.habit.di

import com.getaltair.kairos.feature.habit.CreateHabitViewModel
import com.getaltair.kairos.feature.habit.EditHabitViewModel
import com.getaltair.kairos.feature.habit.HabitDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val habitModule = module {
    viewModelOf(::CreateHabitViewModel)
    viewModelOf(::HabitDetailViewModel)
    viewModelOf(::EditHabitViewModel)
}
