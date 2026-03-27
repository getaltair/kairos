package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

/**
 * Signs the current user out.
 *
 * Delegates to [AuthRepository.signOut] and wraps failures in [Result.Error].
 */
class SignOutUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(): Result<Unit> = try {
        authRepository.signOut()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to sign out: ${e.message}", cause = e)
    }
}
