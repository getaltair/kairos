package com.getaltair.kairos.feature.auth.scan

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardScanViewModelTest {

    private val auth: FirebaseAuth = mockk()
    private val dashboardAuthClient: DashboardAuthClient = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Uri::class)
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    private fun createViewModel() = DashboardScanViewModel(
        auth = auth,
        dashboardAuthClient = dashboardAuthClient,
    )

    private fun mockUri(
        scheme: String?,
        host: String?,
        hostParam: String? = null,
        portParam: String? = null,
        sessionParam: String? = null,
    ): Uri {
        val uri = mockk<Uri>()
        every { uri.scheme } returns scheme
        every { uri.host } returns host
        every { uri.getQueryParameter("host") } returns hostParam
        every { uri.getQueryParameter("port") } returns portParam
        every { uri.getQueryParameter("session") } returns sessionParam
        return uri
    }

    // -------------------------------------------------------------------------
    // 1. Valid QR URI is parsed correctly and triggers auth confirmation
    // -------------------------------------------------------------------------

    @Test
    fun `valid QR URI triggers auth confirmation and succeeds`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user

        val tokenResult = mockk<GetTokenResult>()
        every { tokenResult.token } returns "firebase-id-token-123"
        val tokenTask = mockk<Task<GetTokenResult>>()
        every { user.getIdToken(true) } returns tokenTask
        coEvery { tokenTask.await() } returns tokenResult

        coEvery {
            dashboardAuthClient.confirmAuth(
                host = "192.168.1.10",
                port = 8080,
                sessionToken = "abc123",
                firebaseIdToken = "firebase-id-token-123",
            )
        } returns Result.success(DashboardAuthResponse(userId = "user-1", email = "test@test.com"))

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080&session=abc123")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DashboardScanUiState.Success)
    }

    // -------------------------------------------------------------------------
    // 2. Invalid URI scheme sets error
    // -------------------------------------------------------------------------

    @Test
    fun `invalid URI scheme sets error state`() = runTest {
        val uri = mockUri(
            scheme = "https",
            host = "link-dashboard",
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("https://link-dashboard?host=x&port=8080&session=y")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Not a valid Kairos dashboard QR code", (state as DashboardScanUiState.Error).message)
    }

    // -------------------------------------------------------------------------
    // 3. Invalid URI host sets error
    // -------------------------------------------------------------------------

    @Test
    fun `invalid URI host sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "other-feature",
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://other-feature?host=x&port=8080&session=y")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Not a valid Kairos dashboard QR code", (state as DashboardScanUiState.Error).message)
    }

    // -------------------------------------------------------------------------
    // 4. Missing query parameters sets error
    // -------------------------------------------------------------------------

    @Test
    fun `missing host query parameter sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = null,
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?port=8080&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Not a valid Kairos dashboard QR code", (state as DashboardScanUiState.Error).message)
    }

    @Test
    fun `missing session query parameter sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = null,
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Not a valid Kairos dashboard QR code", (state as DashboardScanUiState.Error).message)
    }

    // -------------------------------------------------------------------------
    // 5. Non-numeric port sets error
    // -------------------------------------------------------------------------

    @Test
    fun `non-numeric port sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "not-a-number",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=not-a-number&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Not a valid Kairos dashboard QR code", (state as DashboardScanUiState.Error).message)
    }

    // -------------------------------------------------------------------------
    // 6. User not signed in sets error
    // -------------------------------------------------------------------------

    @Test
    fun `user not signed in sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri
        every { auth.currentUser } returns null

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals("Please sign in first", (state as DashboardScanUiState.Error).message)
    }

    // -------------------------------------------------------------------------
    // 7. Network error sets error state
    // -------------------------------------------------------------------------

    @Test
    fun `network error during auth confirmation sets error state`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user

        val tokenResult = mockk<GetTokenResult>()
        every { tokenResult.token } returns "firebase-id-token-123"
        val tokenTask = mockk<Task<GetTokenResult>>()
        every { user.getIdToken(true) } returns tokenTask
        coEvery { tokenTask.await() } returns tokenResult

        coEvery {
            dashboardAuthClient.confirmAuth(
                host = "192.168.1.10",
                port = 8080,
                sessionToken = "abc123",
                firebaseIdToken = "firebase-id-token-123",
            )
        } returns Result.failure(java.io.IOException("Connection refused"))

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals(
            "Unable to link dashboard. Make sure you are on the same network.",
            (state as DashboardScanUiState.Error).message,
        )
    }

    // -------------------------------------------------------------------------
    // 8. resetState returns to Idle
    // -------------------------------------------------------------------------

    @Test
    fun `resetState returns to idle state`() = runTest {
        val uri = mockUri(
            scheme = "https",
            host = "not-kairos",
        )
        every { Uri.parse(any()) } returns uri

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("https://not-kairos")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DashboardScanUiState.Error)

        viewModel.resetState()

        assertTrue(viewModel.uiState.value is DashboardScanUiState.Idle)
    }

    // -------------------------------------------------------------------------
    // 9. Null ID token sets error
    // -------------------------------------------------------------------------

    @Test
    fun `onQrCodeScanned null id token sets error`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "test-user-id"

        val tokenResult = mockk<GetTokenResult>()
        every { tokenResult.token } returns null
        val tokenTask = mockk<Task<GetTokenResult>>()
        every { user.getIdToken(true) } returns tokenTask
        coEvery { tokenTask.await() } returns tokenResult

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals(
            "Unable to verify your identity. Please try again.",
            (state as DashboardScanUiState.Error).message,
        )
    }

    // -------------------------------------------------------------------------
    // 10. Unexpected exception during confirmAuth sets error
    // -------------------------------------------------------------------------

    @Test
    fun `onQrCodeScanned unexpected exception sets error`() = runTest {
        val uri = mockUri(
            scheme = "kairos",
            host = "link-dashboard",
            hostParam = "192.168.1.10",
            portParam = "8080",
            sessionParam = "abc123",
        )
        every { Uri.parse(any()) } returns uri

        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user

        val tokenResult = mockk<GetTokenResult>()
        every { tokenResult.token } returns "firebase-id-token-123"
        val tokenTask = mockk<Task<GetTokenResult>>()
        every { user.getIdToken(true) } returns tokenTask
        coEvery { tokenTask.await() } returns tokenResult

        coEvery {
            dashboardAuthClient.confirmAuth(
                host = "192.168.1.10",
                port = 8080,
                sessionToken = "abc123",
                firebaseIdToken = "firebase-id-token-123",
            )
        } throws RuntimeException("Unexpected failure")

        val viewModel = createViewModel()
        viewModel.onQrCodeScanned("kairos://link-dashboard?host=192.168.1.10&port=8080&session=abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardScanUiState.Error)
        assertEquals(
            "Something went wrong. Please try again.",
            (state as DashboardScanUiState.Error).message,
        )
    }
}
