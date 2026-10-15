package com.app.grammarai.ui.practice.drills

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.app.grammarai.data.model.PairMatchItem
import com.app.grammarai.ui.common.animation.popBounceEffect
import com.app.grammarai.ui.common.animation.shakeEffect
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

data class PairCardItem(
    val id: String,
    val text: String
)

@Composable
fun SelectPairsView(
    pairs: List<PairMatchItem>,
    selectedLeftId: String?,
    selectedRightId: String?,
    matchedLeftIds: Set<String>,
    matchedRightIds: Set<String>,
    pairError: Pair<String, String>?,
    onSelectLeft: (id: String, text: String) -> Unit,
    onSelectRight: (id: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Shuffled right options remembered across recompositions for this exercise
    // Every card has a distinct ID so identical words (e.g. 'is' and 'is') are tracked separately
    val leftCards = remember(pairs) {
        pairs.mapIndexed { index, item ->
            PairCardItem(id = "L_$index", text = item.left)
        }
    }
    val rightCards = remember(pairs) {
        pairs.mapIndexed { index, item ->
            PairCardItem(id = "R_$index", text = item.right)
        }.shuffled()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leftCards.forEach { card ->
                    val isMatched = matchedLeftIds.contains(card.id)
                    val isSelected = selectedLeftId == card.id
                    val isError = pairError?.first == card.id

                    PairCard(
                        text = card.text,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isError = isError,
                        onClick = { if (!isMatched) onSelectLeft(card.id, card.text) }
                    )
                }
            }

            // Right Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rightCards.forEach { card ->
                    val isMatched = matchedRightIds.contains(card.id)
                    val isSelected = selectedRightId == card.id
                    val isError = pairError?.second == card.id

                    PairCard(
                        text = card.text,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isError = isError,
                        onClick = { if (!isMatched) onSelectRight(card.id, card.text) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PairCard(
    text: String,
    isSelected: Boolean,
    isMatched: Boolean,
    isError: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isMatched -> SuccessEmerald.copy(alpha = 0.6f)
            isError -> ErrorCrimson
            isSelected -> AccentPrimary
            else -> BorderSubtle
        },
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isMatched -> SuccessEmerald.copy(alpha = 0.12f)
            isError -> ErrorCrimson.copy(alpha = 0.12f)
            isSelected -> AccentPrimary.copy(alpha = 0.15f)
            else -> DarkSurfaceCard
        },
        label = "bgColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isMatched -> SuccessEmerald
            isError -> Color(0xFFFCA5A5)
            isSelected -> Color.White
            else -> TextPrimary
        },
        label = "textColor"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isMatched) 0.5f else 1.0f,
        label = "cardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isError) {
                    Modifier.shakeEffect(trigger = isError)
                } else if (isMatched) {
                    Modifier.popBounceEffect(trigger = isMatched)
                } else {
                    Modifier
                }
            )
            .alpha(cardAlpha)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected || isMatched || isError) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .tactilePress(enabled = !isMatched, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isMatched) FontWeight.Bold else FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
            if (isMatched) {
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = SuccessEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
