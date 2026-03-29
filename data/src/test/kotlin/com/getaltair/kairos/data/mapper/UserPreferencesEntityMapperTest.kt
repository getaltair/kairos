package com.getaltair.kairos.data.mapper

import com.getaltair.kairos.data.entity.UserPreferencesEntity
import com.getaltair.kairos.domain.enums.Theme
import com.getaltair.kairos.domain.testutil.HabitFactory
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class UserPreferencesEntityMapperTest {

    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val later = Instant.ofEpochMilli(1_700_001_000_000L)

    // -------------------------------------------------------------------
    // toDomain
    // -------------------------------------------------------------------

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val entity = UserPreferencesEntity(
            id = id,
            userId = "firebase-uid-123",
            notificationEnabled = true,
            defaultReminderTime = LocalTime.of(9, 30),
            theme = Theme.Dark,
            energyTrackingEnabled = true,
            notificationChannels = "{\"push\":true,\"email\":false}",
            quietHoursEnabled = true,
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals("firebase-uid-123", domain.userId)
        assertTrue(domain.notificationEnabled)
        assertEquals(LocalTime.of(9, 30), domain.defaultReminderTime)
        assertEquals(Theme.Dark, domain.theme)
        assertTrue(domain.energyTrackingEnabled)
        assertEquals(true, domain.notificationChannels?.get("push"))
        assertEquals(false, domain.notificationChannels?.get("email"))
        assertTrue(domain.quietHoursEnabled)
        assertEquals(LocalTime.of(22, 0), domain.quietHoursStart)
        assertEquals(LocalTime.of(7, 0), domain.quietHoursEnd)
        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    @Test
    fun `toDomain maps null optional fields correctly`() {
        val entity = UserPreferencesEntity(
            id = UUID.randomUUID(),
            userId = null,
            notificationChannels = null,
            defaultReminderTime = LocalTime.of(9, 0),
            theme = Theme.System,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(entity)

        assertNull(domain.userId)
        assertNull(domain.notificationChannels)
    }

    @Test
    fun `toDomain maps all Theme enum values`() {
        fun entityWithTheme(theme: Theme) = UserPreferencesEntity(
            id = UUID.randomUUID(),
            defaultReminderTime = LocalTime.of(9, 0),
            theme = theme,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        assertEquals(Theme.System, UserPreferencesEntityMapper.toDomain(entityWithTheme(Theme.System)).theme)
        assertEquals(Theme.Light, UserPreferencesEntityMapper.toDomain(entityWithTheme(Theme.Light)).theme)
        assertEquals(Theme.Dark, UserPreferencesEntityMapper.toDomain(entityWithTheme(Theme.Dark)).theme)
    }

    @Test
    fun `toDomain maps boolean fields correctly`() {
        val entity = UserPreferencesEntity(
            id = UUID.randomUUID(),
            notificationEnabled = false,
            defaultReminderTime = LocalTime.of(9, 0),
            theme = Theme.System,
            energyTrackingEnabled = true,
            quietHoursEnabled = false,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(entity)

        assertFalse(domain.notificationEnabled)
        assertTrue(domain.energyTrackingEnabled)
        assertFalse(domain.quietHoursEnabled)
    }

    @Test
    fun `toDomain converts timestamps from epoch millis`() {
        val entity = UserPreferencesEntity(
            id = UUID.randomUUID(),
            defaultReminderTime = LocalTime.of(9, 0),
            theme = Theme.System,
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(entity)

        assertEquals(now, domain.createdAt)
        assertEquals(later, domain.updatedAt)
    }

    // -------------------------------------------------------------------
    // toEntity
    // -------------------------------------------------------------------

    @Test
    fun `toEntity maps all fields correctly`() {
        val prefs = HabitFactory.userPreferences(
            userId = "firebase-uid-456",
            notificationEnabled = true,
            defaultReminderTime = LocalTime.of(8, 0),
            theme = Theme.Light,
            energyTrackingEnabled = true,
            notificationChannels = mapOf("push" to true, "email" to false),
            quietHoursEnabled = false,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(6, 0),
            createdAt = now,
            updatedAt = later,
        )

        val entity = UserPreferencesEntityMapper.toEntity(prefs)

        assertEquals(prefs.id, entity.id)
        assertEquals("firebase-uid-456", entity.userId)
        assertTrue(entity.notificationEnabled)
        assertEquals(LocalTime.of(8, 0), entity.defaultReminderTime)
        assertEquals(Theme.Light, entity.theme)
        assertTrue(entity.energyTrackingEnabled)
        // notificationChannels should be serialized as JSON string
        assertTrue(entity.notificationChannels?.contains("push") == true)
        assertFalse(entity.quietHoursEnabled)
        assertEquals(LocalTime.of(23, 0), entity.quietHoursStart)
        assertEquals(LocalTime.of(6, 0), entity.quietHoursEnd)
        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    @Test
    fun `toEntity maps null optional fields correctly`() {
        val prefs = HabitFactory.userPreferences(
            userId = null,
            notificationChannels = null,
            createdAt = now,
            updatedAt = now,
        )

        val entity = UserPreferencesEntityMapper.toEntity(prefs)

        assertNull(entity.userId)
        assertNull(entity.notificationChannels)
    }

    @Test
    fun `toEntity maps empty notificationChannels to null`() {
        val prefs = HabitFactory.userPreferences(
            notificationChannels = emptyMap(),
            createdAt = now,
            updatedAt = now,
        )

        val entity = UserPreferencesEntityMapper.toEntity(prefs)

        // The mapper skips empty maps (takeIf { it.isNotEmpty() })
        assertNull(entity.notificationChannels)
    }

    @Test
    fun `toEntity converts timestamps to epoch millis`() {
        val prefs = HabitFactory.userPreferences(
            createdAt = now,
            updatedAt = later,
        )

        val entity = UserPreferencesEntityMapper.toEntity(prefs)

        assertEquals(now.toEpochMilli(), entity.createdAt)
        assertEquals(later.toEpochMilli(), entity.updatedAt)
    }

    // -------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------

    @Test
    fun `round-trip entity to domain to entity preserves all fields`() {
        val original = UserPreferencesEntity(
            id = UUID.randomUUID(),
            userId = "user-123",
            notificationEnabled = true,
            defaultReminderTime = LocalTime.of(10, 30),
            theme = Theme.Dark,
            energyTrackingEnabled = true,
            notificationChannels = "{\"push\":true}",
            quietHoursEnabled = true,
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            createdAt = now.toEpochMilli(),
            updatedAt = later.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(original)
        val roundTripped = UserPreferencesEntityMapper.toEntity(domain)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.userId, roundTripped.userId)
        assertEquals(original.notificationEnabled, roundTripped.notificationEnabled)
        assertEquals(original.defaultReminderTime, roundTripped.defaultReminderTime)
        assertEquals(original.theme, roundTripped.theme)
        assertEquals(original.energyTrackingEnabled, roundTripped.energyTrackingEnabled)
        assertEquals(original.quietHoursEnabled, roundTripped.quietHoursEnabled)
        assertEquals(original.quietHoursStart, roundTripped.quietHoursStart)
        assertEquals(original.quietHoursEnd, roundTripped.quietHoursEnd)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }

    @Test
    fun `round-trip preserves null optional fields`() {
        val original = UserPreferencesEntity(
            id = UUID.randomUUID(),
            userId = null,
            notificationChannels = null,
            defaultReminderTime = LocalTime.of(9, 0),
            theme = Theme.System,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )

        val domain = UserPreferencesEntityMapper.toDomain(original)
        val roundTripped = UserPreferencesEntityMapper.toEntity(domain)

        assertNull(roundTripped.userId)
        assertNull(roundTripped.notificationChannels)
    }
}
