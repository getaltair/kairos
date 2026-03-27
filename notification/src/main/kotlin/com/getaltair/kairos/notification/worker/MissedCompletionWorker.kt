package com.getaltair.kairos.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.usecase.CreateMissedCompletionsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Daily background worker that back-fills MISSED completions for yesterday.
 *
 * Scheduled as a [PeriodicWorkRequest] that runs once per day (ideally overnight).
 * Creates MISSED completion records for any active habit that had no completion
 * on the previous day, so that lapse detection has a complete history.
 */
class MissedCompletionWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams),
    KoinComponent {

    private val createMissedCompletions: CreateMissedCompletionsUseCase by inject()

    override suspend fun doWork(): Result {
        Timber.d("MissedCompletionWorker starting")

        return when (val result = createMissedCompletions()) {
            is com.getaltair.kairos.domain.common.Result.Success -> {
                Timber.d("MissedCompletionWorker created %d missed completions", result.value)
                Result.success()
            }

            is com.getaltair.kairos.domain.common.Result.Error -> {
                Timber.e("MissedCompletionWorker error: %s", result.message)
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "missed_completion_worker"
        private const val MAX_RETRIES = 3
    }
}
