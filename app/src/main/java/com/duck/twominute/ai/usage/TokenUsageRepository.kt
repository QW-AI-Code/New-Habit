package com.duck.twominute.ai.usage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.duck.twominute.ai.network.GeminiUsageListener
import com.duck.twominute.ai.network.LimitKind
import com.duck.twominute.ai.network.QuotaLimitSample
import com.duck.twominute.ai.network.UsageSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

private val Context.usageStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_token_usage")

/** What one model used on the current quota day. */
data class ModelUsage(
    val requests: Long = 0,
    val promptTokens: Long = 0,
    val outputTokens: Long = 0,
    val thoughtTokens: Long = 0,
    val totalTokens: Long = 0,
    val lastUsedAt: Long = 0,
)

/**
 * The limits of one model: [learned] ones are the values Google itself named when
 * it refused a request, [manual] ones were entered by the user and win.
 */
data class ModelLimits(
    val learned: Map<LimitKind, Long> = emptyMap(),
    val manual: Map<LimitKind, Long> = emptyMap(),
) {
    fun effective(kind: LimitKind): Long? = manual[kind] ?: learned[kind]
    fun isManual(kind: LimitKind): Boolean = manual.containsKey(kind)
}

/** Everything stored, already rolled over to the current quota day. */
data class UsageSnapshot(
    val day: String,
    val usage: Map<String, ModelUsage>,
    val limits: Map<String, ModelLimits>,
)

/**
 * Keeps count of the Gemini tokens and requests this app used.
 *
 * Google offers no endpoint that reports the remaining free quota of an API key,
 * so the counts come from the only exact source there is: the `usageMetadata`
 * that Gemini returns with every answer. The limits come from Google as well — a
 * refused request names the limit it hit and its value — or from the user.
 *
 * Daily totals are persisted (they must survive the worker and the app being
 * restarted); the per-minute window lives in memory, because a minute is over long
 * before a process restart would matter.
 */
class TokenUsageRepository private constructor(context: Context) : GeminiUsageListener {

    private val store = context.applicationContext.usageStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private object Keys {
        val day = stringPreferencesKey("day")
        val usage = stringPreferencesKey("usage")
        val limits = stringPreferencesKey("limits")
    }

    private data class Recent(val atMs: Long, val promptTokens: Long)

    /** Requests of the last minute per model; guarded by `synchronized(recent)`. */
    private val recent = HashMap<String, ArrayDeque<Recent>>()

    private val _version = MutableStateFlow(0L)

    /** Bumped on every recorded request, so screens refresh the minute window at once. */
    val version: StateFlow<Long> = _version.asStateFlow()

    val snapshot: Flow<UsageSnapshot> = store.data
        .map { p ->
            val today = QuotaClock.dayKey()
            val storedDay = p[Keys.day]
            UsageSnapshot(
                day = today,
                // A new quota day starts from zero without waiting for the next write.
                usage = if (storedDay == today) decodeUsage(p[Keys.usage]) else emptyMap(),
                limits = decodeLimits(p[Keys.limits]),
            )
        }
        .distinctUntilChanged()

    // ---- GeminiUsageListener (called on the network thread) -------------

    override fun onUsage(sample: UsageSample) {
        synchronized(recent) {
            val queue = recent.getOrPut(sample.model) { ArrayDeque() }
            queue.addLast(Recent(sample.atMs, sample.promptTokens))
            prune(queue, sample.atMs)
        }
        _version.update { it + 1 }
        scope.launch {
            runCatching {
                store.edit { p ->
                    val today = QuotaClock.dayKey(sample.atMs)
                    val current = if (p[Keys.day] == today) decodeUsage(p[Keys.usage]) else emptyMap()
                    val before = current[sample.model] ?: ModelUsage()
                    val after = before.copy(
                        requests = before.requests + 1,
                        promptTokens = before.promptTokens + sample.promptTokens,
                        outputTokens = before.outputTokens + sample.outputTokens,
                        thoughtTokens = before.thoughtTokens + sample.thoughtTokens,
                        totalTokens = before.totalTokens + sample.totalTokens,
                        lastUsedAt = sample.atMs,
                    )
                    p[Keys.day] = today
                    p[Keys.usage] = encodeUsage(current + (sample.model to after))
                }
            }
        }
    }

    override fun onQuotaRejected(model: String, limits: List<QuotaLimitSample>) {
        if (limits.isEmpty()) return
        scope.launch {
            runCatching {
                store.edit { p ->
                    val all = decodeLimits(p[Keys.limits]).toMutableMap()
                    limits.groupBy { it.model }.forEach { (id, samples) ->
                        val before = all[id] ?: ModelLimits()
                        all[id] = before.copy(learned = before.learned + samples.associate { it.kind to it.value })
                    }
                    p[Keys.limits] = encodeLimits(all)
                }
            }
        }
    }

    // ---- reads and user actions -------------------------------------------

    /** Requests and input tokens of [model] in the last 60 seconds. */
    fun lastMinute(model: String, nowMs: Long = System.currentTimeMillis()): Pair<Int, Long> =
        synchronized(recent) {
            val queue = recent[model] ?: return@synchronized 0 to 0L
            prune(queue, nowMs)
            queue.size to queue.sumOf { it.promptTokens }
        }

    /** Stores the user's own limits; null clears a value and falls back to Google's. */
    suspend fun setManualLimits(model: String, requestsPerDay: Long?, tokensPerMinute: Long?) {
        store.edit { p ->
            val all = decodeLimits(p[Keys.limits]).toMutableMap()
            val before = all[model] ?: ModelLimits()
            val manual = before.manual.toMutableMap()
            if (requestsPerDay != null && requestsPerDay > 0) {
                manual[LimitKind.REQUESTS_PER_DAY] = requestsPerDay
            } else {
                manual.remove(LimitKind.REQUESTS_PER_DAY)
            }
            if (tokensPerMinute != null && tokensPerMinute > 0) {
                manual[LimitKind.INPUT_TOKENS_PER_MINUTE] = tokensPerMinute
            } else {
                manual.remove(LimitKind.INPUT_TOKENS_PER_MINUTE)
            }
            all[model] = before.copy(manual = manual)
            p[Keys.limits] = encodeLimits(all)
        }
    }

    /** Forgets today's counters (the limits are kept). */
    suspend fun resetUsage() {
        synchronized(recent) { recent.clear() }
        store.edit { p ->
            p[Keys.day] = QuotaClock.dayKey()
            p[Keys.usage] = "{}"
        }
        _version.update { it + 1 }
    }

    private fun prune(queue: ArrayDeque<Recent>, nowMs: Long) {
        while (queue.isNotEmpty() && nowMs - queue.first().atMs > MINUTE_MS) queue.removeFirst()
    }

    // ---- storage format: small hand-written JSON, no reflection for R8 ---

    private fun decodeUsage(raw: String?): Map<String, ModelUsage> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        return root.mapNotNull { (id, element) ->
            val o = element as? JsonObject ?: return@mapNotNull null
            id to ModelUsage(
                requests = o.long("r"),
                promptTokens = o.long("p"),
                outputTokens = o.long("o"),
                thoughtTokens = o.long("th"),
                totalTokens = o.long("t"),
                lastUsedAt = o.long("at"),
            )
        }.toMap()
    }

    private fun encodeUsage(map: Map<String, ModelUsage>): String = buildJsonObject {
        map.forEach { (id, u) ->
            put(id, buildJsonObject {
                put("r", u.requests)
                put("p", u.promptTokens)
                put("o", u.outputTokens)
                put("th", u.thoughtTokens)
                put("t", u.totalTokens)
                put("at", u.lastUsedAt)
            })
        }
    }.toString()

    private fun decodeLimits(raw: String?): Map<String, ModelLimits> {
        if (raw.isNullOrBlank()) return emptyMap()
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        return root.mapNotNull { (id, element) ->
            val o = element as? JsonObject ?: return@mapNotNull null
            id to ModelLimits(
                learned = decodeKinds(o["learned"] as? JsonObject),
                manual = decodeKinds(o["manual"] as? JsonObject),
            )
        }.toMap()
    }

    private fun decodeKinds(o: JsonObject?): Map<LimitKind, Long> {
        if (o == null) return emptyMap()
        return o.mapNotNull { (name, value) ->
            val kind = runCatching { LimitKind.valueOf(name) }.getOrNull() ?: return@mapNotNull null
            val v = (value as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
            kind to v
        }.toMap()
    }

    private fun encodeLimits(map: Map<String, ModelLimits>): String = buildJsonObject {
        map.forEach { (id, l) ->
            put(id, buildJsonObject {
                put("learned", buildJsonObject { l.learned.forEach { (k, v) -> put(k.name, v) } })
                put("manual", buildJsonObject { l.manual.forEach { (k, v) -> put(k.name, v) } })
            })
        }
    }.toString()

    private fun JsonObject.long(key: String): Long = (this[key] as? JsonPrimitive)?.longOrNull ?: 0L

    companion object {
        private const val MINUTE_MS = 60_000L

        @Volatile
        private var instance: TokenUsageRepository? = null

        fun get(context: Context): TokenUsageRepository =
            instance ?: synchronized(this) {
                instance ?: TokenUsageRepository(context.applicationContext).also { instance = it }
            }
    }
}
