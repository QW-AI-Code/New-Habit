package com.duck.twominute.ai.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tells the two kinds of "429 / RESOURCE_EXHAUSTED" apart.
 *
 * Gemini answers every exceeded limit with the same status code, but for the user
 * they are completely different situations:
 *
 *  * **per minute** (requests or input tokens per minute): clears within seconds.
 *    The engine slows down, waits the time the server asks for, and continues.
 *  * **per day** (requests per day on the free tier), or a model whose free limit
 *    is `0`: nothing clears before the daily reset. Retrying only burns time, so
 *    the run stops and the user gets a dialog that explains it and offers another
 *    model — the free quota is counted per model.
 *
 * The decision is taken from the structured `QuotaFailure` details of the error
 * body, with the message text as a fallback for answers that carry no details.
 */
object QuotaClassifier {

    enum class Scope { MINUTE, DAILY }

    data class Result(val scope: Scope, val quotaId: String?)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val DAILY_MARKERS = listOf("perday", "per day", "per_day", "daily", "requestsperday")
    private val MINUTE_MARKERS = listOf("perminute", "per minute", "per_minute")

    /** Classifies the body of a 429 answer. Never throws. */
    fun classify(body: String, message: String): Result {
        val quotaIds = runCatching { quotaIds(body) }.getOrDefault(emptyList())

        // A structured answer is the most reliable source.
        quotaIds.firstOrNull { id -> DAILY_MARKERS.any { id.lowercase().contains(it) } }?.let {
            return Result(Scope.DAILY, it)
        }
        quotaIds.firstOrNull { id -> MINUTE_MARKERS.any { id.lowercase().contains(it) } }?.let {
            return Result(Scope.MINUTE, it)
        }

        val text = message.lowercase()
        // "limit: 0" = this model has no free quota on this key at all; waiting
        // for the next minute can never help.
        if (Regex("""limit:\s*0\b""").containsMatchIn(text)) {
            return Result(Scope.DAILY, quotaIds.firstOrNull())
        }
        if (DAILY_MARKERS.any { text.contains(it) }) return Result(Scope.DAILY, quotaIds.firstOrNull())
        return Result(Scope.MINUTE, quotaIds.firstOrNull())
    }

    private fun quotaIds(body: String): List<String> {
        val error = json.parseToJsonElement(body).jsonObject["error"] as? JsonObject ?: return emptyList()
        val details = error["details"] as? JsonArray ?: return emptyList()
        val ids = mutableListOf<String>()
        details.forEach { detail ->
            val violations = (detail as? JsonObject)?.get("violations") as? JsonArray ?: return@forEach
            violations.forEach { violation ->
                val obj = violation as? JsonObject ?: return@forEach
                obj["quotaId"]?.jsonPrimitive?.contentOrNull?.let { ids += it }
                obj["quotaMetric"]?.jsonPrimitive?.contentOrNull?.let { ids += it }
            }
        }
        return ids
    }
}
