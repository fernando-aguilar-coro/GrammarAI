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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonViolet
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun ScenarioDialogueBubble(
    dialogueContext: String,
    modifier: Modifier = Modifier
) {
    val lines = dialogueContext.split("\n", "\\n").filter { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1431))
            .border(1.dp, NeonViolet.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        lines.forEach { line ->
            val colonIndex = line.indexOf(':')
            val (speakerRaw, speechRaw) = if (colonIndex != -1) {
                line.substring(0, colonIndex).trim() to line.substring(colonIndex + 1).trim()
            } else {
                "Escenario" to line.trim()
            }
            val isUser = speakerRaw.equals("you", ignoreCase = true) || speakerRaw.equals("tú", ignoreCase = true)
            val speakerIcon = when {
                isUser -> "👤"
                speakerRaw.contains("barista", ignoreCase = true) -> "☕"
                speakerRaw.contains("agent", ignoreCase = true) || speakerRaw.contains("agente", ignoreCase = true) -> "🎫"
                speakerRaw.contains("hotel", ignoreCase = true) || speakerRaw.contains("reception", ignoreCase = true) -> "🛎️"
                speakerRaw.contains("doctor", ignoreCase = true) -> "🩺"
                speakerRaw.contains("waiter", ignoreCase = true) || speakerRaw.contains("mesero", ignoreCase = true) -> "🍽️"
                else -> "💬"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isUser) NeonCyan.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f))
                        .border(1.dp, if (isUser) NeonCyan.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = speakerIcon, fontSize = 14.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = speakerRaw,
                        color = if (isUser) NeonCyan else NeonAmber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = speechRaw.removeSurrounding("'", "'").removeSurrounding("\"", "\""),
                        color = if (speechRaw == "___") TextMuted else TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (speechRaw == "___") FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
