package com.getaltair.kairos.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [FirebaseInitializer], verifying initialization semantics,
 * idempotency, and the initialized flag behavior.
 *
 * Uses reflection to reset the private [AtomicBoolean] between tests
 * since [FirebaseInitializer] is a Kotlin `object` singleton.
 */
class FirebaseInitializerTest {

    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        // Reset the singleton's AtomicBoolean to false before each test
        val field = FirebaseInitializer::class.java.getDeclaredField("initialized")
        field.isAccessible = true
        (field.get(null) as AtomicBoolean).set(false)

        // Mock FirebaseApp static methods
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.initializeApp(any<Context>(), any()) } returns mockk()
        every { FirebaseApp.getInstance() } throws IllegalStateException("No default app")
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseApp::class)
    }

    // ------------------------------------------------------------------
    // initialize
    // ------------------------------------------------------------------

    @Test
    fun `initialize sets initialized flag`() {
        val config = FirebaseConfig(
            projectId = "test-project",
            applicationId = "1:123:android:abc",
            apiKey = "AIzaTestKey",
        )

        FirebaseInitializer.initialize(mockContext, config)

        assertTrue(FirebaseInitializer.isInitialized())
        verify(exactly = 1) { FirebaseApp.initializeApp(any<Context>(), any()) }
    }

    @Test
    fun `double initialize is idempotent`() {
        val config = FirebaseConfig(
            projectId = "test-project",
            applicationId = "1:123:android:abc",
            apiKey = "AIzaTestKey",
        )

        FirebaseInitializer.initialize(mockContext, config)
        FirebaseInitializer.initialize(mockContext, config)

        assertTrue(FirebaseInitializer.isInitialized())
        // FirebaseApp.initializeApp should only be called once
        verify(exactly = 1) { FirebaseApp.initializeApp(any<Context>(), any()) }
    }

    // ------------------------------------------------------------------
    // initializeFromExisting
    // ------------------------------------------------------------------

    @Test
    fun `initializeFromExisting sets initialized flag`() {
        FirebaseInitializer.initializeFromExisting()

        assertTrue(FirebaseInitializer.isInitialized())
    }

    @Test
    fun `initializeFromExisting is idempotent`() {
        FirebaseInitializer.initializeFromExisting()
        FirebaseInitializer.initializeFromExisting()

        assertTrue(FirebaseInitializer.isInitialized())
    }

    // ------------------------------------------------------------------
    // isInitialized
    // ------------------------------------------------------------------

    @Test
    fun `isInitialized returns false initially`() {
        // FirebaseApp.getInstance() is mocked to throw, and AtomicBoolean is reset
        assertFalse(FirebaseInitializer.isInitialized())
    }
}
