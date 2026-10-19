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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.practice.TranslationAnswerMode
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun TranslateDrillCard(
    exercise: GeneratedExercise,
    answerMode: TranslationAnswerMode,
    textAnswer: String,
    orderedWords: List<String>,
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    onAnswerModeChange: (TranslationAnswerMode) -> Unit,
    onTextAnswerChange: (String) -> Unit,
    onAddWord: (String) -> Unit,
    onRemoveWord: (Int) -> Unit,
    onClearWords: () -> Unit,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val expectedAnswer = exercise.correctAnswer.ifBlank { exercise.targetPhrase }
    val wordBank = remember(exercise.id, expectedAnswer) {
        val tokens = expectedAnswer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val shuffled = tokens.shuffled()
        if (tokens.size > 1 && shuffled == tokens) shuffled.drop(1) + shuffled.first() else shuffled
    }
    val sourceText = exercise.readingPassage.takeIf {
        it.isNotBlank() &&
            !it.trim().equals(exercise.prompt.trim(), ignoreCase = true) &&
            !it.trim().equals(exercise.correctAnswer.trim(), ignoreCase = true)
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
                    isTranslationSupported = isTranslationSupported,
                    isTranslationVisible = isTranslationVisible,
                    onToggleTranslation = onToggleTranslation
                )

                if (sourceText != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = sourceText,
                        color = TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TranslationModeButton(
                label = "Escribir",
                selected = answerMode == TranslationAnswerMode.WRITE,
                enabled = !isAnswerChecked,
                onClick = { onAnswerModeChange(TranslationAnswerMode.WRITE) },
                modifier = Modifier.weight(1f)
            )
            TranslationModeButton(
                label = "Ordenar palabras",
                selected = answerMode == TranslationAnswerMode.ORDER,
                enabled = !isAnswerChecked,
                onClick = { onAnswerModeChange(TranslationAnswerMode.ORDER) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (answerMode) {
            TranslationAnswerMode.WRITE -> FillInBlankInputView(
                text = textAnswer,
                onTextChanged = onTextAnswerChange,
                isAnswerChecked = isAnswerChecked,
                isCorrect = isCorrect
            )

            TranslationAnswerMode.ORDER -> SentenceReconstructView(
                scrambledWords = wordBank,
                reconstructedWords = orderedWords,
                isAnswerChecked = isAnswerChecked,
                isCorrect = isCorrect,
                onWordTap = onAddWord,
                onRemoveWord = onRemoveWord,
                onClear = onClearWords,
                onPlaySentence = onPlayAudio ?: {}
            )
        }
    }
}

@Composable
private fun TranslationModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) AccentPrimary.copy(alpha = 0.16f) else DarkSurface)
            .border(
                width = 1.dp,
                color = if (selected) AccentPrimary.copy(alpha = 0.55f) else BorderSubtle,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AccentPrimary else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}