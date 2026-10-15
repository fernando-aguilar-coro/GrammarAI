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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard

@Composable
fun PronunciationDrillCard(
    exercise: GeneratedExercise,
    isListening: Boolean,
    spokenTranscript: String,
    matchScore: Int?,
    onToggleRecord: () -> Unit,
    onSkip: () -> Unit,
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DrillSpeechRecordCard(
            targetPhrase = exercise.targetPhrase.ifBlank { exercise.correctAnswer },
            isListening = isListening,
            spokenTranscript = spokenTranscript,
            matchScore = matchScore,
            onToggleRecord = onToggleRecord,
            onSkip = onSkip
        )
    }
}
