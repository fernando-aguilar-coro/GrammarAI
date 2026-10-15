package com.app.grammarai.ui.practice.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun ListeningPassageDrillCard(
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
    var isTranscriptRevealed by remember(exercise.id) { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.12f))
                                .clickable { onPlayAudio?.invoke() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play Listening Passage",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Reproducir Pasaje de Audio",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val passageText = exercise.readingPassage.ifBlank { exercise.targetPhrase }
                        if (passageText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isTranscriptRevealed) "Ocultar transcripción ▲" else "Mostrar transcripción (Pista) ▼",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isTranscriptRevealed = !isTranscriptRevealed }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            if (isTranscriptRevealed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = passageText,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (exercise.options.isNotEmpty()) {
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
}
