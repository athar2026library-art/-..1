package com.example.data

data class SavedSupplication(
    val text: String,
    val source: String
)

/** Parses a source-labelled dua from the assistant and stores it without the explanatory prose. */
object AssistantSupplicationParser {
    fun parse(reply: String): SavedSupplication? {
        val textLabel = listOf("الذكر:", "الدعاء:")
            .mapNotNull { label ->
                val index = reply.indexOf(label)
                if (index >= 0) (index + label.length) else null
            }
            .minOrNull() ?: return null

        val endLabelCandidates = listOf("فضله:", "الفضل:", "المصدر:")
            .mapNotNull { label ->
                val index = reply.indexOf(label, textLabel)
                if (index >= 0) index else null
            }
        val textEnd = endLabelCandidates.minOrNull() ?: return null
        val duaText = reply.substring(textLabel, textEnd)
            .lineSequence()
            .joinToString(" ") { it.trim() }
            .trim()
            .removePrefix("[")
            .removeSuffix("]")
            .trim('*', ' ', '\n', '\r', '\t')

        val sourceLabel = "المصدر:"
        val sourceIndex = reply.indexOf(sourceLabel, textEnd)
        if (sourceIndex < 0) return null
        val source = reply.substring(sourceIndex + sourceLabel.length)
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .trim()
            .removePrefix("[")
            .removeSuffix("]")
            .trim('*', ' ', '\n', '\r', '\t')

        if (duaText.length < 3 || source.isBlank()) return null
        return SavedSupplication(
            text = duaText.take(MAX_DUA_CHARS),
            source = source.take(MAX_SOURCE_CHARS)
        )
    }

    private const val MAX_DUA_CHARS = 1000
    private const val MAX_SOURCE_CHARS = 160
}

/** Compact, delimiter-based DataStore encoding. The separator is stripped from saved text. */
object SavedSupplicationCodec {
    private const val SEPARATOR = "\u001F"

    fun encode(item: SavedSupplication): String {
        val text = item.text.replace(SEPARATOR, " ").trim().take(1000)
        val source = item.source.replace(SEPARATOR, " ").trim().take(160)
        return "$text$SEPARATOR$source"
    }

    fun decode(raw: String): SavedSupplication? {
        val separatorIndex = raw.indexOf(SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == raw.lastIndex) return null
        val text = raw.substring(0, separatorIndex).trim()
        val source = raw.substring(separatorIndex + SEPARATOR.length).trim()
        if (text.isBlank() || source.isBlank()) return null
        return SavedSupplication(text, source)
    }
}
