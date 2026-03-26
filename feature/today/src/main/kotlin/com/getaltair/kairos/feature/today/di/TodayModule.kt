package com.getaltair.kairos.feature.today.di

import com.getaltair.kairos.feature.today.TodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val todayModule = module {
    viewModelOf(::TodayViewModel)
}
