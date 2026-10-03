package com.duck.twominute.ai.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The usage numbers and limits are read from Google's own answers; these pin the format down. */
class GeminiUsageParserTest {

    private fun root(text: String) = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `usage metadata of an answer is read exactly`() {
        val body = """
            {"candidates":[],"usageMetadata":{"promptTokenCount":1200,"candidatesTokenCount":340,
             "thoughtsTokenCount":20,"totalTokenCount":1560}}
        """.trimIndent()

        val sample = GeminiUsageParser.usage("models/Gemini-3.1-Flash-Lite", root(body), nowMs = 5L)!!

        assertEquals("gemini-3.1-flash-lite", sample.model)
        assertEquals(1200L, sample.promptTokens)
        assertEquals(340L, sample.outputTokens)
        assertEquals(20L, sample.thoughtTokens)
        assertEquals(1560L, sample.totalTokens)
    }

    @Test
    fun `an answer without usage metadata reports nothing`() {
        assertNull(GeminiUsageParser.usage("m", root("""{"candidates":[]}""")))
    }

    @Test
    fun `limits are taken from the quota failure of a 429`() {
        val body = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier",
                 "quotaDimensions":{"model":"gemini-3.8-flash","location":"global"},"quotaValue":"250"},
                {"quotaId":"GenerateContentInputTokensPerModelPerMinute-FreeTier","quotaValue":"250000"}
              ]}]}}
        """.trimIndent()

        val limits = GeminiUsageParser.quotaLimits("gemini-3.1-flash-lite", body)

        assertEquals(2, limits.size)
        assertTrue(limits.contains(QuotaLimitSample("gemini-3.8-flash", LimitKind.REQUESTS_PER_DAY, 250)))
        assertTrue(limits.contains(QuotaLimitSample("gemini-3.1-flash-lite", LimitKind.INPUT_TOKENS_PER_MINUTE, 250_000)))
    }

    @Test
    fun `an unreadable body yields no limits`() {
        assertTrue(GeminiUsageParser.quotaLimits("m", "not json").isEmpty())
    }
}
