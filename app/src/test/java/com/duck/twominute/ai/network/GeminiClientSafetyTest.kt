package com.duck.twominute.ai.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The safety-filter behaviour of the client.
 *
 * A film subtitle is full of insults, threats and sexual references. With the
 * default thresholds Gemini refuses whole batches for that, which is why a
 * subtitle that translates fine on aistudio.google.com used to fail in the app.
 * These tests pin down that the filters are switched off in every request and
 * that a refusal is reported as [GeminiErrorKind.SAFETY] so the engine can skip
 * the single offending line instead of dying.
 */
class GeminiClientSafetyTest {

    private val sentBodies = mutableListOf<String>()

    /** Answers requests from a queue and records what was sent. */
    private fun clientReturning(vararg answers: Pair<Int, String>): GeminiClient {
        val queue = ArrayDeque(answers.toList())
        val http = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                sentBodies += buffer.readUtf8()
                val (code, body) = queue.removeFirst()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Error")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()
        return GeminiClient(http)
    }

    private fun generate(client: GeminiClient): String = runBlocking {
        client.generateJson(
            apiKey = "test-key",
            model = "gemini-flash-lite-latest",
            systemInstruction = "translate",
            userPayload = """{"lines_to_translate":[{"id":1,"text":"hello"}]}""",
            temperature = 0.6f,
        )
    }

    private val okAnswer = """
        {"candidates":[{"content":{"parts":[{"text":"[{\"id\":1,\"fa\":\"سلام\"}]"}]},"finishReason":"STOP"}]}
    """.trimIndent()

    @Test
    fun `every request switches the content filters off`() {
        val client = clientReturning(200 to okAnswer)

        val answer = generate(client)

        assertTrue(answer.contains("سلام"))
        assertEquals(1, sentBodies.size)
        val body = sentBodies.single()
        listOf(
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT",
        ).forEach { category ->
            assertTrue("$category is missing from the request", body.contains(category))
        }
        assertEquals(
            "all four categories must be set to OFF",
            4,
            Regex("\"threshold\":\"OFF\"").findAll(body).count(),
        )
        assertTrue(
            "the old restrictive threshold must be gone",
            !body.contains("BLOCK_ONLY_HIGH"),
        )
    }

    @Test
    fun `a model that rejects OFF is retried with BLOCK_NONE`() {
        val rejection = """
            {"error":{"code":400,"message":"Invalid value at 'safety_settings[0].threshold' (type.googleapis.com/google.ai.generativelanguage.v1beta.SafetySetting.HarmBlockThreshold), \"OFF\""}}
        """.trimIndent()
        val client = clientReturning(400 to rejection, 200 to okAnswer)

        val answer = generate(client)

        assertTrue(answer.contains("سلام"))
        assertEquals("the batch must be retried, not dropped", 2, sentBodies.size)
        assertTrue(sentBodies[0].contains("\"threshold\":\"OFF\""))
        assertTrue(sentBodies[1].contains("\"threshold\":\"BLOCK_NONE\""))
    }

    @Test
    fun `a wrong api key is not mistaken for a safety problem`() {
        val rejection = """
            {"error":{"code":400,"message":"API key not valid. Please pass a valid API key."}}
        """.trimIndent()
        val client = clientReturning(400 to rejection)

        try {
            generate(client)
            fail("expected an auth error")
        } catch (e: GeminiException) {
            assertEquals(GeminiErrorKind.AUTH, e.kind)
        }
        assertEquals("a bad key must not trigger the safety fallback", 1, sentBodies.size)
    }

    @Test
    fun `a blocked prompt is reported as a safety error`() {
        val blocked = """
            {"promptFeedback":{"blockReason":"SAFETY"}}
        """.trimIndent()
        val client = clientReturning(200 to blocked)

        try {
            generate(client)
            fail("expected a safety error")
        } catch (e: GeminiException) {
            assertEquals(GeminiErrorKind.SAFETY, e.kind)
        }
    }

    @Test
    fun `an empty answer with PROHIBITED_CONTENT is a safety error`() {
        val blocked = """
            {"candidates":[{"finishReason":"PROHIBITED_CONTENT","content":{"parts":[]}}]}
        """.trimIndent()
        val client = clientReturning(200 to blocked)

        try {
            generate(client)
            fail("expected a safety error")
        } catch (e: GeminiException) {
            assertEquals(GeminiErrorKind.SAFETY, e.kind)
            assertTrue(e.message.orEmpty().contains("PROHIBITED_CONTENT"))
        }
    }

    @Test
    fun `a safety error is never retried as a whole batch`() {
        // The engine bisects instead, so retrying the identical batch would only
        // burn quota and, before the fix, loop forever.
        assertTrue(!GeminiErrorKind.SAFETY.retryable)
    }
}
