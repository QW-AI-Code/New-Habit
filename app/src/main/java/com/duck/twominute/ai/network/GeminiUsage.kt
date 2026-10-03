package com.duck.twominute.ai.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * What one generateContent answer cost, exactly as Google reported it in the
 * `usageMetadata` block of the response. Nothing here is estimated: these are the
 * numbers Gemini itself bills against the key's quota.
 */
data class UsageSample(
    val model: String,
    val promptTokens: Long,
    val outputTokens: Long,
    val thoughtTokens: Long,
    val totalTokens: Long,
    val atMs: Long = System.currentTimeMillis(),
)

/** The kinds of free-tier limit Gemini reports in a `QuotaFailure`. */
enum class LimitKind {
    REQUESTS_PER_DAY,
    REQUESTS_PER_MINUTE,
    INPUT_TOKENS_PER_MINUTE,
    INPUT_TOKENS_PER_DAY,
}

/** One limit value as Google stated it in a 429 answer (`quotaValue`). */
data class QuotaLimitSample(
    val model: String,
    val kind: LimitKind,
    val value: Long,
)

/** Receives the usage of every request the app sends. */
interface GeminiUsageListener {
    fun onUsage(sample: UsageSample)

    /** A request was refused for a limit; [limits] are the values Google named. */
    fun onQuotaRejected(model: String, limits: List<QuotaLimitSample>)
}

/**
 * The one place the network layer reports usage to. Installed by the Application,
 * left empty in unit tests — the client works identically either way.
 */
object GeminiUsageHub {
    @Volatile
    var listener: GeminiUsageListener? = null
}

/** Reads usage and limits out of Gemini answers. Pure, never throws. */
object GeminiUsageParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** `usageMetadata` of a successful (or content-blocked) answer, or null if absent. */
    fun usage(model: String, root: JsonObject, nowMs: Long = System.currentTimeMillis()): UsageSample? {
        val meta = root["usageMetadata"] as? JsonObject ?: return null
        fun num(key: String): Long = (meta[key] as? JsonPrimitive)?.longOrNull ?: 0L
        val prompt = num("promptTokenCount")
        val output = num("candidatesTokenCount")
        val thoughts = num("thoughtsTokenCount")
        val tool = num("toolUsePromptTokenCount")
        val total = num("totalTokenCount").takeIf { it > 0 } ?: (prompt + output + thoughts + tool)
        if (prompt == 0L && output == 0L && total == 0L) return null
        return UsageSample(
            model = normalize(model),
            promptTokens = prompt + tool,
            outputTokens = output,
            thoughtTokens = thoughts,
            totalTokens = total,
            atMs = nowMs,
        )
    }

    /**
     * The limits named in the `QuotaFailure` details of a 429 body, for example
     * `GenerateRequestsPerDayPerProjectPerModel-FreeTier` with `quotaValue: "250"`.
     * The model is taken from `quotaDimensions.model` when Google sends it.
     */
    fun quotaLimits(fallbackModel: String, body: String): List<QuotaLimitSample> = runCatching {
        val error = json.parseToJsonElement(body).jsonObject["error"] as? JsonObject ?: return emptyList()
        val details = error["details"] as? JsonArray ?: return emptyList()
        val out = mutableListOf<QuotaLimitSample>()
        details.forEach { detail ->
            val violations = (detail as? JsonObject)?.get("violations") as? JsonArray ?: return@forEach
            violations.forEach violation@{ v ->
                val obj = v as? JsonObject ?: return@violation
                val id = (obj["quotaId"] as? JsonPrimitive)?.contentOrNull
                    ?: (obj["quotaMetric"] as? JsonPrimitive)?.contentOrNull
                    ?: return@violation
                val kind = kindOf(id) ?: return@violation
                val value = (obj["quotaValue"] as? JsonPrimitive)?.contentOrNull?.trim()?.toLongOrNull()
                    ?: return@violation
                if (value <= 0L) return@violation
                val dimensions = obj["quotaDimensions"] as? JsonObject
                val model = (dimensions?.get("model") as? JsonPrimitive)?.contentOrNull ?: fallbackModel
                out += QuotaLimitSample(normalize(model), kind, value)
            }
        }
        out
    }.getOrDefault(emptyList())

    fun kindOf(quotaId: String): LimitKind? {
        val id = quotaId.lowercase()
        val tokens = id.contains("token")
        val minute = id.contains("perminute") || id.contains("per_minute")
        val day = id.contains("perday") || id.contains("per_day")
        return when {
            tokens && minute -> LimitKind.INPUT_TOKENS_PER_MINUTE
            tokens && day -> LimitKind.INPUT_TOKENS_PER_DAY
            minute -> LimitKind.REQUESTS_PER_MINUTE
            day -> LimitKind.REQUESTS_PER_DAY
            else -> null
        }
    }

    private fun normalize(model: String): String = model.removePrefix("models/").trim().lowercase()
}
