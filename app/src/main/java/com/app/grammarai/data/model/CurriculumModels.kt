package com.app.grammarai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LessonType {
    GRAMMAR_PILLAR,
    EVERYDAY_ACTION,
    WORKPLACE_ACTION,
    SITUATIONAL_VOCABULARY
}

@Serializable
enum class LessonStatus {
    LOCKED,
    READY,
    IN_PROGRESS,
    MASTERED
}

@Serializable
enum class SkillCategory {
    @SerialName("LEXICON")
    LEXICON,

    @SerialName("STRUCTURE")
    STRUCTURE,

    @SerialName("RECEPTIVE")
    RECEPTIVE,

    @SerialName("PRODUCTIVE")
    PRODUCTIVE,

    @SerialName("PRAGMATICS")
    PRAGMATICS,

    // Legacy aliases for backward compatibility during deserialization
    @SerialName("VOCABULARY")
    VOCABULARY,

    @SerialName("GRAMMAR")
    GRAMMAR,

    @SerialName("INTONATION")
    INTONATION,

    @SerialName("PRONUNCIATION")
    PRONUNCIATION,

    @SerialName("FLUENCY")
    FLUENCY;

    val canonical: SkillCategory
        get() = when (this) {
            VOCABULARY -> LEXICON
            GRAMMAR -> STRUCTURE
            INTONATION -> RECEPTIVE
            PRONUNCIATION -> PRODUCTIVE
            FLUENCY -> PRAGMATICS
            else -> this
        }
}

@Serializable
enum class ExerciseType {
    @SerialName("multiple_choice")
    MULTIPLE_CHOICE,

    @SerialName("fill_blank")
    FILL_BLANK,

    @SerialName("select_pairs")
    SELECT_PAIRS,

    @SerialName("scrambled_sentence")
    SCRAMBLED_SENTENCE,

    @SerialName("pronunciation")
    PRONUNCIATION,

    @SerialName("translate")
    TRANSLATE,

    @SerialName("conversation_dialogue")
    CONVERSATION_DIALOGUE,

    @SerialName("theory")
    THEORY,

    @SerialName("long_reading")
    LONG_READING,

    @SerialName("reading")
    READING,

    @SerialName("listening_passage")
    LISTENING_PASSAGE,

    @SerialName("intonation_challenge")
    INTONATION_CHALLENGE,

    @SerialName("flashcards")
    FLASHCARDS,

    @SerialName("flashcard")
    FLASHCARD,

    @SerialName("scramble_listening")
    SCRAMBLE_LISTENING,

    // Legacy aliases
    @SerialName("REPEAT_AFTER_ME")
    REPEAT_AFTER_ME,

    @SerialName("SENTENCE_RECONSTRUCTION")
    SENTENCE_RECONSTRUCTION,

    @SerialName("FILL_IN_BLANK")
    FILL_IN_BLANK
}

@Serializable
data class PairMatchItem(
    val left: String,
    val right: String
)

@Serializable
data class FlashcardItem(
    val front: String,
    val back: String,
    val phonetic: String = "",
    val example: String = "",
    val exampleTranslation: String = ""
)

@Serializable
data class GeneratedExercise(
    val id: String,
    val type: ExerciseType,
    val prompt: String,
    val options: List<String> = emptyList(),
    val scrambledWords: List<String> = emptyList(),
    val correctAnswer: String = "",
    val explanation: String = "",
    val expectedPattern: String = "",
    val readingPassage: String = "",
    val dialogueContext: String = "",
    val audioBase64: String = "",
    val category: SkillCategory = SkillCategory.STRUCTURE,
    val targetPhrase: String = "",
    val phonetics: String = "",
    val pairs: List<PairMatchItem> = emptyList(),
    val flashcards: List<FlashcardItem> = emptyList()
)

@Serializable
data class Lesson(
    val id: String,
    val module: Int,
    val type: LessonType = LessonType.GRAMMAR_PILLAR,
    val cefr: String = "A1",
    val title: String,
    val summary: String = "",
    val durationMinutes: Int = 5,
    val exercises: List<GeneratedExercise> = emptyList(),
    val status: LessonStatus = LessonStatus.READY,
    val score: Int = 0,
    val youtubeId: String? = null,
    val theoryMarkdown: String = ""
) {
    val moduleTwoDigits: String
        get() = module.toString().takeLast(2).padStart(2, '0')

    val isMastered: Boolean
        get() = status == LessonStatus.MASTERED

    val isLocked: Boolean
        get() = status == LessonStatus.LOCKED
}

@Serializable
data class SkillMetrics(
    val lexicon: Int = 0,
    val structure: Int = 0,
    val receptive: Int = 0,
    val productive: Int = 0,
    val pragmatics: Int = 0,
    val overallCefr: String = "A1"
) {
    val pronunciation: Int get() = productive
    val fluency: Int get() = pragmatics
    val intonation: Int get() = receptive
    val grammar: Int get() = structure
    val vocabulary: Int get() = lexicon

    fun maxScore(): Int = maxOf(lexicon, structure, receptive, productive, pragmatics)
    fun totalScore(): Int = lexicon + structure + receptive + productive + pragmatics

    fun calculateCefr(): String = when {
        totalScore() >= 1500 -> "C1"
        totalScore() >= 900 -> "B2"
        totalScore() >= 450 -> "B1"
        totalScore() >= 150 -> "A2"
        totalScore() >= 50 -> "A1"
        else -> if (overallCefr.contains("Pre-A1", true) || overallCefr.contains("A0", true)) "Pre-A1" else "A1"
    }

    fun getTopStrength(): Pair<String, Int> {
        val list = listOf(
            "Productive" to productive,
            "Pragmatics" to pragmatics,
            "Receptive" to receptive,
            "Structure" to structure,
            "Lexicon" to lexicon
        )
        return list.maxByOrNull { it.second } ?: ("Productive" to productive)
    }

    fun getGrowthPriority(): Pair<String, Int> {
        val list = listOf(
            "Productive" to productive,
            "Lexicon" to lexicon,
            "Structure" to structure,
            "Receptive" to receptive,
            "Pragmatics" to pragmatics
        )
        return list.minByOrNull { it.second } ?: ("Productive" to productive)
    }
}

@Serializable
data class DialogueEvaluation(
    val isCorrect: Boolean,
    val feedback: String,
    val grammarScore: Int,
    val naturalAlternative: String
)

@Serializable
data class ShadowingEvaluation(
    val accuracyScore: Int,
    val rhythmFeedback: String,
    val phoneticTips: String
)

@Serializable
data class AiEvaluationResult(
    val isCorrect: Boolean,
    val explanation: String
)
