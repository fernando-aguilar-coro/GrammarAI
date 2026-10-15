package com.app.grammarai.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.onboarding.ChatMessage
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.WarningAmber

@Composable
fun AiChatBubble(
    message: ChatMessage,
    isAudioLoading: Boolean,
    isPlayingAudio: Boolean,
    onToggleAudio: () -> Unit,
    onToggleTranslation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    // Audio control & speaking animation
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlayingAudio) {
                            AudioWaveformBars()
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (isAudioLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = onToggleAudio,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(
                                        if (isPlayingAudio) R.string.onboarding_audio_stop else R.string.onboarding_audio_listen
                                    ),
                                    tint = if (isPlayingAudio) WarningAmber else AccentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Translation toggle button if translation is available
                if (!message.translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = onToggleTranslation,
                        shape = RoundedCornerShape(8.dp),
                        color = if (message.isTranslationVisible) AccentPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (message.isTranslationVisible) AccentPrimary.copy(alpha = 0.6f) else BorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translate",
                                tint = if (message.isTranslationVisible) AccentPrimary else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (message.isTranslationVisible) "Ocultar traducción" else "Ver traducción",
                                color = if (message.isTranslationVisible) AccentPrimary else TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    AnimatedVisibility(visible = message.isTranslationVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Traducción:",
                                        color = AccentPrimary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = message.translation,
                                    color = TextPrimary.copy(alpha = 0.92f),
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Timestamp
                Text(
                    text = stringResource(R.string.onboarding_time_now),
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
