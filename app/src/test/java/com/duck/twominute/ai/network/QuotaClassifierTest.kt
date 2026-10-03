package com.duck.twominute.ai.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * A "429" from Gemini is either a per-minute limit (wait and continue) or the daily
 * free quota (stop and tell the user). Mixing them up either spams the user or
 * retries a dead key for hours.
 */
class QuotaClassifierTest {

    private fun body(quotaId: String, message: String = "You exceeded your current quota.") = """
        {"error":{"code":429,"message":"$message","status":"RESOURCE_EXHAUSTED","details":[
          {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
            {"quotaMetric":"generativelanguage.googleapis.com/generate_content_free_tier_requests","quotaId":"$quotaId","quotaValue":"20"}]},
          {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"37s"}]}}
    """.trimIndent()

    @Test
    fun `a per-day quota id is the daily limit`() {
        val r = QuotaClassifier.classify(body("GenerateRequestsPerDayPerProjectPerModel-FreeTier"), "")
        assertEquals(QuotaClassifier.Scope.DAILY, r.scope)
    }

    @Test
    fun `a per-minute quota id is the short limit`() {
        val r = QuotaClassifier.classify(body("GenerateRequestsPerMinutePerProjectPerModel-FreeTier"), "")
        assertEquals(QuotaClassifier.Scope.MINUTE, r.scope)
    }

    @Test
    fun `a model without free quota counts as daily`() {
        val r = QuotaClassifier.classify("""{"error":{"code":429}}""", "Quota exceeded for metric: x, limit: 0, model: y")
        assertEquals(QuotaClassifier.Scope.DAILY, r.scope)
    }

    @Test
    fun `an unstructured answer defaults to the short limit`() {
        val r = QuotaClassifier.classify("not json", "Resource has been exhausted (e.g. check quota).")
        assertEquals(QuotaClassifier.Scope.MINUTE, r.scope)
    }

    private fun clientAnswering(code: Int, json: String) = GeminiClient(
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Error")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()
    )

    private fun kindOf(client: GeminiClient): GeminiException = runBlocking {
        try {
            client.generateJson("k", "gemini-3.1-flash-lite", "s", "{}", 0.5f)
            fail("expected an exception")
            throw IllegalStateException()
        } catch (e: GeminiException) {
            e
        }
    }

    @Test
    fun `the client reports the daily limit as not retryable`() {
        val e = kindOf(clientAnswering(429, body("GenerateRequestsPerDayPerProjectPerModel-FreeTier")))
        assertEquals(GeminiErrorKind.QUOTA_DAILY, e.kind)
        assertFalse(e.kind.retryable)
        assertTrue(e.kind.isQuota)
    }

    @Test
    fun `the client reports the minute limit as retryable with the server delay`() {
        val e = kindOf(clientAnswering(429, body("GenerateRequestsPerMinutePerProjectPerModel-FreeTier")))
        assertEquals(GeminiErrorKind.QUOTA, e.kind)
        assertTrue(e.kind.retryable)
        assertEquals(37, e.retryAfterSeconds)
    }
}
