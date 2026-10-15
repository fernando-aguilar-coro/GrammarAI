package com.app.grammarai

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.FlashcardItem
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.repository.expandFlashcardExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumRepositoryFlashcardNormalizationTest {

    @Test
    fun splitsFlashcardDeckIntoIndividualExercises() {
        val exercise = GeneratedExercise(
            id = "ex_situational_vocab",
            type = ExerciseType.FLASHCARDS,
            prompt = "Learn the key phrases",
            flashcards = listOf(
                FlashcardItem(front = "Coffee", back = "Cafe"),
                FlashcardItem(front = "Latte", back = "Coffee with milk")
            )
        )

        val normalized = expandFlashcardExercises(exercise)

        assertEquals(2, normalized.size)
        assertEquals(listOf("ex_situational_vocab_0", "ex_situational_vocab_1"), normalized.map { it.id })
        assertTrue(normalized.all { it.type == ExerciseType.FLASHCARD })
        assertEquals(listOf("Coffee", "Latte"), normalized.map { it.targetPhrase })
        assertTrue(normalized.all { it.flashcards.size == 1 })
        assertTrue(normalized.all { it.prompt == exercise.prompt })
    }

    @Test
    fun convertsSingleLegacyDeckWithoutSplittingIt() {
        val exercise = GeneratedExercise(
            id = "ex_single_vocab",
            type = ExerciseType.FLASHCARDS,
            prompt = "Learn this word",
            flashcards = listOf(FlashcardItem(front = "Coffee", back = "Cafe"))
        )

        val normalized = expandFlashcardExercises(exercise)

        assertEquals(1, normalized.size)
        assertEquals(ExerciseType.FLASHCARD, normalized.single().type)
        assertEquals("Coffee", normalized.single().flashcards.single().front)
    }
}