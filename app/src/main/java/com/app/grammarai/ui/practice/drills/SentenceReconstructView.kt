package com.app.grammarai.ui.practice.drills

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceReconstructView(
    scrambledWords: List<String>,
    reconstructedWords: List<String>,
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    onWordTap: (String) -> Unit,
    onRemoveWord: (Int) -> Unit,
    onClear: () -> Unit,
    onPlaySentence: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val boxBorderColor by animateColorAsState(
        targetValue = when {
            isAnswerChecked && isCorrect -> SuccessEmerald
            isAnswerChecked && !isCorrect -> ErrorCrimson
            reconstructedWords.isNotEmpty() -> AccentPrimary.copy(alpha = 0.5f)
            else -> BorderSubtle
        },
        label = "reconstruct_border_color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Target Sentence Assembly Area (interactive placed chips)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(1.5.dp, boxBorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (reconstructedWords.isEmpty()) {
                Text(
                    text = stringResource(R.string.drill_reconstruct_instruction),
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reconstructedWords.forEachIndexed { index, word ->
                        PlacedWordChip(
                            word = word,
                            isAnswerChecked = isAnswerChecked,
                            onRemove = { onRemoveWord(index) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Actions row: Listen to sentence TTS & Reset/Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val listenText = if (scrambledWords.all { it.length <= 2 }) "Escuchar deletreo" else "Escuchar audio"
            com.app.grammarai.ui.common.components.GrammarButton(
                text = listenText,
                onClick = onPlaySentence,
                style = com.app.grammarai.ui.common.components.GrammarButtonStyle.GHOST,
                leadingIcon = Icons.AutoMirrored.Filled.VolumeUp
            )

            if (!isAnswerChecked && reconstructedWords.isNotEmpty()) {
                com.app.grammarai.ui.common.components.GrammarButton(
                    text = stringResource(R.string.drill_reconstruct_clear),
                    onClick = onClear,
                    style = com.app.grammarai.ui.common.components.GrammarButtonStyle.GHOST,
                    leadingIcon = Icons.Default.Refresh
                )
            }
        }

        // Available Words Pool
        val poolLabel = if (scrambledWords.all { it.length <= 2 }) "TOCA LAS LETRAS PARA ORDENAR:" else "TOCA LAS PALABRAS PARA ORDENAR:"
        Text(
            text = poolLabel,
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scrambledWords.forEachIndexed { index, word ->
                // Calculate if this instance is consumed
                val timesInScrambledBefore = scrambledWords.take(index + 1).count { it == word }
                val timesInReconstructed = reconstructedWords.count { it == word }
                val isUsed = timesInScrambledBefore <= timesInReconstructed

                BankWordChip(
                    word = word,
                    isUsed = isUsed,
                    isAnswerChecked = isAnswerChecked,
                    onTap = { onWordTap(word) }
                )
            }
        }
    }
}

@Composable
private fun PlacedWordChip(
    word: String,
    isAnswerChecked: Boolean,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .tactilePress(enabled = !isAnswerChecked)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(enabled = !isAnswerChecked) { onRemove() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = word,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        if (!isAnswerChecked) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun BankWordChip(
    word: String,
    isUsed: Boolean,
    isAnswerChecked: Boolean,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .tactilePress(enabled = !isUsed && !isAnswerChecked)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isUsed) DarkSurface.copy(alpha = 0.4f) else DarkSurfaceCard)
            .border(
                1.dp,
                if (isUsed) BorderSubtle.copy(alpha = 0.2f) else BorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !isUsed && !isAnswerChecked) { onTap() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = word,
            color = if (isUsed) TextMuted.copy(alpha = 0.35f) else TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
