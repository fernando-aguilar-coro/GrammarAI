package com.app.grammarai.ui.practice.audio

/**
 * Handles text sanitization, markdown cleanup, and locale detection
 * for text-to-speech engine and audio pronunciation matching.
 */
object SpeechTextSanitizer {

    private val SPANISH_SPECIAL_CHARS = setOf(
        'á', 'é', 'í', 'ó', 'ú', 'ñ',
        'Á', 'É', 'Í', 'Ó', 'Ú', 'Ñ',
        '¿', '¡'
    )

    private val SPANISH_COMMON_WORDS = setOf(
        "el", "la", "los", "las", "un", "una", "de", "del", "en", "se", "para", "con", "por",
        "que", "y", "o", "es", "son", "su", "sus", "al", "consejo", "muebles", "equipaje",
        "noticias", "clima", "dinero", "información", "trabajo", "ambos", "ninguno"
    )

    private val PUNCTUATION_TRIM_REGEX = Regex("^[.,!?;:¿¡\"']+|[.,!?;:¿¡\"']+$")
    private val UNDERSCORES_REGEX = Regex("_+")

    /**
     * Cleans an individual word by trimming whitespace and punctuation, lowercased.
     */
    fun cleanWord(word: String): String {
        return word.trim()
            .replace(PUNCTUATION_TRIM_REGEX, "")
            .lowercase()
    }

    /**
     * Prepares text for TTS playback by replacing blanks/underscores with the word 'blank'.
     */
    fun prepareTtsText(text: String): String {
        return text.replace(UNDERSCORES_REGEX, "blank").trim()
    }

    /**
     * Determines whether a given text snippet is likely Spanish based on
     * accents, punctuation (¿, ¡), and high-frequency Spanish function words.
     */
    fun isLikelySpanish(text: String): Boolean {
        if (text.any { it in SPANISH_SPECIAL_CHARS }) return true
        val lower = text.lowercase().trim()
        return lower.split(Regex("\\s+")).any { it in SPANISH_COMMON_WORDS }
    }

    /**
     * Converts raw Markdown formatting into clean spoken prose for Android TTS.
     * Removes headers, dividers, bold/italic, code blocks, lists, and formats tables.
     */
    fun cleanMarkdownForSpeech(markdown: String): String {
        var text = markdown

        // Remove markdown headers (# Header)
        text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")

        // Remove horizontal dividers (---, ***)
        text = text.replace(Regex("^[\\-*_]{3,}\\s*$", RegexOption.MULTILINE), "")

        // Clean table rows: convert "| cell1 | cell2 |" into readable sentences
        text = text.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (trimmed.contains("---")) {
                    ""
                } else {
                    trimmed.split("|")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(". ")
                }
            } else {
                line
            }
        }

        // Remove markdown bold, italic, code formatting
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")
        text = text.replace(Regex("`([^`]+)`"), "$1")

        // Remove list bullets and blockquotes
        text = text.replace(Regex("^[*-]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^>+\\s*", RegexOption.MULTILINE), "")

        // Collapse excess blank lines
        text = text.replace(Regex("\n{3,}"), "\n\n")

        return text.trim()
    }
}
