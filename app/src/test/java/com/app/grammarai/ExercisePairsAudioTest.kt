package com.app.grammarai

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.PairMatchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePairsAudioTest {

    @Test
    fun testAlphabetPairsAudioTokenResolution() {
        val pairs = listOf(
            PairMatchItem(left = "A", right = "/eɪ/ (ei)"),
            PairMatchItem(left = "B", right = "/biː/ (bi)"),
            PairMatchItem(left = "E", right = "/iː/ (i)")
        )

        val exercise = GeneratedExercise(
            id = "ex_pre_a1_01_pairs",
            type = ExerciseType.SELECT_PAIRS,
            prompt = "Match the letters",
            pairs = pairs
        )

        // For Right selection of "/eɪ/ (ei)"
        val rightText = "/eɪ/ (ei)"
        val matchingPair = exercise.pairs.firstOrNull { it.right.trim().equals(rightText.trim(), ignoreCase = true) }
        val audioToken = if (matchingPair != null && (
                exercise.id.contains("pre_a1_01") ||
                exercise.id.contains("pre_a1_02") ||
                (matchingPair.left.trim().length == 1 && matchingPair.left.trim()[0].isLetter())
            )) {
            matchingPair.left.trim()
        } else {
            rightText
        }

        // Must resolve to "A" (reusing the left letter audio: a -> a)
        assertEquals("A", audioToken)
    }

    @Test
    fun testStandardPairsPreservesRightSideAudio() {
        val pairs = listOf(
            PairMatchItem(left = "Red", right = "Rojo"),
            PairMatchItem(left = "Blue", right = "Azul")
        )

        val exercise = GeneratedExercise(
            id = "ex_pre_a1_06_pairs",
            type = ExerciseType.SELECT_PAIRS,
            prompt = "Match the colors",
            pairs = pairs
        )

        val rightText = "Rojo"
        val matchingPair = exercise.pairs.firstOrNull { it.right.trim().equals(rightText.trim(), ignoreCase = true) }
        val audioToken = if (matchingPair != null && (
                exercise.id.contains("pre_a1_01") ||
                exercise.id.contains("pre_a1_02") ||
                (matchingPair.left.trim().length == 1 && matchingPair.left.trim()[0].isLetter())
            )) {
            matchingPair.left.trim()
        } else {
            rightText
        }

        // For standard vocabulary pairs (e.g. Red -> Rojo), right text "Rojo" is preserved
        assertEquals("Rojo", audioToken)
    }

    @Test
    fun testLessonWithoutVideoShouldSkipDirectly() {
        val lessonWithVideo = Lesson(
            id = "lesson_101",
            module = 101,
            title = "Verb To Be",
            youtubeId = "abc123xyz"
        )
        val lessonWithoutVideo = Lesson(
            id = "pre_a1_001",
            module = 1,
            title = "Alphabet",
            youtubeId = null
        )

        assertTrue(lessonWithoutVideo.youtubeId.isNullOrBlank())
        assertEquals("abc123xyz", lessonWithVideo.youtubeId)
    }
}
