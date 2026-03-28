package com.getaltair.kairos.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.model.HabitWithStatus
import org.koin.core.context.GlobalContext
import timber.log.Timber

class KairosWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val getTodayHabits: GetTodayHabitsUseCase = GlobalContext.get().get()

        val habits: List<HabitWithStatus> = try {
            when (val result = getTodayHabits()) {
                is Result.Success -> result.value

                is Result.Error -> {
                    Timber.e(result.cause, "Widget: %s", result.message)
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load habits for widget")
            emptyList()
        }

        provideContent {
            KairosWidgetContent(habits = habits)
        }
    }
}
