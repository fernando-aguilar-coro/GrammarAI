package com.app.grammarai.data.util

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.LessonType
import com.app.grammarai.data.model.SkillCategory

object ExerciseCategoryResolver {
    fun resolveCategory(exercise: GeneratedExercise, lessonType: LessonType? = null): SkillCategory {
        // If explicit new canonical category is already assigned, honor it
        when (exercise.category) {
            SkillCategory.LEXICON,
            SkillCategory.RECEPTIVE,
            SkillCategory.PRODUCTIVE,
            SkillCategory.PRAGMATICS -> return exercise.category
            else -> {}
        }

        return when (exercise.type) {
            ExerciseType.SELECT_PAIRS,
            ExerciseType.FLASHCARD,
            ExerciseType.FLASHCARDS -> SkillCategory.LEXICON

            ExerciseType.PRONUNCIATION,
            ExerciseType.REPEAT_AFTER_ME -> SkillCategory.PRODUCTIVE

            ExerciseType.CONVERSATION_DIALOGUE,
            ExerciseType.INTONATION_CHALLENGE -> SkillCategory.PRAGMATICS

            ExerciseType.LONG_READING,
            ExerciseType.READING,
            ExerciseType.LISTENING_PASSAGE,
            ExerciseType.SCRAMBLE_LISTENING -> SkillCategory.RECEPTIVE

            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.THEORY -> SkillCategory.STRUCTURE

            ExerciseType.FILL_BLANK,
            ExerciseType.FILL_IN_BLANK,
            ExerciseType.MULTIPLE_CHOICE,
            ExerciseType.TRANSLATE -> {
                if (lessonType == LessonType.SITUATIONAL_VOCABULARY ||
                    exercise.pairs.isNotEmpty() ||
                    exercise.flashcards.isNotEmpty() ||
                    exercise.category == SkillCategory.VOCABULARY
                ) {
                    SkillCategory.LEXICON
                } else {
                    SkillCategory.STRUCTURE
                }
            }
        }
    }
}
