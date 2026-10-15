package com.app.grammarai.ui.practice.conversation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald

@Composable
fun LiveVoiceHeader(
    isListening: Boolean,
    isAiRespondingLive: Boolean,
    conversationTurnCount: Int,
    maxConversationTurns: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isListening || isAiRespondingLive) NeonEmerald else NeonAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "GEMINI LIVE",
                color = AccentPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F261F))
                .border(1.dp, NeonEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Turn $conversationTurnCount / $maxConversationTurns",
                color = NeonEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveVoiceOrb(
    isPlayingAudio: Boolean,
    isAiRespondingLive: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isPlayingAudio || isAiRespondingLive || isListening) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val orbGlow = when {
        isPlayingAudio -> Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color.Transparent))
        isAiRespondingLive -> Brush.radialGradient(listOf(NeonAmber.copy(alpha = 0.4f), Color.Transparent))
        isListening -> Brush.radialGradient(listOf(NeonEmerald.copy(alpha = 0.45f), Color.Transparent))
        else -> Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent))
    }

    Box(
        modifier = modifier
            .size(110.dp)
            .scale(pulseScale)
            .background(orbGlow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F261F))
                .border(
                    2.dp,
                    when {
                        isPlayingAudio -> NeonCyan
                        isAiRespondingLive -> NeonAmber
                        isListening -> NeonEmerald
                        else -> NeonCyan
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlayingAudio) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.GraphicEq,
                contentDescription = "Voice Visualizer",
                tint = when {
                    isPlayingAudio -> NeonCyan
                    isAiRespondingLive -> NeonAmber
                    isListening -> NeonEmerald
                    else -> NeonCyan
                },
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
