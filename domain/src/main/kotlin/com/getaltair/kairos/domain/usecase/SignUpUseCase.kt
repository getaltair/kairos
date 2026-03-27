package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

/**
 * Creates a new user account with email and password credentials.
 *
 * Delegates to [AuthRepository.signUp] and wraps failures in [Result.Error].
 */
class SignUpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<Unit> = try {
        authRepository.signUp(email, password)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to sign up: ${e.message}", cause = e)
    }
}
