package com.app.grammarai.ui.practice.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun LiveSpeechCaptionsBox(
    isPreparingSpeech: Boolean,
    isPlayingAudio: Boolean,
    pacedAiText: String,
    fullAiText: String,
    modifier: Modifier = Modifier
) {
    val targetBorderColor = when {
        isPlayingAudio -> NeonCyan.copy(alpha = 0.55f)
        isPreparingSpeech -> NeonAmber.copy(alpha = 0.45f)
        else -> Color(0x33475569)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "captions_border_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPreparingSpeech,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "captions_state_content"
        ) { preparing ->
            if (preparing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NeonAmber,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.drill_live_thinking),
                        color = NeonAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = fullAiText.ifBlank { pacedAiText },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
