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

    val state: Flow<AppState> = dataStore.data.map { preferences -> read(preferences) }

    suspend fun save(value: AppState) {
        dataStore.edit { preferences ->
            preferences[STATE_KEY] = encode(value)
        }
    }

    /**
     * Read-modify-write in ONE DataStore transaction. v1.0.0 read the state and saved
     * it in two separate steps, so two quick taps (or a tap and a notification
     * action) could overwrite each other's change. Returns the state that was written.
     */
    suspend fun update(transform: (AppState) -> AppState): AppState {
        var written = AppState()
        dataStore.edit { preferences ->
            val next = transform(read(preferences))
            preferences[STATE_KEY] = encode(next)
            written = next
        }
        return written
    }

    private fun read(preferences: Preferences): AppState {
        val raw = preferences[STATE_KEY] ?: return AppState()
        return try {
            json.decodeFromString(AppState.serializer(), raw)
        } catch (error: Exception) {
            Log.w("AppStore", "Stored state is unreadable, starting fresh", error)
            AppState()
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
