package com.duck.twominute

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.stateDataStore: DataStore<Preferences> by preferencesDataStore(name = "two_minute_state")

class AppStore(context: Context) {

    private val dataStore = context.applicationContext.stateDataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val state: Flow<AppState> = dataStore.data.map { preferences ->
        val raw = preferences[STATE_KEY] ?: return@map AppState()
        try {
            json.decodeFromString(AppState.serializer(), raw)
        } catch (error: Exception) {
            Log.w("AppStore", "Stored state is unreadable, starting fresh", error)
            AppState()
        }
    }

    suspend fun save(value: AppState) {
        dataStore.edit { preferences ->
            preferences[STATE_KEY] = encode(value)
        }
    }

    /** Used by the backup export. */
    fun encode(value: AppState): String = json.encodeToString(AppState.serializer(), value)

    /** Used by the backup import. Returns null when the payload is not ours. */
    fun decode(raw: String): AppState? =
        runCatching { json.decodeFromString(AppState.serializer(), raw) }.getOrNull()

    private companion object {
        val STATE_KEY = stringPreferencesKey("state")
    }
}
