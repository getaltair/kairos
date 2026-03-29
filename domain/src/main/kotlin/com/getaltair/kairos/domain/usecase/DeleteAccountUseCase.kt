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
 * 3. Stops Firestore snapshot listeners to prevent callbacks during deletion.
 * 4. Deletes all Firestore cloud data for the user.
 * 5. Deletes the Firebase Auth account.
 * 6. Clears all local Room database tables.
 *
 * The ordering ensures that cloud data is removed before the auth account, so a failure
 * after Firestore deletion but before auth deletion leaves orphaned auth (recoverable)
 * rather than orphaned cloud data (unrecoverable without the user's identity).
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
            // Step 3: Stop sync listeners to prevent snapshot callbacks during deletion
            dataCleanup.stopSyncListeners()

            // Step 4: Delete all Firestore data for this user
            dataCleanup.deleteCloudData(userId)

            // Step 5: Delete the Firebase Auth account
            val deleteResult = authRepository.deleteAccount(password)
            if (deleteResult is Result.Error) {
                return deleteResult
            }

            // Step 6: Clear all local Room data
            dataCleanup.clearLocalData()

            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error("Account deletion failed: ${e.message}", cause = e)
        }
    }
}
