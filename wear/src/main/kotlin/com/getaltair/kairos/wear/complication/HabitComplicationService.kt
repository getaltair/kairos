package com.getaltair.kairos.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.getaltair.kairos.wear.data.WearDataRepository
import com.getaltair.kairos.wear.presentation.WearMainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import timber.log.Timber

class HabitComplicationService : SuspendingComplicationDataSourceService() {
    private val repository: WearDataRepository by inject()

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> shortText("3 left")
        ComplicationType.LONG_TEXT -> longText("3 habits remaining")
        ComplicationType.RANGED_VALUE -> rangedValue(0.6f)
        ComplicationType.SMALL_IMAGE -> smallImage()
        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest,): ComplicationData? = try {
        // DEPARTURE habits are device-specific triggers, not meant for manual tracking on the watch
        val habits = repository.todayHabits.first()
            .filter { it.category != "DEPARTURE" }
        val completions = repository.todayCompletions.first()
        val completedIds = completions.map { it.habitId }.toSet()
        val completedCount = habits.count { it.id in completedIds }
        val remainingCount = habits.size - completedCount
        val progress = if (habits.isEmpty()) 1f else completedCount.toFloat() / habits.size

        when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val text = when {
                    habits.isEmpty() -> "---"
                    remainingCount == 0 -> "Done"
                    else -> "$remainingCount left"
                }
                shortText(text)
            }

            ComplicationType.LONG_TEXT -> {
                val text = when {
                    habits.isEmpty() -> "No habits today"
                    remainingCount == 0 -> "All done for today"
                    remainingCount == 1 -> "1 habit remaining"
                    else -> "$remainingCount habits remaining"
                }
                longText(text)
            }

            ComplicationType.RANGED_VALUE -> rangedValue(progress)

            ComplicationType.SMALL_IMAGE -> smallImage()

            else -> null
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Error computing complication data")
        // Return a minimal fallback complication so the slot isn't blank on error
        when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortText("---")
            ComplicationType.LONG_TEXT -> longText("---")
            ComplicationType.RANGED_VALUE -> rangedValue(0f)
            ComplicationType.SMALL_IMAGE -> smallImage()
            else -> null
        }
    }

    private fun tapIntent(): PendingIntent? {
        val intent = Intent(this, WearMainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun shortText(text: String): ShortTextComplicationData = ShortTextComplicationData.Builder(
        PlainComplicationText.Builder(text).build(),
        PlainComplicationText.Builder(text).build(),
    )
        .setTapAction(tapIntent())
        .build()

    private fun longText(text: String): LongTextComplicationData = LongTextComplicationData.Builder(
        PlainComplicationText.Builder(text).build(),
        PlainComplicationText.Builder(text).build(),
    )
        .setTapAction(tapIntent())
        .build()

    private fun smallImage(): SmallImageComplicationData = SmallImageComplicationData.Builder(
        smallImage = SmallImage.Builder(
            image = Icon.createWithResource(this, android.R.drawable.sym_def_app_icon),
            type = SmallImageType.ICON,
        ).build(),
        contentDescription = PlainComplicationText.Builder("Kairos habits").build(),
    )
        .setTapAction(tapIntent())
        .build()

    private fun rangedValue(value: Float): RangedValueComplicationData = RangedValueComplicationData.Builder(
        value = value,
        min = 0f,
        max = 1f,
        contentDescription = PlainComplicationText.Builder("Habit progress").build(),
    )
        .setTapAction(tapIntent())
        .build()
}
