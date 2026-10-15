package com.app.grammarai.core.audio

object PronunciationScorer {

    /**
     * Evaluates pronunciation match between student's spoken transcript and target phrase.
     * Returns accuracy score from 0 to 100%.
     */
    fun calculateMatchScore(spoken: String, target: String): Int {
        if (spoken.isBlank() || target.isBlank()) return 0

        val cleanSpoken = sanitizeText(spoken)
        val cleanTarget = sanitizeText(target)

        if (cleanSpoken == cleanTarget) return 100

        val spokenTokens = cleanSpoken.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val targetTokens = cleanTarget.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (targetTokens.isEmpty()) return 0

        var matchCount = 0
        for (token in spokenTokens) {
            if (targetTokens.contains(token)) matchCount++
        }

        val precision = matchCount.toFloat() / spokenTokens.size.coerceAtLeast(1)
        val recall = matchCount.toFloat() / targetTokens.size
        val f1 = if (precision + recall > 0) (2 * precision * recall) / (precision + recall) else 0f

        return (f1 * 100).toInt().coerceIn(10, 100)
    }

    private fun sanitizeText(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }
}
