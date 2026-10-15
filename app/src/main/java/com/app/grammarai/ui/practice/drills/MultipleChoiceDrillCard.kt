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
fun MultipleChoiceDrillCard(
    exercise: GeneratedExercise,
    selectedOption: String?,
    isAnswerChecked: Boolean,
    onSelectOption: (String) -> Unit,
    onSpeakOption: (String) -> Unit,
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

                // Only show reading passage if it's genuine comprehension text (not prompt or answer)
                if (exercise.readingPassage.isNotBlank() &&
                    !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
                    !exercise.readingPassage.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true)
                ) {
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
                            text = exercise.readingPassage,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
