package com.getaltair.kairos.feature.widget.di

import com.getaltair.kairos.core.widget.WidgetRefreshNotifier
import com.getaltair.kairos.feature.widget.GlanceWidgetRefreshNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val widgetModule = module {
    single<WidgetRefreshNotifier> { GlanceWidgetRefreshNotifier(androidContext()) }
}
