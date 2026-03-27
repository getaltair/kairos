package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

/**
 * Signs in a user with email and password credentials.
 *
 * Delegates to [AuthRepository.signIn] and wraps failures in [Result.Error].
 */
class SignInUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<Unit> = try {
        authRepository.signIn(email, password)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to sign in: ${e.message}", cause = e)
    }
}
