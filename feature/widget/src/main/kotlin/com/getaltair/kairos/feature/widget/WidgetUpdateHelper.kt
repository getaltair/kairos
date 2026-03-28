package com.getaltair.kairos.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object WidgetUpdateHelper {

    suspend fun updateAll(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val widget = KairosWidget()
            val ids = manager.getGlanceIds(widget.javaClass)
            ids.forEach { id -> widget.update(context, id) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update widgets")
        }
    }

    fun updateAllBlocking(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateAll(context)
        }
    }
}
