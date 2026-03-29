package com.getaltair.kairos

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.getaltair.kairos.core.di.useCaseModule
import com.getaltair.kairos.data.di.dataModule
import com.getaltair.kairos.di.setupModule
import com.getaltair.kairos.domain.sync.DataCleanup
import com.getaltair.kairos.feature.auth.di.authModule
import com.getaltair.kairos.feature.habit.di.habitModule
import com.getaltair.kairos.feature.recovery.di.recoveryModule
import com.getaltair.kairos.feature.routine.di.routineModule
import com.getaltair.kairos.feature.settings.di.settingsModule
import com.getaltair.kairos.feature.today.di.todayModule
import com.getaltair.kairos.feature.widget.di.widgetModule
import com.getaltair.kairos.notification.di.notificationModule
import com.getaltair.kairos.sync.di.syncModule
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Validates that all Koin DI modules can resolve their dependency graphs.
 *
 * Uses Koin 4.x [verify] which performs static analysis of module declarations
 * to catch missing bindings, circular dependencies, and type mismatches at
 * compile/test time rather than at runtime.
 *
 * External types (Android framework, Firebase, Play Services) are declared as
 * extra types since they are provided by the platform or by `androidContext()`
 * at runtime and are not part of the Koin module graph.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinModuleCheckTest {

    /**
     * Platform and framework types that are provided externally at runtime.
     * These are passed to [verify] so it does not flag them as missing bindings.
     */
    private val externalTypes = listOf(
        Context::class,
        Application::class,
        SavedStateHandle::class,
        FirebaseApp::class,
        FirebaseAuth::class,
        FirebaseFirestore::class,
        DataClient::class,
        MessageClient::class,
        CapabilityClient::class,
        DataCleanup::class,
    )

    /**
     * Verifies the full Koin graph exactly as assembled in [KairosApp.onCreate].
     * Catches missing bindings, circular dependencies, and type mismatches
     * across all modules except [firebaseModule].
     *
     * [firebaseModule] is excluded because it uses `getInstance()` factory calls
     * whose internal constructor dependencies (FirebaseApp, Provider, etc.) cannot
     * be statically verified. FirebaseAuth and FirebaseFirestore are instead
     * declared as external types.
     */
    @Test
    fun `all app koin modules verify`() {
        val allModules = module {
            includes(
                setupModule,
                dataModule,
                useCaseModule,
                syncModule,
                notificationModule,
                authModule,
                todayModule,
                habitModule,
                settingsModule,
                recoveryModule,
                routineModule,
                widgetModule,
            )
        }
        allModules.verify(
            extraTypes = externalTypes,
        )
    }

    /**
     * Verifies the data layer (Room database, DAOs, repositories) in isolation.
     * This module only depends on external types (Context, Firebase).
     */
    @Test
    fun `data layer verifies`() {
        dataModule.verify(
            extraTypes = externalTypes,
        )
    }

    /**
     * Verifies the setup module (FirebaseConfigStore + FirebaseSetupViewModel) in isolation.
     */
    @Test
    fun `setup module verifies`() {
        setupModule.verify(
            extraTypes = externalTypes,
        )
    }

    /**
     * Verifies the domain layer (use cases) with its data layer dependency.
     * Use cases depend on repository interfaces provided by [dataModule].
     */
    @Test
    fun `domain layer verifies with data`() {
        val domainWithData = module {
            includes(dataModule, syncModule, useCaseModule)
        }
        domainWithData.verify(
            extraTypes = externalTypes,
        )
    }

    /**
     * Verifies all presentation-layer modules (feature ViewModels, notification,
     * widget) together with their upstream dependencies (data + domain).
     * This confirms the full wiring from ViewModel down to repository.
     *
     * [firebaseModule] is excluded (see [all app koin modules verify] for rationale).
     */
    @Test
    fun `presentation layer verifies with domain and data`() {
        val presentationStack = module {
            includes(
                setupModule,
                dataModule,
                useCaseModule,
                syncModule,
                notificationModule,
                authModule,
                todayModule,
                habitModule,
                settingsModule,
                recoveryModule,
                routineModule,
                widgetModule,
            )
        }
        presentationStack.verify(
            extraTypes = externalTypes,
        )
    }
}
