package com.app.grammarai.ui.practice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarChip
import com.app.grammarai.ui.common.components.GrammarIconButton
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import com.app.grammarai.ui.theme.WarningAmber

@Composable
fun ExerciseHeaderBar(
    currentIndex: Int,
    totalCount: Int,
    progressFraction: Float,
    score: Int,
    isPlayingAudio: Boolean,
    isAudioLoading: Boolean,
    consecutiveCorrectCount: Int = 0,
    onBackClick: () -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GrammarIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.nav_back),
                    size = 38.dp,
                    iconSize = 18.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = AccentPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (consecutiveCorrectCount >= 2) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn()
                    ) {
                        GrammarChip(
                            text = "🔥 $consecutiveCorrectCount",
                            textColor = WarningAmber,
                            containerColor = WarningAmber.copy(alpha = 0.12f),
                            borderColor = WarningAmber.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            shapeRadius = 6.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                GrammarIconButton(
                    icon = if (isPlayingAudio) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    onClick = onToggleAudio,
                    contentDescription = "Audio",
                    size = 38.dp,
                    iconSize = 18.dp,
                    containerColor = if (isPlayingAudio) AccentPrimary.copy(alpha = 0.15f) else DarkSurfaceCard,
                    borderColor = if (isPlayingAudio) AccentPrimary.copy(alpha = 0.4f) else BorderSubtle,
                    tint = if (isPlayingAudio) AccentPrimary else TextSecondary
                )

                Spacer(modifier = Modifier.width(8.dp))

                GrammarChip(
                    text = "$score XP",
                    textColor = SuccessEmerald,
                    containerColor = SuccessEmerald.copy(alpha = 0.10f),
                    borderColor = SuccessEmerald.copy(alpha = 0.25f),
                    fontSize = 12.sp,
                    shapeRadius = 6.dp
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentPrimary,
            trackColor = DarkSurface
        )
    }
}
