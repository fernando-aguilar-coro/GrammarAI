package com.app.grammarai.ui.practice.drills

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary

/**
 * Renders target phrase words. As the native Android SpeechRecognizer
 * captures spoken words, recognized tokens turn NeonEmerald with a smooth transition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordPronunciationHighlightView(
    targetPhrase: String,
    spokenTranscript: String,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val targetWords = targetPhrase.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val spokenTokens = spokenTranscript.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .toSet()

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        targetWords.forEachIndexed { index, word ->
            val cleanWord = word.lowercase().replace(Regex("[^a-z0-9]"), "")
            val isRecognized = spokenTokens.contains(cleanWord)

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isRecognized -> NeonEmerald.copy(alpha = 0.18f)
                    isListening && index == spokenTokens.size.coerceAtMost(targetWords.size - 1) -> NeonAmber.copy(alpha = 0.12f)
                    else -> DarkSurface
                },
                label = "wordBg"
            )

            val borderColor by animateColorAsState(
                targetValue = when {
                    isRecognized -> NeonEmerald
                    isListening && index == spokenTokens.size.coerceAtMost(targetWords.size - 1) -> NeonAmber.copy(alpha = 0.6f)
                    else -> Color(0x33475569)
                },
                label = "wordBorder"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    isRecognized -> NeonEmerald
                    isListening && index == spokenTokens.size.coerceAtMost(targetWords.size - 1) -> NeonAmber
                    else -> if (spokenTranscript.isNotBlank()) TextMuted else TextPrimary
                },
                label = "wordText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = word,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = if (isRecognized) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
