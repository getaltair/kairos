package com.getaltair.kairos.sync.di

import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.SyncStateProvider
import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.sync.SyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Koin module that provides the [SyncManager] singleton.
 *
 * All DAO dependencies are resolved from the data module's Koin graph.
 * Also binds [SyncTrigger] so that data-layer repositories can trigger
 * sync without depending on the sync module directly, and [SyncStateProvider]
 * so that UI layers can observe sync state.
 */
val syncModule = module {
    single {
        SyncManager(
            firestore = Firebase.firestore,
            auth = Firebase.auth,
            habitDao = get(),
            completionDao = get(),
            routineDao = get(),
            routineHabitDao = get(),
            routineVariantDao = get(),
            routineExecutionDao = get(),
            recoverySessionDao = get(),
            userPreferencesDao = get(),
        ).also { manager ->
            val authRepo: AuthRepository = get()
            manager.observeAuthAndSync(authRepo.observeAuthState())
        }
    } binds arrayOf(SyncTrigger::class, SyncStateProvider::class)
}
