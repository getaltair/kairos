package com.getaltair.kairos.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module

/**
 * Provides Firebase service singletons that the rest of the Koin graph
 * can inject. These are resolved lazily so Firebase must already be
 * initialized (via google-services.json or [FirebaseInitializer]) before
 * any consumer touches them.
 */
val firebaseModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { FirebaseFirestore.getInstance() }
}
