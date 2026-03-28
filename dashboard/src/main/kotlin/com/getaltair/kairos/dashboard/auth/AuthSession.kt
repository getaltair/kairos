package com.getaltair.kairos.dashboard.auth

import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [Instant] that persists as ISO-8601 text.
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

/**
 * In-memory representation of a QR login session awaiting confirmation
 * from the mobile app.
 */
data class PendingSession(
    val sessionToken: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant,
)

/**
 * Persisted session written to `session.json` after successful authentication.
 */
@Serializable
data class PersistedSession(
    val userId: String,
    val email: String? = null,
    @Serializable(with = InstantSerializer::class)
    val authenticatedAt: Instant,
)

/**
 * Observable auth state exposed by [AuthSessionManager].
 */
sealed class DashboardAuthState {
    /** Initial state while checking for a persisted session on disk. */
    data object Checking : DashboardAuthState()

    /** No persisted session found; QR login should be displayed. */
    data object Unauthenticated : DashboardAuthState()

    /** A valid session exists for the given user. */
    data class Authenticated(val userId: String, val email: String?) : DashboardAuthState()
}
