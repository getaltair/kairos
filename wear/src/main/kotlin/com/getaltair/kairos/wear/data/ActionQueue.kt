package com.getaltair.kairos.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.getaltair.kairos.domain.wear.WearAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.actionQueueDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wear_action_queue",
)

class ActionQueue(private val context: Context) {
    private val queueKey = stringPreferencesKey("pending_actions")

    suspend fun enqueue(action: WearAction) {
        context.actionQueueDataStore.edit { prefs ->
            val existing = prefs[queueKey] ?: "[]"
            val trimmed = existing.trim().removePrefix("[").removeSuffix("]")
            val newEntry = action.toJson()
            prefs[queueKey] = if (trimmed.isBlank()) "[$newEntry]" else "[$trimmed,$newEntry]"
        }
    }

    suspend fun dequeueAll(): List<WearAction> {
        val json = context.actionQueueDataStore.data.map { it[queueKey] ?: "[]" }.first()
        val actions = parseActionList(json)
        context.actionQueueDataStore.edit { it[queueKey] = "[]" }
        return actions
    }

    suspend fun isEmpty(): Boolean {
        val json = context.actionQueueDataStore.data.map { it[queueKey] ?: "[]" }.first()
        return parseActionList(json).isEmpty()
    }

    private fun parseActionList(json: String): List<WearAction> {
        if (json.isBlank() || json == "[]") return emptyList()
        val trimmed = json.trim().removePrefix("[").removeSuffix("]").trim()
        if (trimmed.isEmpty()) return emptyList()
        return splitJsonObjects(trimmed).mapNotNull { WearAction.fromJson(it) }
    }

    private fun splitJsonObjects(s: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in s.indices) {
            when (s[i]) {
                '{' -> depth++

                '}' -> {
                    depth--
                    if (depth == 0) {
                        result.add(s.substring(start, i + 1))
                        start = i + 2
                    }
                }
            }
        }
        return result
    }
}
