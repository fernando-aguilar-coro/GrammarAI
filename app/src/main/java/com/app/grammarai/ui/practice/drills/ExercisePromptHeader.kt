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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.SkillCategory
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.NeonViolet
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun ExercisePromptHeader(
    prompt: String,
    category: SkillCategory,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryBadge = when (category.canonical) {
        SkillCategory.STRUCTURE -> "GRAMÁTICA" to NeonViolet
        SkillCategory.LEXICON -> "VOCABULARIO" to NeonEmerald
        SkillCategory.PRAGMATICS -> "SITUACIONAL" to NeonAmber
        SkillCategory.RECEPTIVE -> "COMPRENSIÓN" to NeonCyan
        SkillCategory.PRODUCTIVE -> "PRONUNCIACIÓN" to AccentPrimary
        else -> "PRÁCTICA" to NeonViolet
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                            contentDescription = "Escuchar audio del ejercicio",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (prompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = prompt,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
