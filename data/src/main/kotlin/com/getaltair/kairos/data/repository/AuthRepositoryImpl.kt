package com.getaltair.kairos.data.repository

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthRepository
import com.getaltair.kairos.domain.repository.AuthState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Firebase-backed implementation of [AuthRepository].
 * Delegates authentication to [FirebaseAuth] and maps results to domain types.
 */
class AuthRepositoryImpl(private val auth: FirebaseAuth) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val state = if (user != null) {
                AuthState.SignedIn(userId = user.uid, email = user.email)
            } else {
                AuthState.SignedOut
            }
            val result = trySend(state)
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "Failed to emit auth state change")
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to sign in email=%s", email)
        Result.Error("Failed to sign in: ${e.message}", cause = e)
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = try {
        auth.createUserWithEmailAndPassword(email, password).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to sign up email=%s", email)
        Result.Error("Failed to sign up: ${e.message}", cause = e)
    }

    override suspend fun signOut(): Result<Unit> = try {
        auth.signOut()
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to sign out")
        Result.Error("Failed to sign out: ${e.message}", cause = e)
    }

    override suspend fun resetPassword(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to send password reset email=%s", email)
        Result.Error("Failed to reset password: ${e.message}", cause = e)
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun isSignedIn(): Boolean = auth.currentUser != null
}
