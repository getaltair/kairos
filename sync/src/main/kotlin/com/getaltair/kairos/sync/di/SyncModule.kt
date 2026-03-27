package com.getaltair.kairos.sync.di

import com.getaltair.kairos.domain.sync.SyncTrigger
import com.getaltair.kairos.sync.SyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module that provides the [SyncManager] singleton.
 *
 * All DAO dependencies are resolved from the data module's Koin graph.
 * Also binds [SyncTrigger] so that data-layer repositories can trigger
 * sync without depending on the sync module directly.
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
        )
    } bind SyncTrigger::class
}
