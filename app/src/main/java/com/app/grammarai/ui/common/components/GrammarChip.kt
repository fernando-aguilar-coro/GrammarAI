package com.app.grammarai.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun GrammarChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    textColor: Color = TextPrimary,
    containerColor: Color = DarkSurface,
    borderColor: Color = BorderSubtle,
    fontSize: TextUnit = 11.sp,
    letterSpacing: TextUnit = 0.5.sp,
    horizontalPadding: Dp = 9.dp,
    verticalPadding: Dp = 4.5.dp,
    shapeRadius: Dp = 7.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(shapeRadius)
    val boxModifier = modifier
        .clip(shape)
        .background(containerColor)
        .border(1.dp, borderColor, shape)
        .then(
            if (onClick != null) Modifier.tactilePress(onClick = onClick) else Modifier
        )
        .padding(horizontal = horizontalPadding, vertical = verticalPadding)

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: textColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = letterSpacing
            )
        }
    }
}
