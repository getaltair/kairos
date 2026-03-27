package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

/**
 * Sends a password reset email to the given address.
 *
 * Delegates to [AuthRepository.resetPassword] and wraps failures in [Result.Error].
 */
class ResetPasswordUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String): Result<Unit> = try {
        authRepository.resetPassword(email)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to reset password: ${e.message}", cause = e)
    }
}
