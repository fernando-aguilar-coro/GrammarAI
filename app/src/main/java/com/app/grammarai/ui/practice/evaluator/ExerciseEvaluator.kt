package com.app.grammarai.ui.practice.evaluator

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise

object ExerciseEvaluator {

    private fun normalize(s: String): String {
        return s.replace(Regex("[.,!?;:¿¡\"']"), "").trim().lowercase()
    }

    fun isAnswerCorrect(
        exercise: GeneratedExercise,
        selectedOption: String?,
        reconstructedWords: List<String>,
        spokenTranscript: String = "",
        pronunciationMatchScore: Int? = null
    ): Boolean {
        return when (exercise.type) {
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.SCRAMBLE_LISTENING -> {
                val sentence = reconstructedWords.joinToString(" ")
                val compact = reconstructedWords.joinToString("")
                val target = exercise.correctAnswer.ifBlank { exercise.targetPhrase }
                val normTarget = normalize(target)
                val targetNoHyphen = normalize(target.replace("-", " "))
                val targetCompact = normalize(target.replace(Regex("[- ]"), ""))

                normalize(sentence) == normTarget ||
                    normalize(sentence) == targetNoHyphen ||
                    normalize(compact) == targetCompact
            }
            ExerciseType.PRONUNCIATION,
            ExerciseType.REPEAT_AFTER_ME,
            ExerciseType.INTONATION_CHALLENGE -> {
                if (pronunciationMatchScore != null) {
                    pronunciationMatchScore >= 60
                } else if (spokenTranscript.isNotBlank()) {
                    val target = exercise.targetPhrase.ifBlank { exercise.correctAnswer }
                    normalize(spokenTranscript) == normalize(target)
                } else if (selectedOption != null) {
                    normalize(selectedOption) == normalize(exercise.correctAnswer)
                } else {
                    false
                }
            }
            ExerciseType.SELECT_PAIRS,
            ExerciseType.THEORY,
            ExerciseType.FLASHCARDS,
            ExerciseType.FLASHCARD -> true
            else -> {
                if (selectedOption == null) false
                else normalize(selectedOption) == normalize(exercise.correctAnswer)
            }
        }
    }

    fun getSpokenExerciseText(exercise: GeneratedExercise): String {
        val raw = when (exercise.type) {
            ExerciseType.THEORY -> {
                exercise.readingPassage.ifBlank { exercise.prompt }
            }
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.SCRAMBLE_LISTENING -> {
                exercise.targetPhrase.ifBlank { exercise.correctAnswer.ifBlank { exercise.prompt } }
            }
            ExerciseType.PRONUNCIATION, ExerciseType.REPEAT_AFTER_ME, ExerciseType.TRANSLATE -> {
                exercise.targetPhrase.ifBlank { exercise.correctAnswer.ifBlank { exercise.prompt } }
            }
            ExerciseType.FILL_BLANK, ExerciseType.FILL_IN_BLANK -> {
                exercise.readingPassage.ifBlank { exercise.targetPhrase.ifBlank { exercise.prompt } }
            }
            ExerciseType.SELECT_PAIRS -> {
                exercise.prompt
            }
            ExerciseType.FLASHCARDS, ExerciseType.FLASHCARD -> {
                exercise.flashcards.firstOrNull()?.front ?: exercise.prompt
            }
            else -> {
                if (exercise.readingPassage.isNotBlank()) exercise.readingPassage
                else if (exercise.targetPhrase.isNotBlank()) exercise.targetPhrase
                else exercise.prompt
            }
        }
        return raw.replace(Regex("_+"), "blank").trim()
    }
}
