package com.app.grammarai.ui.practice.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.SkillCategory
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.NeonViolet
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun DrillPromptCard(
    exercise: GeneratedExercise,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isTranscriptRevealed by androidx.compose.runtime.remember(exercise.id) { androidx.compose.runtime.mutableStateOf(false) }

    val categoryBadge = when (exercise.category.canonical) {
        SkillCategory.PRODUCTIVE -> "PRODUCTIVE" to NeonAmber
        SkillCategory.PRAGMATICS -> "PRAGMATICS" to NeonEmerald
        SkillCategory.RECEPTIVE -> "RECEPTIVE" to Color(0xFF38BDF8)
        SkillCategory.STRUCTURE -> "STRUCTURE" to NeonCyan
        SkillCategory.LEXICON -> "LEXICON" to Color(0xFFF43F5E)
        else -> "STRUCTURE" to NeonCyan
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        // Meta row: category tag, translation icon and TTS audio button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(categoryBadge.second.copy(alpha = 0.12f))
                    .border(1.dp, categoryBadge.second.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = categoryBadge.first,
                    color = categoryBadge.second,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isTranslationSupported && onToggleTranslation != null) {
                    IconButton(
                        onClick = onToggleTranslation,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "Traducir ejercicio",
                            tint = if (isTranslationVisible) NeonCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onPlayAudio != null) {
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen to exercise audio",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Instructional Prompt
        Text(
            text = exercise.prompt,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        )

        // Scenario Dialogue Context Bubble (e.g. Barista, Ticket Agent, Pedestrian)
        if (exercise.dialogueContext.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            ScenarioDialogueBubble(dialogueContext = exercise.dialogueContext)
        }

        // Focus Phrase or Reading Passage if present (strictly prevent answer leaks)
        val sentenceText = when (exercise.type) {
            ExerciseType.PRONUNCIATION,
            ExerciseType.REPEAT_AFTER_ME,
            ExerciseType.INTONATION_CHALLENGE,
            ExerciseType.SELECT_PAIRS,
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION -> null

            ExerciseType.MULTIPLE_CHOICE -> {
                // In multiple choice, NEVER display targetPhrase or correctAnswer.
                // Only show readingPassage if it represents a reading comprehension excerpt.
                if (exercise.readingPassage.isNotBlank() &&
                    !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
                    !exercise.readingPassage.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true) &&
                    !exercise.readingPassage.trim().equals(exercise.targetPhrase.trim(), ignoreCase = true)
                ) {
                    exercise.readingPassage
                } else {
                    null
                }
            }

            ExerciseType.FILL_BLANK,
            ExerciseType.FILL_IN_BLANK -> {
                // Only show readingPassage containing the blank placeholder, never the solved targetPhrase
                if (exercise.readingPassage.isNotBlank() &&
                    !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true)
                ) {
                    exercise.readingPassage
                } else {
                    null
                }
            }

            ExerciseType.TRANSLATE -> {
                // In translate, only show the source reading passage, never the target answer
                if (exercise.readingPassage.isNotBlank() &&
                    !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
                    !exercise.readingPassage.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true)
                ) {
                    exercise.readingPassage
                } else {
                    null
                }
            }

            else -> when {
                exercise.readingPassage.isNotBlank() &&
                    !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
                    !exercise.readingPassage.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true) -> exercise.readingPassage
                exercise.targetPhrase.isNotBlank() &&
                    !exercise.targetPhrase.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
                    !exercise.targetPhrase.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true) -> exercise.targetPhrase
                else -> null
            }
        }

        if (exercise.type == ExerciseType.LISTENING_PASSAGE) {
            Spacer(modifier = Modifier.height(14.dp))
            // Audio-First Prominent listening trigger
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

                    if (isTranscriptRevealed && sentenceText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sentenceText,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (sentenceText != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = sentenceText,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )
                    if (exercise.phonetics.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.drill_phonetic_target, exercise.phonetics),
                            color = AccentPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (isTranslationSupported && onToggleTranslation != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTranslationVisible) NeonCyan.copy(alpha = 0.12f) else DarkSurface)
                    .border(
                        1.dp,
                        if (isTranslationVisible) NeonCyan.copy(alpha = 0.45f) else BorderSubtle,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleTranslation() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = if (isTranslationVisible) NeonCyan else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isTranslationVisible) "Ocultar traducción" else "Ver traducción",
                    color = if (isTranslationVisible) NeonCyan else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
