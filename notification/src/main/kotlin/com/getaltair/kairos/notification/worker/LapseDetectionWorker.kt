package com.getaltair.kairos.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getaltair.kairos.domain.common.Result as DomainResult
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.usecase.DetectLapsesUseCase
import com.getaltair.kairos.domain.usecase.LapseDetection
import com.getaltair.kairos.notification.RecoveryNotificationBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Daily background worker that detects lapse and relapse conditions.
 *
 * Runs after [MissedCompletionWorker] to scan active habits for consecutive
 * missed days. When a lapse or relapse is detected, posts a shame-free
 * notification via [RecoveryNotificationBuilder].
 */
class LapseDetectionWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams),
    KoinComponent {

    private val detectLapses: DetectLapsesUseCase by inject()
    private val habitRepository: HabitRepository by inject()
    private val recoveryNotificationBuilder: RecoveryNotificationBuilder by inject()

    override suspend fun doWork(): Result {
        Timber.d("LapseDetectionWorker starting")

        return when (val result = detectLapses()) {
            is DomainResult.Success -> {
                val affected = result.value
                Timber.d("LapseDetectionWorker detected %d lapse/relapse events", affected.size)

                for (detection in affected) {
                    val habitResult = habitRepository.getById(detection.habitId)
                    if (habitResult is DomainResult.Success) {
                        val habit = habitResult.value
                        when (habit.phase) {
                            is HabitPhase.RELAPSED -> {
                                recoveryNotificationBuilder.postRelapseNotification(
                                    habitId = detection.habitId.toString(),
                                    habitName = habit.name
                                )
                            }

                            is HabitPhase.LAPSED -> {
                                recoveryNotificationBuilder.postLapseNotification(
                                    habitId = detection.habitId.toString(),
                                    habitName = habit.name,
                                    missedDays = detection.consecutiveMissedDays
                                )
                            }

                            else -> {
                                Timber.w(
                                    "Habit %s detected but phase is %s; skipping notification",
                                    detection.habitId,
                                    habit.phase.displayName
                                )
                            }
                        }
                    } else {
                        Timber.w("Could not load habit %s for notification", detection.habitId)
                    }
                }

                Result.success()
            }

            is DomainResult.Error -> {
                Timber.e("LapseDetectionWorker error: %s", result.message)
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "lapse_detection_worker"
        private const val MAX_RETRIES = 3
    }
}
