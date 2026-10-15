package com.app.grammarai.ui.practice.drills

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.app.grammarai.data.model.FlashcardItem
import com.app.grammarai.data.model.GeneratedExercise

@Composable
fun FlashcardDrillCard(
    exercise: GeneratedExercise,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val card = remember(exercise.id, exercise.flashcards, exercise.targetPhrase) {
        exercise.flashcards.firstOrNull() ?: FlashcardItem(
            front = exercise.targetPhrase.ifBlank { exercise.prompt },
            back = exercise.correctAnswer.ifBlank { exercise.explanation },
            phonetic = exercise.phonetics
        )
    }

    FlashcardCardView(
        card = card,
        prompt = exercise.prompt,
        onSpeak = onSpeak,
        modifier = modifier
    )
}
