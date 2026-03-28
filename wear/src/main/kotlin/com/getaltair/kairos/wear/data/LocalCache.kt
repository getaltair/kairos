package com.getaltair.kairos.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.domain.wear.WearRoutineData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.localCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wear_local_cache",
)

/**
 * DataStore-backed reactive cache of habit, completion, and routine data received
 * from the phone via the Data Layer. Exposes Flow-based accessors so the watch UI
 * recomposes when data changes.
 */
class LocalCache(private val context: Context) {
    private val habitsKey = stringPreferencesKey("habits_json")
    private val completionsKey = stringPreferencesKey("completions_json")
    private val routineKey = stringPreferencesKey("routine_json")

    val habits: Flow<List<WearHabitData>> = context.localCacheDataStore.data
        .map { prefs ->
            val parsed = WearHabitData.listFromJson(prefs[habitsKey] ?: "[]")
            if (parsed.isEmpty() && prefs[habitsKey] != null && prefs[habitsKey] != "[]") {
                Timber.w("LocalCache: failed to parse cached habits data, returning empty")
            }
            parsed
        }

    val completions: Flow<List<WearCompletionData>> = context.localCacheDataStore.data
        .map { prefs ->
            val parsed = WearCompletionData.listFromJson(prefs[completionsKey] ?: "[]")
            if (parsed.isEmpty() && prefs[completionsKey] != null && prefs[completionsKey] != "[]") {
                Timber.w("LocalCache: failed to parse cached completions data, returning empty")
            }
            parsed
        }

    val activeRoutine: Flow<WearRoutineData?> = context.localCacheDataStore.data
        .map { prefs ->
            val json = prefs[routineKey]
            if (json != null) {
                val parsed = WearRoutineData.fromJson(json)
                if (parsed == null) {
                    Timber.w("LocalCache: failed to parse cached routines data, returning empty")
                }
                parsed
            } else {
                null
            }
        }

    suspend fun updateHabits(json: String) {
        context.localCacheDataStore.edit { it[habitsKey] = json }
    }

    suspend fun updateCompletions(json: String) {
        context.localCacheDataStore.edit { it[completionsKey] = json }
    }

    suspend fun updateRoutineState(json: String?) {
        context.localCacheDataStore.edit {
            if (json != null) it[routineKey] = json else it.remove(routineKey)
        }
    }
}
