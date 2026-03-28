package com.getaltair.kairos.feature.auth.di

import com.getaltair.kairos.feature.auth.AuthViewModel
import com.getaltair.kairos.feature.auth.scan.DashboardAuthClient
import com.getaltair.kairos.feature.auth.scan.DashboardScanViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    viewModelOf(::AuthViewModel)
    singleOf(::DashboardAuthClient)
    viewModel { DashboardScanViewModel(auth = Firebase.auth, dashboardAuthClient = get()) }
}
