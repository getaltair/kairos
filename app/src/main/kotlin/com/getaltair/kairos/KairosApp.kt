package com.getaltair.kairos

import android.app.Application
import com.getaltair.kairos.core.di.useCaseModule
import com.getaltair.kairos.data.di.dataModule
import com.getaltair.kairos.feature.habit.di.habitModule
import com.getaltair.kairos.feature.today.di.todayModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class KairosApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Logging
        Timber.plant(Timber.DebugTree())

        // Koin DI
        startKoin {
            androidLogger()
            androidContext(this@KairosApp)
            modules(
                dataModule,
                useCaseModule,
                todayModule,
                habitModule
            )
        }
    }
}
