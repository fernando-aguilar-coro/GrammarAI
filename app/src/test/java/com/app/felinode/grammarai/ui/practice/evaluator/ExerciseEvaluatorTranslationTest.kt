package com.app.grammarai.ui.practice.evaluator

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseEvaluatorTranslationTest {
    private val exercise = GeneratedExercise(
        id = "translate_test",
        type = ExerciseType.TRANSLATE,
        prompt = "Translate this sentence",
        correctAnswer = "I am ready."
    )

    @Test
    fun acceptsTypedAndOrderedTranslationAnswers() {
        assertTrue(isCorrect("i am ready"))
        assertTrue(isCorrect(listOf("I", "am", "ready.").joinToString(" ")))
        assertFalse(isCorrect("I ready am"))
        assertFalse(isCorrect(""))
    }

    private fun isCorrect(answer: String): Boolean = ExerciseEvaluator.isAnswerCorrect(
        exercise = exercise,
        selectedOption = null,
        reconstructedWords = emptyList(),
        translationAnswer = answer
    )
}