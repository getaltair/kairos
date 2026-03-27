package com.getaltair.kairos.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.getaltair.kairos.domain.common.Result as DomainResult
import com.getaltair.kairos.domain.usecase.GetPendingRecoveriesUseCase
import com.getaltair.kairos.notification.RecoveryNotificationBuilder
import java.time.DayOfWeek
import java.time.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Background worker that posts a "fresh start" notification on Mondays
 * and the 1st of each month when there are pending recovery sessions.
 *
 * If today is not an eligible day, the worker returns success immediately
 * as a no-op.
 */
class FreshStartWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams),
    KoinComponent {

    private val getPendingRecoveries: GetPendingRecoveriesUseCase by inject()
    private val recoveryNotificationBuilder: RecoveryNotificationBuilder by inject()

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        if (!isFreshStartDay(today)) {
            Timber.d("FreshStartWorker: not a fresh start day, no-op")
            return Result.success()
        }

        Timber.d("FreshStartWorker: fresh start day detected (%s)", today)

        return when (val result = getPendingRecoveries()) {
            is DomainResult.Success -> {
                val count = result.value.size
                if (count > 0) {
                    Timber.d("FreshStartWorker: posting notification for %d pending recoveries", count)
                    recoveryNotificationBuilder.postFreshStartNotification(count)
                } else {
                    Timber.d("FreshStartWorker: no pending recoveries, skipping notification")
                }
                Result.success()
            }

            is DomainResult.Error -> {
                Timber.e("FreshStartWorker error: %s", result.message)
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    /**
     * Checks if the given date is a Monday or the 1st of the month.
     * Exposed for testing.
     */
    internal fun isFreshStartDay(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.MONDAY || date.dayOfMonth == 1

    companion object {
        const val WORK_NAME = "fresh_start_worker"
        private const val MAX_RETRIES = 3
    }
}
