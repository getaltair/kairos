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

private val Context.localCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wear_local_cache",
)

class LocalCache(private val context: Context) {
    private val habitsKey = stringPreferencesKey("habits_json")
    private val completionsKey = stringPreferencesKey("completions_json")
    private val routineKey = stringPreferencesKey("routine_json")

    val habits: Flow<List<WearHabitData>> = context.localCacheDataStore.data
        .map { prefs -> WearHabitData.listFromJson(prefs[habitsKey] ?: "[]") }

    val completions: Flow<List<WearCompletionData>> = context.localCacheDataStore.data
        .map { prefs -> WearCompletionData.listFromJson(prefs[completionsKey] ?: "[]") }

    val activeRoutine: Flow<WearRoutineData?> = context.localCacheDataStore.data
        .map { prefs ->
            prefs[routineKey]?.let { WearRoutineData.fromJson(it) }
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
