package com.duck.twominute.ai.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker must only ever offer the five models that work on a free key.
 * Anything else the API reports has to be dropped.
 */
class FreeModelCatalogTest {

    private fun model(id: String, generate: Boolean = true) = GeminiModelInfo(
        id = id,
        displayName = id,
        description = "",
        supportsGenerateContent = generate,
        inputTokenLimit = 1_000_000,
    )

    @Test
    fun `accepts exactly the five allowed models`() {
        listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-flash-lite-latest",
        ).forEach { assertTrue("$it must be offered", FreeModelCatalog.isFree(it)) }

        assertEquals(5, FreeModelCatalog.ALLOWED.size)
    }

    @Test
    fun `rejects the old families that a free key refuses`() {
        listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-3-pro-preview",
        ).forEach { assertFalse("$it must not be offered", FreeModelCatalog.isFree(it)) }
    }

    @Test
    fun `rejects everything that cannot translate text`() {
        listOf(
            "text-embedding-004",
            "gemini-embedding-001",
            "imagen-3.0-generate-002",
            "veo-2.0-generate-001",
            "gemini-2.5-flash-image",
            "gemini-2.5-flash-preview-tts",
            "gemini-2.0-flash-live-001",
            "learnlm-2.0-flash-experimental",
            "aqa",
        ).forEach { assertFalse("$it must not be offered", FreeModelCatalog.isFree(it)) }
    }

    @Test
    fun `the models prefix and stray case are tolerated`() {
        assertTrue(FreeModelCatalog.isFree("models/gemini-3.8-flash"))
        assertTrue(FreeModelCatalog.isFree("  Gemini-3.8-Flash  "))
        assertEquals("gemini-3.8-flash", FreeModelCatalog.entryFor("models/GEMINI-3.8-FLASH").id)
    }

    @Test
    fun `filter drops models without generateContent`() {
        val result = FreeModelCatalog.filter(
            listOf(
                model("gemini-3.8-flash"),
                model("gemini-3.5-flash-lite", generate = false),
            )
        )
        assertEquals(listOf("gemini-3.8-flash"), result.map { it.id })
    }

    @Test
    fun `filter keeps the allow-list order and deduplicates`() {
        val result = FreeModelCatalog.filter(
            listOf(
                model("gemini-flash-lite-latest"),
                model("gemini-3.1-flash-lite"),
                model("gemini-3.8-flash"),
                model("models/gemini-3.8-flash"),
                model("text-embedding-004"),
                model("gemini-2.5-flash"),
            )
        )

        assertEquals(
            listOf("gemini-3.1-flash-lite", "gemini-3.8-flash", "gemini-flash-lite-latest"),
            result.map { it.id },
        )
    }

    @Test
    fun `an api answer without any allowed model falls back to the full list`() {
        val result = FreeModelCatalog.filter(listOf(model("gemini-2.5-flash"), model("aqa")))
        assertEquals(FreeModelCatalog.ALLOWED, result.map { it.id })
    }

    @Test
    fun `preview models are marked`() {
        assertTrue(FreeModelCatalog.entryFor("gemini-3.1-flash-lite-preview").preview)
        assertFalse(FreeModelCatalog.entryFor("gemini-3.8-flash").preview)
    }

    @Test
    fun `every allowed model has a readable persian label and hint`() {
        FreeModelCatalog.ALLOWED.forEach { id ->
            val entry = FreeModelCatalog.entryFor(id)
            assertTrue(entry.label.isNotBlank())
            assertTrue(entry.note.isNotBlank())
        }
    }

    @Test
    fun `encode and decode survive a round trip`() {
        val entries = FreeModelCatalog.fallback
        assertEquals(entries.map { it.id }, FreeModelCatalog.decode(FreeModelCatalog.encode(entries)).map { it.id })
    }

    @Test
    fun `decode ignores junk and models that are no longer allowed`() {
        val restored = FreeModelCatalog.decode("gemini-3.8-flash\n\n  \ngemini-2.5-pro\ntext-embedding-004\n")
        assertEquals(listOf("gemini-3.8-flash"), restored.map { it.id })
    }

    @Test
    fun `the free-key friendly flash-lite is the default and is preferred after a refresh`() {
        assertEquals("gemini-3.1-flash-lite", FreeModelCatalog.defaultModel)
        assertTrue(FreeModelCatalog.isRecommended("models/gemini-3.1-flash-lite"))
        val loaded = FreeModelCatalog.filter(listOf(model("gemini-3.8-flash"), model("gemini-3.1-flash-lite")))
        assertEquals("gemini-3.1-flash-lite", FreeModelCatalog.preferredFrom(loaded))
        val withoutIt = FreeModelCatalog.filter(listOf(model("gemini-3.8-flash")))
        assertEquals("gemini-3.8-flash", FreeModelCatalog.preferredFrom(withoutIt))
    }

    @Test
    fun `the default model is part of the allow-list`() {
        assertTrue(FreeModelCatalog.isFree(FreeModelCatalog.defaultModel))
        assertEquals(FreeModelCatalog.ALLOWED.first(), FreeModelCatalog.defaultModel)
        assertEquals(FreeModelCatalog.ALLOWED, FreeModelCatalog.fallback.map { it.id })
    }
}
