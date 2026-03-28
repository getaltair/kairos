package com.getaltair.kairos.feature.auth.scan

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Response from a successful dashboard auth confirmation.
 */
data class DashboardAuthResponse(val userId: String, val email: String?,)

/**
 * Client that confirms authentication with a Kairos dashboard instance.
 * Sends the user's Firebase ID token to the dashboard's local HTTP server
 * so the desktop app can verify the user's identity.
 */
class DashboardAuthClient {

    /**
     * Posts the Firebase ID token to the dashboard's auth confirmation endpoint.
     *
     * @param host the dashboard host address (e.g. 192.168.1.10)
     * @param port the dashboard HTTP server port (e.g. 8080)
     * @param sessionToken the session token from the QR code
     * @param firebaseIdToken the current user's Firebase ID token
     * @return [Result] containing [DashboardAuthResponse] on success
     */
    suspend fun confirmAuth(
        host: String,
        port: Int,
        sessionToken: String,
        firebaseIdToken: String,
    ): Result<DashboardAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$host:$port/auth/confirm")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject().apply {
                put("sessionToken", sessionToken)
                put("firebaseIdToken", firebaseIdToken)
            }

            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(body.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = runCatching {
                    connection.errorStream?.bufferedReader()?.readText()
                }.getOrNull() ?: "No error body"
                Timber.w(
                    "Dashboard auth failed: HTTP %d -- %s",
                    responseCode,
                    errorBody,
                )
                return@withContext Result.failure(
                    IOException("Dashboard returned HTTP $responseCode"),
                )
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(responseText)

            val response = DashboardAuthResponse(
                userId = json.getString("userId"),
                email = json.optString("email", null),
            )
            Result.success(response)
        } catch (e: IOException) {
            Timber.e(e, "Network error during dashboard auth confirmation")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during dashboard auth confirmation")
            Result.failure(e)
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 10_000
    }
}
