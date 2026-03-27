package com.getaltair.kairos.feature.auth.di

import com.getaltair.kairos.feature.auth.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    viewModelOf(::AuthViewModel)
}
