package com.app.grammarai.ui.practice.conversation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.theme.DarkCardBorder
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun LiveMicToggleBar(
    isPlayingAudio: Boolean,
    isAiRespondingLive: Boolean,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isPlayingAudio -> Color(0xFF131D38)
                    isAiRespondingLive -> Color(0xFF261D13)
                    isListening -> Color(0xFF1E2818)
                    else -> DarkSurface
                }
            )
            .border(
                1.dp,
                when {
                    isPlayingAudio -> NeonCyan.copy(alpha = 0.5f)
                    isAiRespondingLive -> NeonAmber.copy(alpha = 0.5f)
                    isListening -> NeonEmerald.copy(alpha = 0.5f)
                    else -> DarkCardBorder
                },
                RoundedCornerShape(14.dp)
            )
            .clickable { onToggleMic() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isPlayingAudio -> NeonCyan
                        isAiRespondingLive -> DarkSurfaceCard
                        isListening -> NeonEmerald
                        else -> DarkSurfaceCard
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPlayingAudio) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "AI Speaking",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Mic Toggle",
                    tint = if (isListening) Color.Black else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = when {
                    isPlayingAudio -> stringResource(R.string.drill_live_mic_speaking)
                    isAiRespondingLive -> stringResource(R.string.drill_live_mic_standby)
                    isListening -> stringResource(R.string.drill_live_mic_active)
                    else -> stringResource(R.string.drill_live_mic_muted)
                },
                color = when {
                    isPlayingAudio -> NeonCyan
                    isAiRespondingLive -> NeonAmber
                    isListening -> NeonEmerald
                    else -> TextPrimary
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    isPlayingAudio -> stringResource(R.string.drill_live_sub_playing)
                    isAiRespondingLive -> stringResource(R.string.drill_live_sub_waiting)
                    isListening -> stringResource(R.string.drill_live_sub_speak)
                    else -> stringResource(R.string.drill_live_sub_tap)
                },
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
