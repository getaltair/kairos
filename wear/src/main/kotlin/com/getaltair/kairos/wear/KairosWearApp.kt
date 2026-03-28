package com.getaltair.kairos.wear

import android.app.Application
import com.getaltair.kairos.wear.di.wearModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class KairosWearApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@KairosWearApp)
            modules(wearModule)
        }
    }
}
