package com.getaltair.kairos.notification.di

import com.getaltair.kairos.notification.HabitReminderBuilder
import com.getaltair.kairos.notification.NotificationScheduler
import com.getaltair.kairos.notification.QuietHoursChecker
import com.getaltair.kairos.notification.ReminderHandler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module providing notification-layer dependencies.
 *
 * Registers the scheduler, builder, quiet hours checker, and reminder handler
 * as singletons so all receivers share the same instances.
 */
val notificationModule = module {
    single { QuietHoursChecker() }
    single { HabitReminderBuilder(androidContext()) }
    single { NotificationScheduler(androidContext(), get()) }
    single {
        ReminderHandler(
            preferencesRepository = get(),
            habitRepository = get(),
            habitNotificationDao = get(),
            habitReminderBuilder = get(),
            notificationScheduler = get(),
            quietHoursChecker = get(),
        )
    }
}
