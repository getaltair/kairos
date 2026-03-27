package com.getaltair.kairos.notification.di

import com.getaltair.kairos.notification.HabitReminderBuilder
import com.getaltair.kairos.notification.NotificationScheduler
import com.getaltair.kairos.notification.QuietHoursChecker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module providing notification-layer dependencies.
 *
 * Registers the scheduler, builder, and quiet hours checker as singletons
 * so all receivers share the same instances.
 */
val notificationModule = module {
    single { QuietHoursChecker() }
    single { HabitReminderBuilder(androidContext()) }
    single { NotificationScheduler(androidContext(), get()) }
}
