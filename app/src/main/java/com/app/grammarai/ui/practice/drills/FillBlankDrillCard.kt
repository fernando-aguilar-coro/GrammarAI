package com.app.grammarai.ui.practice.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun FillBlankDrillCard(
    exercise: GeneratedExercise,
    selectedOption: String?,
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    onSelectOption: (String) -> Unit,
    onSpeakOption: (String) -> Unit,
    onInputText: (String) -> Unit,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                ExercisePromptHeader(
                    prompt = exercise.prompt,
                    category = exercise.category,
                    onPlayAudio = onPlayAudio,
                    isTranslationSupported = isTranslationSupported,
                    isTranslationVisible = isTranslationVisible,
                    onToggleTranslation = onToggleTranslation
                )

                if (exercise.dialogueContext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ScenarioDialogueBubble(dialogueContext = exercise.dialogueContext)
                }

                // Show sentence with blank placeholder (e.g. "A coffee with milk, ___.")
                val blankSentence = exercise.readingPassage.takeIf {
                    it.isNotBlank() && !it.trim().equals(exercise.prompt.trim(), ignoreCase = true)
                }
                if (blankSentence != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = blankSentence,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exercise.options.size <= 1) {
            FillInBlankInputView(
                text = selectedOption ?: "",
                onTextChanged = onInputText,
                isAnswerChecked = isAnswerChecked,
                isCorrect = isCorrect,
                correctAnswer = exercise.correctAnswer
            )
        } else {
            DrillOptionList(
                options = exercise.options,
                selectedOption = selectedOption,
                correctAnswer = exercise.correctAnswer,
                isAnswerChecked = isAnswerChecked,
                onSelectOption = onSelectOption,
                onSpeakOption = onSpeakOption
            )
        }
    }
}
