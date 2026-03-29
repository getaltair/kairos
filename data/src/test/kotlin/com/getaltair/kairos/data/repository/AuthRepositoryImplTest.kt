package com.getaltair.kairos.data.repository

import com.getaltair.kairos.domain.common.Result
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        firebaseAuth = mockk(relaxed = true)
        repository = AuthRepositoryImpl(firebaseAuth)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // ---- getCurrentUserId ----

    @Test
    fun `getCurrentUserId returns uid when user is signed in`() {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "test-uid-123"
        every { firebaseAuth.currentUser } returns user

        val result = repository.getCurrentUserId()

        assertEquals("test-uid-123", result)
    }

    @Test
    fun `getCurrentUserId returns null when user is not signed in`() {
        every { firebaseAuth.currentUser } returns null

        val result = repository.getCurrentUserId()

        assertNull(result)
    }

    // ---- isSignedIn ----

    @Test
    fun `isSignedIn returns true when user exists`() {
        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user

        assertTrue(repository.isSignedIn())
    }

    @Test
    fun `isSignedIn returns false when no user`() {
        every { firebaseAuth.currentUser } returns null

        assertFalse(repository.isSignedIn())
    }

    // ---- signIn ----

    @Test
    fun `signIn returns success when Firebase succeeds`() = runTest {
        val taskMock = mockk<Task<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword("test@test.com", "password") } returns taskMock
        coEvery { taskMock.await() } returns mockk()

        val result = repository.signIn("test@test.com", "password")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `signIn returns error when Firebase throws`() = runTest {
        val taskMock = mockk<Task<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword("test@test.com", "bad") } returns taskMock
        coEvery { taskMock.await() } throws RuntimeException("Invalid credentials")

        val result = repository.signIn("test@test.com", "bad")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to sign in"))
    }

    // ---- signUp ----

    @Test
    fun `signUp returns success when Firebase succeeds`() = runTest {
        val taskMock = mockk<Task<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword("new@test.com", "password") } returns taskMock
        coEvery { taskMock.await() } returns mockk()

        val result = repository.signUp("new@test.com", "password")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `signUp returns error when Firebase throws`() = runTest {
        val taskMock = mockk<Task<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword("dup@test.com", "password") } returns taskMock
        coEvery { taskMock.await() } throws RuntimeException("Email already in use")

        val result = repository.signUp("dup@test.com", "password")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to sign up"))
    }

    // ---- signOut ----

    @Test
    fun `signOut returns success`() = runTest {
        every { firebaseAuth.signOut() } just Runs

        val result = repository.signOut()

        assertTrue(result is Result.Success)
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `signOut returns error when Firebase throws`() = runTest {
        every { firebaseAuth.signOut() } throws RuntimeException("Sign out failed")

        val result = repository.signOut()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to sign out"))
    }

    // ---- resetPassword ----

    @Test
    fun `resetPassword returns success when Firebase succeeds`() = runTest {
        val taskMock = mockk<Task<Void>>()
        every { firebaseAuth.sendPasswordResetEmail("test@test.com") } returns taskMock
        coEvery { taskMock.await() } returns mockk()

        val result = repository.resetPassword("test@test.com")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `resetPassword returns error when Firebase throws`() = runTest {
        val taskMock = mockk<Task<Void>>()
        every { firebaseAuth.sendPasswordResetEmail("unknown@test.com") } returns taskMock
        coEvery { taskMock.await() } throws RuntimeException("User not found")

        val result = repository.resetPassword("unknown@test.com")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to reset password"))
    }
}
