package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Observes the current authentication state as a reactive flow.
 *
 * Delegates to [AuthRepository.observeAuthState] and returns a [Flow] of [AuthState].
 */
class ObserveAuthStateUseCase(private val authRepository: AuthRepository) {

    operator fun invoke(): Flow<AuthState> = authRepository.observeAuthState()
}
