package com.app.grammarai.ui.practice.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.grammarai.R
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.practice.ExerciseUiState
import com.app.grammarai.ui.practice.feedback.ExerciseFeedbackSection
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald

@Composable
fun ExerciseBottomActionBar(
    state: ExerciseUiState,
    onContinueFromConversation: () -> Unit,
    onCheckAnswer: () -> Unit,
    onNextExercise: () -> Unit,
    onGiveUpExercise: () -> Unit = {},
    onRequestAiEvaluation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val exercise = state.currentExercise
    if (exercise?.type == ExerciseType.CONVERSATION_DIALOGUE) {
        return
    }

    val isAnswerChecked = state.isAnswerChecked
    val isCorrect = state.isCorrect
    val isLast = state.currentExerciseIndex == state.exercises.size - 1

    val sheetBgColor by animateColorAsState(
        targetValue = when {
            !isAnswerChecked -> DarkBackground
            isCorrect -> Color(0xFF0F241B)
            else -> Color(0xFF261418)
        },
        label = "sheet_bg_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(sheetBgColor)
            .border(
                1.dp,
                when {
                    !isAnswerChecked -> Color.Transparent
                    isCorrect -> SuccessEmerald.copy(alpha = 0.35f)
                    else -> ErrorCrimson.copy(alpha = 0.35f)
                },
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ExerciseFeedbackSection(
            isAnswerChecked = isAnswerChecked,
            isCorrect = isCorrect,
            isOverriddenByAi = state.isOverriddenByAi,
            isEvaluatingWithAi = state.isEvaluatingWithAi,
            aiExplanation = state.aiExplanation,
            exercise = exercise,
            onRequestAiEvaluation = onRequestAiEvaluation
        )

        val isVoiceExercise = exercise?.type == ExerciseType.PRONUNCIATION ||
                exercise?.type == ExerciseType.REPEAT_AFTER_ME ||
                exercise?.type == ExerciseType.INTONATION_CHALLENGE
        val isVoiceWithoutInput = isVoiceExercise &&
                state.spokenTranscript.isBlank() &&
                state.selectedOption == null

        if (isVoiceWithoutInput && !isAnswerChecked) {
            GrammarButton(
                text = stringResource(R.string.drill_btn_skip_voice_exercise),
                onClick = onGiveUpExercise,
                style = GrammarButtonStyle.OUTLINED,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (!isAnswerChecked && exercise?.type != ExerciseType.THEORY && exercise?.type != ExerciseType.FLASHCARDS && exercise?.type != ExerciseType.FLASHCARD) {
            val canCheck = when (exercise?.type) {
                ExerciseType.SCRAMBLED_SENTENCE,
                ExerciseType.SENTENCE_RECONSTRUCTION,
                ExerciseType.SCRAMBLE_LISTENING -> state.reconstructedWords.isNotEmpty()
                ExerciseType.PRONUNCIATION,
                ExerciseType.REPEAT_AFTER_ME,
                ExerciseType.INTONATION_CHALLENGE -> state.spokenTranscript.isNotBlank() || state.selectedOption != null
                ExerciseType.SELECT_PAIRS -> exercise.pairs.isNotEmpty() && state.matchedPairs.size == exercise.pairs.size
                ExerciseType.TRANSLATE -> state.translationAnswer.isNotBlank()
                else -> !state.selectedOption.isNullOrBlank()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrammarButton(
                    text = "Saltar",
                    onClick = onGiveUpExercise,
                    style = GrammarButtonStyle.OUTLINED,
                    height = 50.dp,
                    modifier = Modifier.weight(0.38f)
                )

                GrammarButton(
                    text = stringResource(R.string.drill_btn_check),
                    onClick = onCheckAnswer,
                    style = GrammarButtonStyle.PRIMARY,
                    enabled = canCheck,
                    height = 50.dp,
                    modifier = Modifier.weight(0.62f)
                )
            }
        } else {
            val buttonStyle = when {
                exercise?.type == ExerciseType.THEORY || exercise?.type == ExerciseType.FLASHCARDS || exercise?.type == ExerciseType.FLASHCARD -> GrammarButtonStyle.PRIMARY
                !isAnswerChecked -> GrammarButtonStyle.PRIMARY
                isCorrect -> GrammarButtonStyle.SUCCESS
                else -> GrammarButtonStyle.DANGER
            }

            GrammarButton(
                text = when {
                    exercise?.type == ExerciseType.THEORY -> "Continuar a los ejercicios"
                    exercise?.type == ExerciseType.FLASHCARDS || exercise?.type == ExerciseType.FLASHCARD -> {
                        when {
                            !isAnswerChecked -> "Entendido"
                            isLast -> stringResource(R.string.drill_btn_finish)
                            else -> "Continuar"
                        }
                    }
                    !isAnswerChecked -> stringResource(R.string.drill_btn_check)
                    isLast -> stringResource(R.string.drill_btn_finish)
                    else -> stringResource(R.string.drill_btn_next)
                },
                onClick = {
                    if (exercise?.type == ExerciseType.FLASHCARDS || exercise?.type == ExerciseType.FLASHCARD) {
                        if (!isAnswerChecked) onCheckAnswer() else onNextExercise()
                    } else if (exercise?.type == ExerciseType.THEORY) {
                        onNextExercise()
                    } else if (!isAnswerChecked) {
                        onCheckAnswer()
                    } else {
                        onNextExercise()
                    }
                },
                style = buttonStyle,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
