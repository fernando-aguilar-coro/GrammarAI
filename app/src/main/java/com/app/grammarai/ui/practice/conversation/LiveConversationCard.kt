package com.app.grammarai.ui.practice.conversation

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.app.grammarai.data.remote.LiveConversationTurn
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Voice-call screen card for Gemini Live conversation.
 * Manages teleprompter captions synchronized with speech playback,
 * visualizer orb, mic state, and topic switching dialog.
 */
@Composable
fun LiveConversationCard(
    conversationTurns: List<LiveConversationTurn>,
    conversationTopic: String = "",
    isAiRespondingLive: Boolean,
    isPlayingAudio: Boolean = false,
    isListening: Boolean,
    conversationTurnCount: Int = 0,
    maxConversationTurns: Int = 8,
    liveStreamingAiText: String = "",
    liveStreamingUserText: String = "",
    isTopicSwitchable: Boolean = true,
    onChangeTopic: (String) -> Unit = {},
    onToggleMic: () -> Unit,
    onSkipConversation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTopicDialog by remember { mutableStateOf(false) }

    // Compute the latest target AI response text
    val latestAiTurn = conversationTurns.filter { it.role == "model" }.lastOrNull()
    val isUserLastSpeaker = conversationTurns.lastOrNull()?.role == "user"
    val fullAiText = when {
        liveStreamingAiText.isNotBlank() -> liveStreamingAiText
        latestAiTurn != null && !isUserLastSpeaker -> latestAiTurn.text.trim()
        else -> stringResource(R.string.drill_live_partner_waiting)
    }

    // Only show thinking indicator while waiting for the first AI token/audio
    val isPreparingSpeech = isAiRespondingLive && liveStreamingAiText.isBlank() && !isPlayingAudio

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, Color(0x3322C55E), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status badge and turn counter
        LiveVoiceHeader(
            isListening = isListening,
            isAiRespondingLive = isAiRespondingLive || isPlayingAudio,
            conversationTurnCount = conversationTurnCount,
            maxConversationTurns = maxConversationTurns
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Center Voice Visualizer Orb
        LiveVoiceOrb(
            isPlayingAudio = isPlayingAudio,
            isAiRespondingLive = isAiRespondingLive,
            isListening = isListening
        )

        Spacer(modifier = Modifier.height(18.dp))

        // AI Spoken Captions / Teleprompter Box
        LiveSpeechCaptionsBox(
            isPreparingSpeech = isPreparingSpeech,
            isPlayingAudio = isPlayingAudio,
            pacedAiText = fullAiText,
            fullAiText = fullAiText
        )

        // Live User Speech Transcript Preview (if user is speaking)
        if (liveStreamingUserText.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentPrimary.copy(alpha = 0.15f))
                    .border(1.dp, AccentPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "You: \"$liveStreamingUserText\"",
                    color = AccentPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mic Toggle & Status Bar
        LiveMicToggleBar(
            isPlayingAudio = isPlayingAudio,
            isAiRespondingLive = isAiRespondingLive,
            isListening = isListening,
            onToggleMic = onToggleMic
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Guidance Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101B2B))
                .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Habla libremente en inglés o español. Tu coach te entiende y te guiará.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Topic Switcher Bar or Locked Situation Badge
        if (isTopicSwitchable) {
            LiveTopicSelectorBar(
                conversationTopic = conversationTopic,
                onClickChange = { showTopicDialog = true }
            )

            if (showTopicDialog) {
                ChangeTopicDialog(
                    currentTopic = conversationTopic,
                    onTopicSelected = {
                        showTopicDialog = false
                        onChangeTopic(it)
                    },
                    onDismiss = { showTopicDialog = false }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, AccentPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = conversationTopic,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "SITUACIÓN REAL",
                        color = AccentPrimary,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer Action: Skip Conversation
        LiveFooterActions(
            onSkipConversation = onSkipConversation
        )
    }
}
