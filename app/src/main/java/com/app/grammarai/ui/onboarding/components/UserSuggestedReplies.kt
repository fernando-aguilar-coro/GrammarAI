package com.app.grammarai.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.common.animation.tick
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.DarkSurfaceCardElevated
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun UserSuggestedReplies(
    chips: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isMultiSelect: Boolean = false,
    selectedChips: Set<String> = emptySet(),
    minSelections: Int = 1,
    maxSelections: Int = 5,
    onConfirmMultiSelect: (() -> Unit)? = null,
    confirmButtonText: String = "Continuar con mis escenarios"
) {
    if (chips.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val canConfirm = enabled && selectedChips.size >= minSelections

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEachIndexed { index, chipText ->
            val isSelected = isMultiSelect && chipText in selectedChips
            val isChipClickable = enabled && (!isMultiSelect || isSelected || selectedChips.size < maxSelections)

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 * (index + 1) })
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 320.dp)
                        .tactilePress(enabled = isChipClickable)
                        .clip(
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 4.dp,
                                bottomEnd = 14.dp,
                                bottomStart = 14.dp
                            )
                        )
                        .background(
                            if (isSelected) DarkSurfaceCardElevated else DarkSurfaceCard
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AccentPrimary else BorderSubtle,
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 4.dp,
                                bottomEnd = 14.dp,
                                bottomStart = 14.dp
                            )
                        )
                        .clickable(enabled = isChipClickable) {
                            haptic.tick()
                            onSelect(chipText)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = chipText,
                            color = if (isSelected) AccentPrimary else if (isChipClickable) TextPrimary else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AccentPrimary.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMultiSelect) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = if (isChipClickable) TextMuted else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send reply",
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMultiSelect && onConfirmMultiSelect != null) {
            Spacer(modifier = Modifier.height(4.dp))
            GrammarButton(
                text = confirmButtonText,
                onClick = onConfirmMultiSelect,
                style = GrammarButtonStyle.PRIMARY,
                enabled = canConfirm,
                trailingIcon = Icons.AutoMirrored.Filled.Send,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
