package com.getaltair.kairos

import android.app.Application
import com.getaltair.kairos.core.di.useCaseModule
import com.getaltair.kairos.data.di.dataModule
import com.getaltair.kairos.feature.auth.di.authModule
import com.getaltair.kairos.feature.habit.di.habitModule
import com.getaltair.kairos.feature.settings.di.settingsModule
import com.getaltair.kairos.feature.today.di.todayModule
import com.getaltair.kairos.notification.NotificationChannels
import com.getaltair.kairos.notification.di.notificationModule
import com.getaltair.kairos.sync.di.syncModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class KairosApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Logging
        Timber.plant(Timber.DebugTree())

        // Notification channels (safe to call multiple times)
        NotificationChannels.createAll(this)

        // Koin DI
        startKoin {
            androidLogger()
            androidContext(this@KairosApp)
            modules(
                dataModule,
                useCaseModule,
                syncModule,
                notificationModule,
                authModule,
                todayModule,
                habitModule,
                settingsModule,
            )
        }
    }
}
