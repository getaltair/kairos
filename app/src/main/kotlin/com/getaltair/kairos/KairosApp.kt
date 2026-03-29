package com.getaltair.kairos

import android.app.Application
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.getaltair.kairos.core.di.useCaseModule
import com.getaltair.kairos.data.di.dataModule
import com.getaltair.kairos.data.firebase.FirebaseConfigStore
import com.getaltair.kairos.data.firebase.FirebaseInitializer
import com.getaltair.kairos.di.firebaseModule
import com.getaltair.kairos.di.setupModule
import com.getaltair.kairos.feature.auth.di.authModule
import com.getaltair.kairos.feature.habit.di.habitModule
import com.getaltair.kairos.feature.recovery.di.recoveryModule
import com.getaltair.kairos.feature.routine.di.routineModule
import com.getaltair.kairos.feature.settings.di.settingsModule
import com.getaltair.kairos.feature.today.di.todayModule
import com.getaltair.kairos.feature.widget.di.widgetModule
import com.getaltair.kairos.notification.NotificationChannels
import com.getaltair.kairos.notification.di.notificationModule
import com.getaltair.kairos.notification.worker.FreshStartWorker
import com.getaltair.kairos.notification.worker.LapseDetectionWorker
import com.getaltair.kairos.notification.worker.MissedCompletionWorker
import com.getaltair.kairos.sync.di.syncModule
import com.getaltair.kairos.wear.WearDataSyncService
import com.google.firebase.FirebaseApp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import timber.log.Timber

class KairosApp : Application() {

    private val _firebaseReady = MutableStateFlow(false)

    /** Observe this to know when Firebase (and all app modules) are available. */
    val firebaseReady = _firebaseReady.asStateFlow()

    override fun onCreate() {
        super.onCreate()

        // Logging
        Timber.plant(Timber.DebugTree())

        // Notification channels (safe to call multiple times)
        NotificationChannels.createAll(this)

        // Phase 1: Start Koin with only the setup module (provides FirebaseConfigStore)
        startKoin {
            androidLogger()
            androidContext(this@KairosApp)
            modules(setupModule)
        }

        // Phase 2: Determine Firebase initialization strategy
        when {
            // google-services plugin already initialized Firebase
            FirebaseApp.getApps(this).isNotEmpty() -> {
                Timber.d("Firebase auto-initialized via google-services.json")
                FirebaseInitializer.initializeFromExisting()
                loadAllAppModules()
            }

            // User previously configured Firebase at runtime
            get<FirebaseConfigStore>().isConfigured() -> {
                val config = get<FirebaseConfigStore>().load()!!
                Timber.d("Initializing Firebase from stored runtime config")
                FirebaseInitializer.initialize(this, config)
                loadAllAppModules()
            }

            // No Firebase config available -- setup screen will handle it
            else -> {
                Timber.d("No Firebase config found; awaiting user setup")
            }
        }
    }

    /**
     * Called from the setup screen after the user provides Firebase credentials.
     * Loads all remaining Koin modules and starts background services.
     */
    fun onFirebaseConfigured() {
        loadAllAppModules()
    }

    private fun loadAllAppModules() {
        loadKoinModules(allAppModules)
        _firebaseReady.value = true

        // Recovery system workers
        scheduleRecoveryWorkers()

        // Start Wear Data Layer sync service
        startWearDataSync()
    }

    /**
     * Starts the [WearDataSyncService] to push habit/completion data to the
     * Wear Data Layer. Wrapped in a try-catch so the app still functions on
     * devices without Google Play Services Wearable support.
     */
    private fun startWearDataSync() {
        try {
            startService(Intent(this, WearDataSyncService::class.java))
        } catch (e: Exception) {
            Timber.w(e, "Could not start WearDataSyncService")
        }
    }

    /**
     * Enqueues the three daily recovery workers via WorkManager.
     *
     * - MissedCompletionWorker: runs at midnight to flag missed completions
     * - LapseDetectionWorker: runs at 2 AM to detect lapses/relapses
     * - FreshStartWorker: runs at 7 AM on Mondays / 1st-of-month for fresh start prompts
     *
     * Uses [ExistingPeriodicWorkPolicy.KEEP] so re-launches do not reset the schedule.
     */
    private fun scheduleRecoveryWorkers() {
        val workManager = WorkManager.getInstance(this)

        scheduleWorker(
            workManager = workManager,
            uniqueName = "missed_completion_worker",
            workerClass = MissedCompletionWorker::class.java,
            targetHour = 0,
            targetMinute = 0,
        )

        scheduleWorker(
            workManager = workManager,
            uniqueName = "lapse_detection_worker",
            workerClass = LapseDetectionWorker::class.java,
            targetHour = 2,
            targetMinute = 0,
        )

        scheduleWorker(
            workManager = workManager,
            uniqueName = "fresh_start_worker",
            workerClass = FreshStartWorker::class.java,
            targetHour = 7,
            targetMinute = 0,
        )
    }

    private fun scheduleWorker(
        workManager: WorkManager,
        uniqueName: String,
        workerClass: Class<out androidx.work.ListenableWorker>,
        targetHour: Int,
        targetMinute: Int,
    ) {
        val initialDelay = computeInitialDelay(targetHour, targetMinute)

        val request = PeriodicWorkRequest.Builder(
            workerClass,
            24,
            TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        /**
         * All Koin modules that require Firebase to be initialized.
         * Loaded either at startup (when google-services.json exists or
         * runtime config is stored) or after the user completes setup.
         */
        private val allAppModules = listOf(
            firebaseModule,
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

        /**
         * Computes the milliseconds from now until the next occurrence of [hour]:[minute].
         * If that time has already passed today, the delay targets tomorrow.
         */
        internal fun computeInitialDelay(hour: Int, minute: Int): Long {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            var target = ZonedDateTime.of(
                LocalDate.now(zone),
                LocalTime.of(hour, minute),
                zone
            )
            if (target.isBefore(now) || target.isEqual(now)) {
                target = target.plusDays(1)
            }
            return Duration.between(now, target).toMillis()
        }
    }
}
