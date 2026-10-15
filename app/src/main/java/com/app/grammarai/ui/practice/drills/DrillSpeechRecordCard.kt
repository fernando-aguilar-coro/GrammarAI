package com.app.grammarai.ui.practice.drills

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.app.grammarai.R
import com.app.grammarai.ui.theme.DarkCardBorder
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun DrillSpeechRecordCard(
    targetPhrase: String = "",
    isListening: Boolean,
    spokenTranscript: String,
    matchScore: Int?,
    onToggleRecord: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, if (isListening) NeonAmber else DarkCardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (targetPhrase.isNotBlank()) {
            WordPronunciationHighlightView(
                targetPhrase = targetPhrase,
                spokenTranscript = spokenTranscript,
                isListening = isListening,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Text(
            text = if (isListening) stringResource(R.string.drill_record_listening) else stringResource(R.string.drill_record_tap_speak),
            color = if (isListening) NeonAmber else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (isListening) NeonAmber.copy(alpha = 0.2f) else DarkSurface)
                .border(2.dp, if (isListening) NeonAmber else NeonCyan, CircleShape)
                .clickable { onToggleRecord() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Speak",
                tint = if (isListening) NeonAmber else NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        AnimatedVisibility(visible = spokenTranscript.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.drill_record_you_said, spokenTranscript),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (matchScore != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isGood = matchScore >= 70
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGood) Color(0x2222C55E) else Color(0x22F59E0B))
                            .border(1.dp, if (isGood) NeonEmerald else NeonAmber, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drill_record_accuracy, matchScore),
                            color = if (isGood) NeonEmerald else NeonAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.drill_skip_exercise),
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSkip() }.padding(8.dp)
        )
    }
}
