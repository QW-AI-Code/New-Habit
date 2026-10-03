package com.duck.twominute.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.duck.twominute.ai.network.FreeModelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Its own DataStore file, on purpose: the API key must never end up in the JSON
 * backup the user exports and shares, and it is excluded from Android's cloud
 * backup (see res/xml/backup_rules.xml and data_extraction_rules.xml).
 */
private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "new_habit_ai")

data class AiSettings(
    val apiKey: String = "",
    val model: String = FreeModelCatalog.defaultModel,
    val cachedModels: String = "",
    /** The last plan the planner produced, as JSON, so it survives leaving the screen. */
    val lastPlan: String = "",
    val lastGoal: String = "",
    /**
     * True only when [cachedModels] was loaded by a successful connection test
     * made with exactly the current [apiKey]. Until then the app offers no model
     * at all: no key, an untested key or a rejected key means an empty picker.
     */
    val modelsVerified: Boolean = false,
)

class AiSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.aiDataStore

    private object Keys {
        val apiKey = stringPreferencesKey("api_key")
        val model = stringPreferencesKey("model")
        val cachedModels = stringPreferencesKey("cached_models")
        val lastPlan = stringPreferencesKey("last_plan")
        val lastGoal = stringPreferencesKey("last_goal")

        /** Which key the cached model list belongs to (a hash, never the key itself). */
        val modelsKeyFingerprint = stringPreferencesKey("models_key_fingerprint")
    }

    val settings: Flow<AiSettings> = dataStore.data.map { p ->
        val defaults = AiSettings()
        val key = p[Keys.apiKey] ?: defaults.apiKey
        val cached = p[Keys.cachedModels] ?: defaults.cachedModels
        AiSettings(
            apiKey = key,
            // A model saved by an older build may no longer be offered — fall back
            // instead of sending requests that the free key rejects.
            model = p[Keys.model]?.takeIf { FreeModelCatalog.isFree(it) } ?: defaults.model,
            cachedModels = cached,
            lastPlan = p[Keys.lastPlan] ?: defaults.lastPlan,
            lastGoal = p[Keys.lastGoal] ?: defaults.lastGoal,
            modelsVerified = key.isNotBlank() &&
                cached.isNotBlank() &&
                p[Keys.modelsKeyFingerprint] == fingerprint(key),
        )
    }

    /**
     * Saves the key. A different key (or no key) drops the model list that was
     * loaded for the previous one, so models only reappear after this key has
     * passed its own connection test.
     */
    suspend fun setApiKey(value: String) = put {
        val trimmed = value.trim()
        if ((it[Keys.apiKey] ?: "") != trimmed) {
            it.remove(Keys.cachedModels)
            it.remove(Keys.modelsKeyFingerprint)
        }
        if (trimmed.isEmpty()) {
            it.remove(Keys.apiKey)
        } else {
            it[Keys.apiKey] = trimmed
        }
    }

    /** Forgets the loaded models, e.g. after Google rejected the key. */
    suspend fun clearModels() = put {
        it.remove(Keys.cachedModels)
        it.remove(Keys.modelsKeyFingerprint)
    }

    suspend fun setModel(value: String) = put { it[Keys.model] = value }

    suspend fun setLastPlan(goal: String, planJson: String) = put {
        it[Keys.lastGoal] = goal
        it[Keys.lastPlan] = planJson
    }

    suspend fun clearLastPlan() = put {
        it.remove(Keys.lastPlan)
        it.remove(Keys.lastGoal)
    }

    /**
     * Stores a freshly loaded model list. When the list was loaded for a key that
     * was not seen before — the user just entered it — the recommended free model
     * is selected; afterwards a refresh keeps whatever the user picked, unless that
     * model is no longer offered.
     */
    suspend fun applyLoadedModels(apiKey: String, encodedModels: String, preferredModel: String?, offered: Set<String>) = put {
        val fingerprint = fingerprint(apiKey)
        val newKey = it[Keys.modelsKeyFingerprint] != fingerprint
        it[Keys.cachedModels] = encodedModels
        it[Keys.modelsKeyFingerprint] = fingerprint
        val current = it[Keys.model]
        if (preferredModel != null && (newKey || current == null || current !in offered)) {
            it[Keys.model] = preferredModel
        }
    }

    private suspend inline fun put(crossinline block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    companion object {
        internal fun fingerprint(apiKey: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(apiKey.trim().toByteArray(Charsets.UTF_8))
            return digest.take(12).joinToString("") { "%02x".format(it) }
        }
    }
}
