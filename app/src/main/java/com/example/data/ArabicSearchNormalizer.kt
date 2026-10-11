package com.example.data

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizes Arabic text for search without changing the stored Quranic or hadith text.
 */
object ArabicSearchNormalizer {
    fun normalize(value: String): String {
        val canonical = Normalizer.normalize(value, Normalizer.Form.NFKC)
        val withoutMarks = canonical.filterNot { character ->
            character in '\u064B'..'\u065F' ||
                character == '\u0670' ||
                character in '\u06D6'..'\u06ED' ||
                character == '\u0640' ||
                character == '\u200E' ||
                character == '\u200F'
        }

        return withoutMarks
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
