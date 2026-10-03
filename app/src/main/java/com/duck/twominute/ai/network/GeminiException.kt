package com.duck.twominute.ai.network

/** Error classes we can react to: retryable ones are retried automatically. */
enum class GeminiErrorKind {
    NO_KEY,
    AUTH,

    /**
     * A short-term rate limit (requests or tokens per minute). It clears by itself
     * within seconds, so the engine waits and carries on.
     */
    QUOTA,

    /**
     * The free quota of this key is used up for the day (or the model has no free
     * quota at all). Waiting a few seconds does not help: the run stops and the user
     * is told in a dialog, with the option to continue on another model.
     */
    QUOTA_DAILY,
    SERVER,
    NETWORK,
    PARSE,
    SAFETY,
    UNKNOWN;

    val retryable: Boolean
        get() = this == QUOTA || this == SERVER || this == NETWORK

    /** True for both kinds of limit: the per-minute one and the daily one. */
    val isQuota: Boolean
        get() = this == QUOTA || this == QUOTA_DAILY
}

class GeminiException(
    val kind: GeminiErrorKind,
    message: String,
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null,
    /** The quota the API reported as exhausted, e.g. `GenerateRequestsPerDayPerProjectPerModel-FreeTier`. */
    val quotaId: String? = null,
) : Exception(message, cause)
