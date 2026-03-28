package com.getaltair.kairos.feature.widget

import android.content.Context
import com.getaltair.kairos.core.widget.WidgetRefreshNotifier

class GlanceWidgetRefreshNotifier(private val context: Context) : WidgetRefreshNotifier {
    override suspend fun refreshAll() {
        WidgetUpdateHelper.updateAll(context)
    }
}
