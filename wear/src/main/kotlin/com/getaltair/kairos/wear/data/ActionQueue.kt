package com.getaltair.kairos.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.getaltair.kairos.domain.wear.WearAction
import com.getaltair.kairos.domain.wear.splitJsonObjects
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.actionQueueDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wear_action_queue",
)

/**
 * Persists pending WearAction items in DataStore so they survive process death.
 * When the phone reconnects, WearDataRepository.flushQueue() drains this queue
 * and sends all buffered actions.
 */
class ActionQueue(private val context: Context) {
    private val queueKey = stringPreferencesKey("pending_actions")

    suspend fun enqueue(action: WearAction) {
        try {
            context.actionQueueDataStore.edit { prefs ->
                val existing = prefs[queueKey] ?: "[]"
                val trimmed = existing.trim().removePrefix("[").removeSuffix("]")
                val newEntry = action.toJson()
                prefs[queueKey] = if (trimmed.isBlank()) "[$newEntry]" else "[$trimmed,$newEntry]"
            }
        } catch (e: IOException) {
            Timber.e(e, "ActionQueue: failed to enqueue action")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "ActionQueue: failed to enqueue action")
            throw IOException("Failed to enqueue action", e)
        }
    }

    suspend fun dequeueAll(): List<WearAction> {
        var captured = "[]"
        context.actionQueueDataStore.edit { prefs ->
            captured = prefs[queueKey] ?: "[]"
            prefs[queueKey] = "[]"
        }
        return parseActionList(captured)
    }

    suspend fun isEmpty(): Boolean {
        val json = context.actionQueueDataStore.data.map { it[queueKey] ?: "[]" }.first()
        return parseActionList(json).isEmpty()
    }

    private fun parseActionList(json: String): List<WearAction> {
        if (json.isBlank() || json == "[]") return emptyList()
        val trimmed = json.trim().removePrefix("[").removeSuffix("]").trim()
        if (trimmed.isEmpty()) return emptyList()
        return splitJsonObjects(trimmed).mapNotNull { obj ->
            WearAction.fromJson(obj).also { action ->
                if (action == null) {
                    Timber.w("ActionQueue: dropping unrecognized action: %s", obj)
                }
            }
        }
    }
}
