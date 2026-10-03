package com.duck.twominute.ai.network

/**
 * The list of models the picker is allowed to offer.
 *
 * This is a STRICT allow-list, not a heuristic: the earlier version guessed which
 * families are free and offered models that a free API key rejects. Only the ids
 * below are ever shown, and any other id the `models` endpoint reports is dropped —
 * including everything the app itself cannot use (embeddings, image, audio, video).
 *
 * To add or remove a model, edit [ALLOWED] only; the rest of the app follows.
 */
object FreeModelCatalog {

    data class Entry(
        val id: String,
        val label: String,
        val note: String,
        val preview: Boolean,
        val rank: Int,
        /** New Habit is bilingual: the same label and note for the English UI. */
        val labelEn: String = label,
        val noteEn: String = note,
    ) {
        fun label(fa: Boolean): String = if (fa) label else labelEn
        fun note(fa: Boolean): String = if (fa) note else noteEn
    }

    /**
     * The model a free key should use: it has the largest free quota of the list,
     * so a long subtitle runs much further before any limit is reached. The other
     * models hit the free ceiling after a few hundred lines.
     */
    const val RECOMMENDED: String = "gemini-3.1-flash-lite"

    /** The only models offered, recommended first. */
    val ALLOWED: List<String> = listOf(
        RECOMMENDED,
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite-preview",
        "gemini-flash-lite-latest",
    )

    private val LABELS = mapOf(
        "gemini-3.8-flash" to "Gemini 3.8 Flash",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash-Lite",
        "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash-Lite (پیش‌نمایش)",
        "gemini-flash-lite-latest" to "Gemini Flash-Lite (آخرین نسخه)",
    )

    private val NOTES = mapOf(
        "gemini-3.8-flash" to "بهترین کیفیت ترجمه؛ اما سهمیه رایگانش کم است و زود به سقف مصرف می‌رسد",
        "gemini-3.5-flash-lite" to "سریع و سبک؛ سهمیه رایگان متوسط",
        "gemini-3.1-flash-lite" to "پیشنهاد ما برای کلید رایگان: بیشترین سهمیه، دیرتر از همه به سقف محدودیت می‌رسد",
        "gemini-3.1-flash-lite-preview" to "نسخه پیش‌نمایش؛ ممکن است بدون اطلاع تغییر کند",
        "gemini-flash-lite-latest" to "همیشه به آخرین نسخه Flash-Lite وصل می‌شود",
    )

    private val LABELS_EN = mapOf(
        "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash-Lite (Preview)",
        "gemini-flash-lite-latest" to "Gemini Flash-Lite (Latest)",
    )

    /** Word-for-word English versions of [NOTES]. */
    private val NOTES_EN = mapOf(
        "gemini-3.8-flash" to "Best translation quality, but its free quota is small and reaches the usage cap quickly",
        "gemini-3.5-flash-lite" to "Fast and light; medium free quota",
        "gemini-3.1-flash-lite" to "Our pick for a free key: the largest quota, the last one to reach the limit",
        "gemini-3.1-flash-lite-preview" to "Preview version; it may change without notice",
        "gemini-flash-lite-latest" to "Always points to the newest Flash-Lite version",
    )

    /** Shown before the first successful refresh, and whenever the list cannot be loaded. */
    val fallback: List<Entry> = ALLOWED.map { entryFor(it) }

    /** Selected after a key is entered and its model list has been loaded. */
    val defaultModel: String = RECOMMENDED

    /**
     * The model to pick from a freshly loaded list: the recommended one when the key
     * has it, otherwise the first offered one.
     */
    fun preferredFrom(entries: List<Entry>): String? =
        entries.firstOrNull { it.id == RECOMMENDED }?.id ?: entries.firstOrNull()?.id

    /** True for the model with the largest free quota. */
    fun isRecommended(id: String): Boolean = normalize(id) == RECOMMENDED

    /**
     * Keeps only the allowed ids from what the API reported.
     *
     * If the key sees none of them, the full allow-list is returned anyway so the
     * user can still choose one — a `models` endpoint that hides a model does not
     * always mean generateContent will refuse it.
     */
    fun filter(models: List<GeminiModelInfo>): List<Entry> {
        val fromApi = models
            .asSequence()
            .filter { it.supportsGenerateContent }
            .map { normalize(it.id) }
            .filter { isFree(it) }
            .distinct()
            .map { entryFor(it) }
            .sortedBy { it.rank }
            .toList()

        return fromApi.ifEmpty { fallback }
    }

    /** True only for the ids in [ALLOWED]. */
    fun isFree(id: String): Boolean = normalize(id) in ALLOWED

    private fun normalize(id: String): String = id.removePrefix("models/").trim().lowercase()

    fun entryFor(id: String): Entry {
        val key = normalize(id)
        return Entry(
            id = key,
            label = LABELS[key] ?: prettify(key),
            note = NOTES[key] ?: "مدل رایگان جمینای",
            preview = key.contains("preview"),
            rank = ALLOWED.indexOf(key).takeIf { it >= 0 } ?: ALLOWED.size,
            labelEn = LABELS_EN[key] ?: LABELS[key] ?: prettify(key),
            noteEn = NOTES_EN[key] ?: "Free Gemini model",
        )
    }

    private fun prettify(id: String): String = id
        .split('-')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

    fun encode(entries: List<Entry>): String = entries.joinToString("\n") { it.id }

    fun decode(raw: String): List<Entry> = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && isFree(it) }
        .map { entryFor(it) }
        .distinctBy { it.id }
        .sortedBy { it.rank }
        .toList()
}
