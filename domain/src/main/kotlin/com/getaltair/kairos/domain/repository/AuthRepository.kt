package com.getaltair.kairos.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Represents the current authentication state of the user.
 */
sealed class AuthState {
    /**
     * User is signed in with a valid session.
     */
    data class SignedIn(val userId: String, val email: String?) : AuthState() {
        init {
            require(userId.isNotBlank()) { "userId must not be blank" }
        }
    }

    /**
     * User is not signed in.
     */
    data object SignedOut : AuthState()
}

/**
 * Repository interface for authentication operations.
 * Implemented in data layer with Firebase Auth.
 */
interface AuthRepository {
    /**
     * Observes the current authentication state as a reactive flow.
     * Emits [AuthState.SignedIn] or [AuthState.SignedOut] whenever auth state changes.
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Signs in with email and password.
     */
    suspend fun signIn(email: String, password: String): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Creates a new account with email and password.
     */
    suspend fun signUp(email: String, password: String): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Signs the current user out.
     */
    suspend fun signOut(): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Sends a password reset email to the given address.
     */
    suspend fun resetPassword(email: String): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Re-authenticates the current user with their password.
     * Required before sensitive operations like account deletion.
     */
    suspend fun reauthenticate(password: String): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Permanently deletes the current user's authentication account.
     *
     * The caller is responsible for re-authenticating the user (via [reauthenticate])
     * before invoking this method. This method only removes the auth account;
     * data cleanup is the caller's responsibility.
     */
    suspend fun deleteAccount(): com.getaltair.kairos.domain.common.Result<Unit>

    /**
     * Returns the current user's ID, or null if not signed in.
     */
    fun getCurrentUserId(): String?

    /**
     * Returns true if a user is currently signed in.
     */
    fun isSignedIn(): Boolean
}
