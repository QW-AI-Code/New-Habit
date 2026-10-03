package com.duck.twominute.ai.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One model as reported by the Gemini REST API. */
data class GeminiModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val supportsGenerateContent: Boolean,
    val inputTokenLimit: Int,
)

/**
 * Minimal Gemini REST client (generativelanguage.googleapis.com, v1beta).
 * Only what the app needs: list models and generate JSON.
 */
class GeminiClient(
    private val client: OkHttpClient = defaultClient(),
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        const val API_KEY_URL = "https://aistudio.google.com/apikey"

        private const val THRESHOLD_OFF = "OFF"
        private const val THRESHOLD_BLOCK_NONE = "BLOCK_NONE"

        /** Enough for a summary, ten characters and twenty glossary entries. */
        private const val ANALYSIS_TOKENS = 2048
        private const val TRANSLATION_TOKENS = 8192

        /** A complete habit plan: phases, several identities, tips and milestones. */
        const val PLAN_TOKENS = 8192

        private val HARM_CATEGORIES = listOf(
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT",
        )

        /**
         * One shared client for the whole app: the connection pool and the HTTP/2
         * connection are reused across all parallel batches, which saves a TLS
         * handshake per request. The dispatcher limits are raised because OkHttp
         * allows only 5 concurrent requests per host by default — that would cap
         * our parallel translation at 5 no matter what the user configures.
         */
        private val shared: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
                .dispatcher(
                    okhttp3.Dispatcher().apply {
                        maxRequests = 16
                        maxRequestsPerHost = 16
                    }
                )
                .build()
        }

        fun defaultClient(): OkHttpClient = shared
    }

    suspend fun listModels(apiKey: String): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw GeminiException(GeminiErrorKind.NO_KEY, "missing api key")

        val models = mutableListOf<GeminiModelInfo>()
        var pageToken: String? = null
        var page = 0
        do {
            val url = buildString {
                append("$BASE/models?pageSize=200")
                if (!pageToken.isNullOrBlank()) append("&pageToken=").append(pageToken)
            }
            val request = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .get()
                .build()

            val body = execute(request)
            val root = json.parseToJsonElement(body).jsonObject
            root["models"]?.jsonArray?.forEach { element ->
                val obj = element.jsonObject
                val rawName = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val methods = obj["supportedGenerationMethods"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                models += GeminiModelInfo(
                    id = rawName.removePrefix("models/"),
                    displayName = obj["displayName"]?.jsonPrimitive?.contentOrNull ?: rawName,
                    description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    supportsGenerateContent = methods.contains("generateContent"),
                    inputTokenLimit = obj["inputTokenLimit"]?.jsonPrimitive?.runCatching { int }?.getOrNull() ?: 0,
                )
            }
            pageToken = root["nextPageToken"]?.jsonPrimitive?.contentOrNull
            page++
        } while (!pageToken.isNullOrBlank() && page < 5)

        models
    }

    /**
     * Sends one translation batch and returns the raw text answer of the model.
     *
     * Safety filters are switched OFF, which is what makes a film subtitle
     * translatable at all: insults, violence and sexual references are normal
     * dialogue in a film, and with the default thresholds Gemini refuses whole
     * batches for them. aistudio.google.com behaves the same way — that is why a
     * subtitle that translates fine on the website used to fail here.
     *
     * "OFF" is the widest setting the API accepts. Older models only know
     * "BLOCK_NONE", so a rejected safety setting is retried once with that value
     * instead of failing the batch.
     */
    suspend fun generateJson(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPayload: String,
        temperature: Float,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw GeminiException(GeminiErrorKind.NO_KEY, "missing api key")

        try {
            generateWithThreshold(apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_OFF)
        } catch (e: GeminiException) {
            if (rejectedSafetySetting(e)) {
                generateWithThreshold(apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_BLOCK_NONE)
            } else {
                throw e
            }
        }
    }

    /**
     * The one request that reads the whole subtitle before translating.
     *
     * Same safety handling as a translation batch — a film summary trips the
     * filters for exactly the same reasons the dialogue does — but a free-form JSON
     * object instead of the id/fa array, a low temperature because this is analysis
     * and not writing, and a small output budget.
     */
    suspend fun generateAnalysis(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPayload: String,
        temperature: Float = 0.25f,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw GeminiException(GeminiErrorKind.NO_KEY, "missing api key")

        try {
            generateWithThreshold(
                apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_OFF,
                responseSchema = null, maxOutputTokens = ANALYSIS_TOKENS,
            )
        } catch (e: GeminiException) {
            if (rejectedSafetySetting(e)) {
                generateWithThreshold(
                    apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_BLOCK_NONE,
                    responseSchema = null, maxOutputTokens = ANALYSIS_TOKENS,
                )
            } else {
                throw e
            }
        }
    }

    /**
     * New Habit: one structured answer that must follow [responseSchema].
     *
     * Exactly the same request pipeline as the subtitle calls above — same safety
     * handling with the OFF → BLOCK_NONE fallback, same thinking budget per model,
     * same error classification and usage reporting — only with the caller's schema
     * and a larger output budget, because a full habit plan is longer than a batch.
     */
    suspend fun generateStructured(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPayload: String,
        responseSchema: JsonObject,
        temperature: Float = 0.6f,
        maxOutputTokens: Int = PLAN_TOKENS,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw GeminiException(GeminiErrorKind.NO_KEY, "missing api key")

        try {
            generateWithThreshold(
                apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_OFF,
                responseSchema = responseSchema, maxOutputTokens = maxOutputTokens,
            )
        } catch (e: GeminiException) {
            if (rejectedSafetySetting(e)) {
                generateWithThreshold(
                    apiKey, model, systemInstruction, userPayload, temperature, THRESHOLD_BLOCK_NONE,
                    responseSchema = responseSchema, maxOutputTokens = maxOutputTokens,
                )
            } else {
                throw e
            }
        }
    }

    /** True when the API refused the safety setting itself, not the content. */
    private fun rejectedSafetySetting(e: GeminiException): Boolean {
        if (e.kind != GeminiErrorKind.UNKNOWN) return false
        val message = e.message.orEmpty()
        return message.contains("safetySettings", ignoreCase = true) ||
            message.contains("threshold", ignoreCase = true) ||
            (message.contains("OFF", ignoreCase = false) && message.contains("Invalid", ignoreCase = true))
    }

    private fun generateWithThreshold(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPayload: String,
        temperature: Float,
        threshold: String,
        responseSchema: JsonObject? = translationSchema(),
        maxOutputTokens: Int = TRANSLATION_TOKENS,
    ): String {
        val requestJson = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray { add(buildJsonObject { put("text", systemInstruction) }) })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", userPayload) }) })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", temperature)
                put("topP", 0.95)
                put("maxOutputTokens", maxOutputTokens)
                put("responseMimeType", "application/json")
                responseSchema?.let { put("responseSchema", it) }
                thinkingBudgetFor(model)?.let { budget ->
                    put("thinkingConfig", buildJsonObject { put("thinkingBudget", budget) })
                }
            })
            put("safetySettings", buildJsonArray {
                HARM_CATEGORIES.forEach { category ->
                    add(buildJsonObject {
                        put("category", category)
                        put("threshold", threshold)
                    })
                }
            })
        }

        val request = Request.Builder()
            .url("$BASE/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(mediaType))
            .build()

        val body = execute(request, model)
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw GeminiException(GeminiErrorKind.PARSE, "unreadable response")
        // Recorded before the text is checked: a refused or truncated answer has
        // still used up tokens and a request of the quota.
        reportUsage(model, root)
        return extractText(root)
    }

    private fun reportUsage(model: String, root: JsonObject) {
        val listener = GeminiUsageHub.listener ?: return
        val sample = GeminiUsageParser.usage(model, root) ?: return
        runCatching { listener.onUsage(sample) }
    }

    /** The id/fa array every translation batch must answer with. */
    private fun translationSchema(): JsonObject = buildJsonObject {
        put("type", "ARRAY")
        put("items", buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("id", buildJsonObject { put("type", "INTEGER") })
                put("fa", buildJsonObject { put("type", "STRING") })
            })
            put("required", buildJsonArray { add("id"); add("fa") })
        })
    }

    /**
     * 2.5 models think by default, which slows subtitle work down and can eat the
     * whole output budget. Flash models get thinking disabled, Pro gets the minimum.
     */
    private fun thinkingBudgetFor(model: String): Int? = when {
        model.startsWith("gemini-2.5-pro") -> 128
        model.startsWith("gemini-2.5") -> 0
        else -> null
    }

    /** @param model set for generateContent calls, so a rejection can be attributed to it. */
    private fun execute(request: Request, model: String? = null): String {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw GeminiException(GeminiErrorKind.NETWORK, e.message ?: "network error", cause = e)
        }

        response.use { res ->
            val body = res.body?.string().orEmpty()
            if (res.isSuccessful) return body

            val message = errorMessage(body).ifBlank { "HTTP ${res.code}" }
            val exhausted = res.code == 429 ||
                (res.code == 403 && body.contains("RESOURCE_EXHAUSTED"))
            if (exhausted) {
                // Per-minute limit or the daily free quota? They need opposite
                // reactions, see [QuotaClassifier].
                val quota = QuotaClassifier.classify(body, message)
                if (model != null) {
                    GeminiUsageHub.listener?.let { listener ->
                        runCatching { listener.onQuotaRejected(model, GeminiUsageParser.quotaLimits(model, body)) }
                    }
                }
                throw GeminiException(
                    kind = if (quota.scope == QuotaClassifier.Scope.DAILY) {
                        GeminiErrorKind.QUOTA_DAILY
                    } else {
                        GeminiErrorKind.QUOTA
                    },
                    message = message,
                    retryAfterSeconds = retryDelaySeconds(body, res),
                    quotaId = quota.quotaId,
                )
            }
            val kind = when {
                res.code == 400 && message.contains("API key", ignoreCase = true) -> GeminiErrorKind.AUTH
                res.code == 400 -> GeminiErrorKind.UNKNOWN
                res.code == 401 || res.code == 403 -> GeminiErrorKind.AUTH
                res.code == 404 -> GeminiErrorKind.UNKNOWN
                res.code == 429 -> GeminiErrorKind.QUOTA
                res.code in 500..599 -> GeminiErrorKind.SERVER
                else -> GeminiErrorKind.UNKNOWN
            }
            throw GeminiException(kind, message, retryAfterSeconds = retryDelaySeconds(body, res))
        }
    }

    private fun errorMessage(body: String): String = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
            ?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
    }.getOrDefault("")

    private fun retryDelaySeconds(body: String, response: okhttp3.Response): Int? {
        response.header("Retry-After")?.toIntOrNull()?.let { return it }
        val match = Regex(""""retryDelay"\s*:\s*"(\d+)(?:\.\d+)?s"""").find(body)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractText(root: JsonObject): String {
        root["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull?.let { reason ->
            throw GeminiException(GeminiErrorKind.SAFETY, "blocked: $reason")
        }

        val candidates = root["candidates"] as? JsonArray
        val candidate = candidates?.firstOrNull()?.jsonObject
            ?: throw GeminiException(GeminiErrorKind.PARSE, "no candidate returned")

        val finishReason = candidate["finishReason"]?.jsonPrimitive?.contentOrNull
        val parts = candidate["content"]?.jsonObject?.get("parts") as? JsonArray
        val text = parts?.mapNotNull { part ->
            (part as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull
        }?.joinToString("").orEmpty()

        if (text.isBlank()) {
            val kind = when (finishReason) {
                "SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST" -> GeminiErrorKind.SAFETY
                "MAX_TOKENS" -> GeminiErrorKind.PARSE
                else -> GeminiErrorKind.PARSE
            }
            throw GeminiException(kind, "empty answer (finishReason=$finishReason)")
        }
        return text
    }
}
