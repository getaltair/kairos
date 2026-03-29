package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.sync.DataCleanup
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orchestrates the full account deletion flow.
 *
 * Performs the following steps in order:
 * 1. Validates that a user is currently signed in.
 * 2. Re-authenticates with the provided password (early validation before any data is touched).
 * 3. Deletes all cloud data for the user (sync listeners are stopped internally).
 * 4. Deletes the authentication account.
 * 5. Clears all local database tables (best-effort; failure does not fail the operation).
 *
 * The ordering ensures that cloud data is removed before the auth account, so a failure
 * after cloud deletion but before auth deletion leaves orphaned auth (recoverable)
 * rather than orphaned cloud data (unrecoverable without the user's identity).
 *
 * If step 4 fails after step 3 has completed, a specific error message is returned
 * informing the user that their data has already been removed.
 */
class DeleteAccountUseCase(private val authRepository: AuthRepository, private val dataCleanup: DataCleanup) {

    suspend operator fun invoke(password: String): Result<Unit> {
        // Step 1: Verify user is signed in
        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            return Result.Error("No user is currently signed in")
        }

        // Step 2: Re-authenticate to validate password before any data changes
        val reauthResult = authRepository.reauthenticate(password)
        if (reauthResult is Result.Error) {
            return reauthResult
        }

        return try {
            // Step 3: Delete all Firestore data for this user
            dataCleanup.deleteCloudData(userId)

            // Step 4: Delete the auth account (Firestore data is already gone at this point)
            val deleteResult = authRepository.deleteAccount()
            if (deleteResult is Result.Error) {
                return Result.Error(
                    "Your data has been deleted, but we could not remove your account. " +
                        "Please try signing out and deleting again, or contact support.",
                    cause = deleteResult.cause,
                )
            }

            // Step 5: Clear local data (best-effort -- account is already gone)
            try {
                dataCleanup.clearLocalData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Best-effort: account is already deleted, log swallowed at caller level
            }

            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error("Account deletion failed. Please try again or contact support.", cause = e)
        }
    }
}
