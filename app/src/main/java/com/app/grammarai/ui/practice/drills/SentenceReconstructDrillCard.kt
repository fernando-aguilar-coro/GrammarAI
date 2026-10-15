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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.ui.theme.DarkSurface

@Composable
fun SentenceReconstructDrillCard(
    exercise: GeneratedExercise,
    reconstructedWords: List<String>,
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    onWordTap: (String) -> Unit,
    onRemoveWord: (Int) -> Unit,
    onClear: () -> Unit,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isListeningDrill = exercise.type == ExerciseType.SCRAMBLE_LISTENING

    val displayedScrambledWords = remember(exercise.id, exercise.scrambledWords) {
        if (exercise.scrambledWords.isNotEmpty()) {
            exercise.scrambledWords
        } else {
            val text = exercise.correctAnswer.ifBlank { exercise.targetPhrase }
            val rawTokens = if (text.contains(" - ")) {
                text.split("\\s*-\\s*".toRegex())
            } else {
                text.split("\\s+".toRegex())
            }
            val tokens = rawTokens
                .map { it.trim().trim('.', ',', '!', '?', ';', ':', '¿', '¡', '"') }
                .filter { it.isNotEmpty() }
            if (tokens.size > 1) tokens.shuffled() else tokens
        }
    }

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
                    isTranslationSupported = if (isListeningDrill) false else isTranslationSupported,
                    isTranslationVisible = if (isListeningDrill) false else isTranslationVisible,
                    onToggleTranslation = if (isListeningDrill) null else onToggleTranslation
                )

                if (isListeningDrill) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onPlayAudio?.invoke() }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Reproducir audio",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Toca para escuchar el audio",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SentenceReconstructView(
            scrambledWords = displayedScrambledWords,
            reconstructedWords = reconstructedWords,
            isAnswerChecked = isAnswerChecked,
            isCorrect = isCorrect,
            onWordTap = onWordTap,
            onRemoveWord = onRemoveWord,
            onClear = onClear,
            onPlaySentence = onPlayAudio ?: {}
        )
    }
}
